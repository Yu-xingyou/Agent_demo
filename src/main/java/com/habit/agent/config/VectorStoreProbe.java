package com.habit.agent.config;

import org.bson.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 阶段八：MongoDB Atlas 向量库连通性探针。
 *
 * <p>启动时执行一次最小代价检索（topK=1），用于尽早暴露以下三类配置问题：
 * <ol>
 *   <li>MONGODB_URI 未指向 Atlas（本地社区版不支持 $vectorSearch）；</li>
 *   <li>Atlas 控制台尚未创建 habit_vector_index 向量索引；</li>
 *   <li>索引维度与 text-embedding-v3 输出维度（1024）不一致。</li>
 * </ol>
 *
 * <p>探测失败仅打印 WARN 并给出排查指引，<b>不抛异常、不阻断应用启动</b>。
 *
 * <p><b>关键增强（2026-08-06）：</b>原来仅从 Environment 读取 yml 配置值，无法反映运行时实际连接。
 * 现改为注入 {@link MongoTemplate}，通过 {@code runCommand("buildInfo")} 获取<b>实际连接的
 * MongoDB 主机地址</b>，同时打印 yml 配置值做对比。若两者不一致（如 yml 写 Atlas、实际连 localhost），
 * 可立即定位配置覆盖源（IDE run config / 环境变量 / old build output 等）。
 */
@Slf4j
@Component
public class VectorStoreProbe {

    private final String configUri;

    public VectorStoreProbe(VectorStore vectorStore, MongoTemplate mongoTemplate, Environment environment) {
        this.configUri = environment.getProperty("spring.data.mongodb.uri", "");

        // 打印 yml 配置值 vs 实际连接地址，便于对比诊断
        String realHost = getRealHost(mongoTemplate);
        log.info("[阶段八] MongoDB 连接诊断：yml 配置 host={} | 实际连接 host={}",
                extractHost(configUri), realHost);

        probe(vectorStore, mongoTemplate);
    }

    private void probe(VectorStore vectorStore, MongoTemplate mongoTemplate) {
        // 先判断实际连接的 host 是否为 Atlas（从 MongoTemplate 判断，而非 yml）
        String realHost = getRealHost(mongoTemplate);
        boolean isAtlas = realHost.contains(".mongodb.net") || realHost.contains("mongodb.net");

        if (!isAtlas) {
            log.info("[阶段八] 实际连接 host={}（非 MongoDB Atlas），跳过向量库连通性探针。RAG 检索将降级为无检索对话。"
                    + "如需启用，请确认 spring.data.mongodb.uri 指向 Atlas 且无环境变量/IDE配置覆盖。",
                    realHost);
            return;
        }

        try {
            vectorStore.similaritySearch(SearchRequest.builder()
                    .query("ping")
                    .topK(1)
                    .similarityThresholdAll()
                    .build());
            log.info("[阶段八] MongoDB Atlas 向量库连通性验证成功（host={}），RAG 知识库已就绪。", realHost);
        } catch (Exception e) {
            log.warn("""
                    [阶段八] MongoDB Atlas 向量库连通性验证失败（实际连接 host={}，不影响应用启动）：{}
                    请依次检查：
                      1) spring.data.mongodb.uri 是否指向 Atlas 且未被环境变量/IDE配置覆盖；
                      2) Atlas 控制台是否已为 habit_knowledge 集合创建名为 habit_vector_index 的 Vector Search 索引；
                      3) 索引配置是否为 path=embedding、numDimensions=1024、similarity=cosine。""",
                    realHost, e.getMessage());
        }
    }

    /** 从 MongoTemplate 获取实际连接的 MongoDB 主机地址。失败时返回 "unknown"。 */
    private static String getRealHost(MongoTemplate mongoTemplate) {
        try {
            Document result = mongoTemplate.getDb().runCommand(new Document("buildInfo", 1));
            // 如果 buildInfo 不包含 host info，尝试 serverStatus + isMaster
            if (result != null) {
                // 尝试从 isMaster 获取 primary host
                Document isMaster = mongoTemplate.getDb().runCommand(new Document("isMaster", 1));
                if (isMaster != null) {
                    String primary = isMaster.getString("primary");
                    if (primary != null) return primary;
                    String me = isMaster.getString("me");
                    if (me != null) return me;
                }
                // 兜底：直接取 serverStatus 中的 host
                Document serverStatus = mongoTemplate.getDb().runCommand(new Document("serverStatus", 1));
                if (serverStatus != null) {
                    String host = serverStatus.getString("host");
                    if (host != null) return host;
                }
            }
        } catch (Exception e) {
            // 如果连不上，说明连接本身有问题
            return "连接失败(" + e.getMessage() + ")";
        }
        return "unknown";
    }

    /** 从连接串中解析出 host 段，解析失败兜底返回原串。 */
    private static String extractHost(String uri) {
        try {
            String s = uri;
            int at = s.lastIndexOf('@');
            if (at >= 0) s = s.substring(at + 1);
            int proto = s.indexOf("://");
            if (proto >= 0) s = s.substring(proto + 3);
            int end = s.length();
            int slash = s.indexOf('/');
            int q = s.indexOf('?');
            if (slash >= 0) end = Math.min(end, slash);
            if (q >= 0) end = Math.min(end, q);
            return s.substring(0, end);
        } catch (Exception e) {
            return uri;
        }
    }
}
