package com.habit.agent.config;

import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.env.Environment;
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
 * <p>探测失败仅打印 WARN 并给出排查指引，<b>不抛异常、不阻断应用启动</b>，
 * 保证在无 Atlas 的本地开发环境下其余功能仍可正常使用。
 * 写法与 {@code ChatClientConfig.ChatConnectivityProbe} 保持一致。
 *
 * <p>增强点：
 * <ul>
 *   <li>从 {@link Environment} 读取<b>实际生效</b>的 {@code spring.data.mongodb.uri} 并打印 host，
 *       以便一眼区分「连的是 Atlas 还是 localhost/被环境变量覆盖/旧配置未重启」；</li>
 *   <li>当连接串非 {@code mongodb+srv} 且不含 {@code .mongodb.net} 时，判定为非 Atlas 环境，
 *       跳过探针并 INFO 提示，避免本地每次启动打 WARN 噪音。</li>
 * </ul>
 */
@Slf4j
@Component
public class VectorStoreProbe {

    private final String resolvedUri;

    public VectorStoreProbe(VectorStore vectorStore, Environment environment) {
        this.resolvedUri = environment.getProperty("spring.data.mongodb.uri", "");
        probe(vectorStore);
    }

    private void probe(VectorStore vectorStore) {
        boolean isAtlas = resolvedUri.contains("mongodb+srv") || resolvedUri.contains(".mongodb.net");
        if (!isAtlas) {
            log.info("[阶段八] 当前连接串非 MongoDB Atlas（host={}），跳过向量库连通性探针。RAG 检索将降级为无检索对话。",
                    extractHost(resolvedUri));
            return;
        }

        try {
            vectorStore.similaritySearch(SearchRequest.builder()
                    .query("ping")
                    .topK(1)
                    .similarityThresholdAll()
                    .build());
            log.info("[阶段八] MongoDB Atlas 向量库连通性验证成功（host={}），RAG 知识库已就绪。", extractHost(resolvedUri));
        } catch (Exception e) {
            log.warn("""
                    [阶段八] MongoDB Atlas 向量库连通性验证失败（连接 host={}，不影响应用启动）：{}
                    请依次检查：
                      1) MONGODB_URI 是否指向 MongoDB Atlas 且连接串已带库名（如 .../habit_agent）；
                      2) Atlas 控制台是否已为 habit_knowledge 集合创建名为 habit_vector_index 的 Vector Search 索引；
                      3) 索引配置是否为 path=embedding、numDimensions=1024、similarity=cosine。""",
                    extractHost(resolvedUri), e.getMessage());
        }
    }

    /** 从连接串中解析出 host 段（mongodb+srv://user:pwd@host/db 或 mongodb://host:port/db），解析失败兜底返回原串。 */
    private static String extractHost(String uri) {
        try {
            String s = uri;
            int at = s.lastIndexOf('@');
            if (at >= 0) {
                s = s.substring(at + 1);
            }
            // 去掉协议头
            int proto = s.indexOf("://");
            if (proto >= 0) {
                s = s.substring(proto + 3);
            }
            // 截到第一个 / 或 ?
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
