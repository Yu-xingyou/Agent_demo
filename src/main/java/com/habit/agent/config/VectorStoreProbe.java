package com.habit.agent.config;

import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
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
 */
@Slf4j
@Component
public class VectorStoreProbe {

    public VectorStoreProbe(VectorStore vectorStore) {
        probe(vectorStore);
    }

    private void probe(VectorStore vectorStore) {
        try {
            vectorStore.similaritySearch(SearchRequest.builder()
                    .query("ping")
                    .topK(1)
                    .similarityThresholdAll()
                    .build());
            log.info("[阶段八] MongoDB Atlas 向量库连通性验证成功，RAG 知识库已就绪。");
        } catch (Exception e) {
            log.warn("""
                    [阶段八] MongoDB Atlas 向量库连通性验证失败（不影响应用启动）：{}
                    请依次检查：
                      1) MONGODB_URI 是否指向 MongoDB Atlas（本地社区版不支持 $vectorSearch）；
                      2) Atlas 控制台是否已为 habit_knowledge 集合创建名为 habit_vector_index 的 Vector Search 索引；
                      3) 索引配置是否为 path=embedding、numDimensions=1024、similarity=cosine。""",
                    e.getMessage());
        }
    }
}
