package com.habit.agent.config;

import java.util.List;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.mongodb.atlas.MongoDBAtlasVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * 向量库装配（仅负责构建 {@link MongoDBAtlasVectorStore}）。
 *
 * <p>双数据源的连接（本地聊天库 + 远端 Atlas 向量库）已由 {@link MongoDataSourceConfig}
 * 统一显式装配，本类单一职责：基于 {@code atlasMongoTemplate}（Atlas 向量库专用）构建
 * 向量检索 Bean，供 {@code RagServiceImpl} / {@code SafeRetrievalAdvisor} / 探针注入。
 *
 * <p>注：{@code MongoDBAtlasVectorStoreAutoConfiguration} 已在启动类排除，
 * 防止其用主本地 MongoTemplate 自动构建 VectorStore 覆盖本配置的 Atlas Bean。
 */
@Configuration
public class AtlasVectorStoreConfig {

    /** Atlas 向量库集合名（与 application.yml 中 vectorstore 配置保持一致）。 */
    private static final String ATLAS_COLLECTION = "habit_knowledge";
    /** Atlas 向量索引名（Atlas 控制台需预先创建）。 */
    private static final String ATLAS_INDEX_NAME = "habit_vector_index";
    /** 向量字段名（text-embedding-v4 输出 1024 维）。 */
    private static final String ATLAS_PATH_NAME = "embedding";

    /** RAG 向量库（Atlas habit_knowledge 集合），供 SafeRetrievalAdvisor / RagServiceImpl / 探针注入。 */
    @Bean
    public MongoDBAtlasVectorStore vectorStore(@Qualifier("atlasMongoTemplate") MongoTemplate template,
            EmbeddingModel embeddingModel) {
        return MongoDBAtlasVectorStore.builder(template, embeddingModel)
                .collectionName(ATLAS_COLLECTION)
                .vectorIndexName(ATLAS_INDEX_NAME)
                .pathName(ATLAS_PATH_NAME)
                // 索引已在 Atlas 控制台创建，禁止启动时重建 schema，避免覆盖既有索引配置
                .initializeSchema(false)
                .metadataFieldsToFilter(List.of("doc_type", "source"))
                .build();
    }
}
