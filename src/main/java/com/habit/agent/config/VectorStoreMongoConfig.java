package com.habit.agent.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.mongodb.atlas.MongoDBAtlasVectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.util.StringUtils;

/**
 * 远程 MongoDB Atlas 向量库配置（RAG）
 * <p>
 * 与本地 MongoDB（spring.data.mongodb 自动配置）完全解耦：
 * 通过顶层属性 REMOTE_MONGO_URI 在 Bean 方法内创建独立连接（不注册 MongoTemplate/MongoClient
 * Bean，避免 @ConditionalOnMissingBean 抑制 Spring Data 自动配置的本地 mongoTemplate）。
 * <p>
 * initializeSchema=false：不自动创建向量索引，复用 Atlas 上已存在的 habit_vector_index
 * （MongoDBAtlasVectorStore 的索引创建发生在 Spring afterPropertiesSet 回调中，无法在 Bean
 * 工厂方法内捕获 IndexAlreadyExists，故固定复用既有索引）。
 * <p>
 * 容错策略：连接或构造失败时降级为无向量库（vectorStore Bean 返回 null），不阻断应用启动；
 * 若 Atlas 尚无该索引，需在 Atlas Console 手动创建向量搜索索引 habit_vector_index：
 * <pre>
 * {
 *   "fields": [
 *     { "type": "vector", "path": "embedding", "numDimensions": 1024, "similarity": "cosine" }
 *   ]
 * }
 * </pre>
 */
@Slf4j
@Configuration
public class VectorStoreMongoConfig {

    /** 远程 Atlas 数据库名 */
    private static final String ATLAS_DATABASE = "habit_agent";

    @Value("${REMOTE_MONGO_URI:}")
    private String remoteMongoUri;

    /**
     * 远程 Atlas 向量库（集合 habit_knowledge，索引 habit_vector_index）
     */
    @Bean
    public VectorStore vectorStore(ObjectProvider<EmbeddingModel> embeddingModelProvider) {
        if (!StringUtils.hasText(remoteMongoUri)) {
            log.warn("未配置 REMOTE_MONGO_URI，向量库 RAG 功能不可用");
            return null;
        }
        EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
        if (embeddingModel == null) {
            log.warn("EmbeddingModel 不可用，向量库 RAG 功能不可用");
            return null;
        }
        try {
            MongoClient client = MongoClients.create(remoteMongoUri);
            MongoTemplate template = new MongoTemplate(client, ATLAS_DATABASE);
            return MongoDBAtlasVectorStore.builder(template, embeddingModel)
                    .collectionName("habit_knowledge")
                    .vectorIndexName("habit_vector_index")
                    .pathName("embedding")
                    .numCandidates(200)
                    .initializeSchema(false)
                    .build();
        } catch (Exception e) {
            log.error("创建 MongoDBAtlasVectorStore 失败，向量库 RAG 降级为不可用", e);
            return null;
        }
    }
}
