package com.habit.agent.config;

import java.util.List;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.mongodb.atlas.MongoDBAtlasVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

/**
 * 双数据源装配（本地聊天库 + 远端 Atlas 向量库）。
 *
 * <ul>
 *   <li><b>主库（本地 MongoDB，spring.mongodb.uri = localhost:27017）</b>：@Primary 的
 *       {@code mongoTemplate} 供业务实体 / 对话记忆（ai_chat_memory）/ 会话（chatSession）等
 *       结构化数据默认注入，Repository 自动绑定此库。</li>
 *   <li><b>Atlas 专用连接（app.atlas.mongodb.uri）</b>：{@code atlasMongoClient} +
 *       {@code atlasMongoTemplate} 仅用于构建 {@link MongoDBAtlasVectorStore}（habit_knowledge
 *       向量库）与 RAG 文档管理，实现「本地聊天记录 + 远端向量库」的架构隔离。</li>
 * </ul>
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

    /**
     * 主库（本地）：@Primary 供业务默认注入。
     *
     * <p>MongoDatabaseFactory 由 Spring Boot 自动配置基于 spring.mongodb.uri（本地）提供，
     * 此处重新包装为 @Primary MongoTemplate，确保无同名 bean 歧义。
     */
    @Bean
    @Primary
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory) {
        return new MongoTemplate(mongoDatabaseFactory);
    }

    /** Atlas 专用客户端（向量库），连接 app.atlas.mongodb.uri。 */
    @Bean(destroyMethod = "close")
    public MongoClient atlasMongoClient(@Value("${app.atlas.mongodb.uri}") String uri) {
        return MongoClients.create(uri);
    }

    /** Atlas 专用 MongoTemplate（向量库），供 MongoDBAtlasVectorStore 与 RAG 文档管理使用。 */
    @Bean("atlasMongoTemplate")
    public MongoTemplate atlasMongoTemplate(MongoClient atlasMongoClient,
            @Value("${app.atlas.mongodb.uri}") String uri) {
        return new MongoTemplate(
                new SimpleMongoClientDatabaseFactory(atlasMongoClient, databaseName(uri, ATLAS_COLLECTION)));
    }

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

    /** 从连接串提取数据库名；无法解析时使用 fallback。 */
    private static String databaseName(String uri, String fallback) {
        int idx = uri.lastIndexOf('/');
        if (idx >= 0 && idx + 1 < uri.length()) {
            String candidate = uri.substring(idx + 1);
            int q = candidate.indexOf('?');
            if (q >= 0) {
                candidate = candidate.substring(0, q);
            }
            if (!candidate.isBlank()) {
                return candidate;
            }
        }
        return fallback;
    }
}
