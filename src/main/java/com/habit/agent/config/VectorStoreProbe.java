package com.habit.agent.config;

import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.mongodb.client.MongoClient;
import com.mongodb.connection.ClusterDescription;
import com.mongodb.connection.ServerDescription;

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
 * <p><b>诊断增强（2026-08-06）：</b>一次性打印全部相关配置源与真实连接地址：
 * <ul>
 *   <li>{@code spring.data.mongodb.uri}（yml / profile / 环境变量解析后的最终值）；</li>
 *   <li>系统属性与环境变量中所有 MongoDB 相关项（覆盖源排查）；</li>
 *   <li>{@link MongoClient} 底层 {@link ClusterDescription} 实际连接地址（唯一真相）。</li>
 * </ul>
 * 三者对照即可精确定位「配置写 Atlas 但实际连 localhost」的覆盖源。
 */
@Slf4j
@Component
public class VectorStoreProbe {

    private final String configUri;

    public VectorStoreProbe(VectorStore vectorStore,
                            @Qualifier("atlasMongoTemplate") MongoTemplate mongoTemplate,
                            @Qualifier("atlasMongoClient") MongoClient mongoClient,
                            Environment environment) {
        // 向量库连接串实为远端 Atlas（app.atlas.mongodb.uri）；本地聊天库走 spring.mongodb.uri
        this.configUri = environment.getProperty("app.atlas.mongodb.uri", "");

        dumpConfigSources(environment);
        String realHost = dumpActualConnection(mongoClient, mongoTemplate);
        probe(vectorStore, realHost);
    }

    /** 打印 yml 最终值 + 环境变量 + 系统属性中所有 MongoDB 相关项。 */
    private void dumpConfigSources(Environment environment) {
        String ymlUri = maskUri(environment.getProperty("spring.mongodb.uri", "(未配置)"));
        String ymlOldUri = environment.getProperty("spring.data.mongodb.uri", "(未配置)");
        String atlasUri = maskUri(environment.getProperty("app.atlas.mongodb.uri", "(未配置)"));
        log.info("[阶段八][配置] 本地聊天库 spring.mongodb.uri={}（旧键 spring.data.mongodb.uri={}）", ymlUri, ymlOldUri);
        log.info("[阶段八][配置] 远端向量库 app.atlas.mongodb.uri={}", atlasUri);

        // 环境变量
        for (String k : new String[]{"MONGODB_URI", "SPRING_MONGODB_URI", "SPRING_DATA_MONGODB_URI",
                "SPRING_MONGODB_HOST", "SPRING_MONGODB_PORT", "SPRING_MONGODB_DATABASE"}) {
            String v = System.getenv(k);
            if (v != null && !v.isBlank()) {
                log.info("[阶段八][配置] 环境变量 {}={}", k, v);
            }
        }
        // 系统属性
        for (String k : new String[]{"spring.mongodb.uri", "spring.mongodb.host",
                "spring.mongodb.port", "spring.mongodb.database",
                "spring.data.mongodb.uri", "spring.data.mongodb.host"}) {
            String v = System.getProperty(k);
            if (v != null && !v.isBlank()) {
                log.info("[阶段八][配置] 系统属性 -D{}={}", k, v);
            }
        }
    }

    /** 从 MongoClient 底层 ClusterDescription 读取真实连接地址（唯一真相）。 */
    private String dumpActualConnection(MongoClient mongoClient, MongoTemplate mongoTemplate) {
        try {
            ClusterDescription cluster = mongoClient.getClusterDescription();
            List<ServerDescription> servers = cluster.getServerDescriptions();
            if (servers != null && !servers.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (ServerDescription s : servers) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(s.getAddress()).append("(").append(s.getType()).append(")");
                }
                log.info("[阶段八][真实连接] MongoClient cluster type={} 服务器=[{}]",
                        cluster.getType(), sb);
                return sb.toString();
            }
            // 尚未建立连接时 cluster 为空，用 isMaster 兜底
            Document isMaster = mongoTemplate.getDb().runCommand(new Document("isMaster", 1));
            String me = isMaster.getString("me");
            String primary = isMaster.getString("primary");
            log.info("[阶段八][真实连接] isMaster.me={} isMaster.primary={}", me, primary);
            return (me != null ? me : primary);
        } catch (Exception e) {
            log.info("[阶段八][真实连接] 读取真实连接地址失败：{}", e.getMessage());
            return "unknown";
        }
    }

    private void probe(VectorStore vectorStore, String realHost) {
        boolean isAtlas = realHost != null
                && (realHost.contains(".mongodb.net") || realHost.contains("mongodb.net"));
        if (!isAtlas) {
            log.info("[阶段八] 实际连接 host={}（非 MongoDB Atlas），跳过向量库连通性探针。RAG 检索将降级为无检索对话。"
                    + "请检查上方 [配置]/[真实连接] 日志找出覆盖源。", realHost);
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

    private static String maskUri(String uri) {
        if (uri == null || uri.isBlank() || !uri.contains("@")) {
            return uri;
        }
        return uri.replaceAll("://[^@]+@", "://****:****@");
    }
}
