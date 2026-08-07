package com.habit.agent.config;

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

import lombok.extern.slf4j.Slf4j;

/**
 * 双数据源显式装配（完全绕开 Spring Boot 4.1 的 Mongo 自动配置）。
 *
 * <p>背景：SB4.1 的 {@code MongoAutoConfiguration} 类级别带有
 * {@code @ConditionalOnMissingBean(type = MongoDatabaseFactory)}，且其创建
 * MongoClient 的方法带有 {@code @ConditionalOnMissingBean(MongoClient)}。
 * 一旦项目里出现任一 {@code MongoClient} / {@code MongoDatabaseFactory} 类型的 Bean，
 * 自动配置便整体退避，导致"本地聊天库客户端"永远不会被创建——这正是此前两个库
 * 全部指向 Atlas 的根因。
 *
 * <p>因此本类<b>完全自行创建</b>两条独立的连接链路，杜绝自动配置反咬：
 * <ul>
 *   <li><b>本地聊天库（@Primary）</b>：{@code spring.mongodb.uri}（localhost:27017）→
 *       {@code localMongoClient} → {@code mongoDatabaseFactory}（@Primary）→
 *       {@code mongoTemplate}（@Primary）。承载 chatSession / ai_chat_memory /
 *       ai_analysis_task 等结构化数据，Mongo Repository 默认绑定此库。</li>
 *   <li><b>Atlas 向量库</b>：{@code app.atlas.mongodb.uri} →
 *       {@code atlasMongoClient} → {@code atlasMongoTemplate}。仅承载 habit_knowledge
 *       向量集合，供 {@code MongoDBAtlasVectorStore} 与 RAG 检索链路使用。</li>
 * </ul>
 */
@Slf4j
@Configuration
public class MongoDataSourceConfig {

    /** chatSession 等集合所在库名兜底（与 spring.mongodb.uri 末尾库名一致）。 */
    private static final String LOCAL_FALLBACK_DB = "habit_agent";
    /** Atlas 向量库集合名（与 application.yml 中 vectorstore 配置保持一致）。 */
    private static final String ATLAS_COLLECTION = "habit_knowledge";

    // ===================== 本地聊天库链路（@Primary）=====================

    /** 本地聊天库客户端：连接 spring.mongodb.uri（默认 localhost:27017）。 */
    @Bean(destroyMethod = "close")
    public MongoClient localMongoClient(@Value("${spring.mongodb.uri}") String uri) {
        log.info("[双数据源] 创建本地聊天库 MongoClient：{}", maskUri(uri));
        return MongoClients.create(uri);
    }

    /**
     * 本地 MongoDatabaseFactory（@Primary）。
     *
     * <p>命名 {@code mongoDatabaseFactory} 并标注 @Primary，使 Spring Data Mongo Repository
     * 装配时按名称/@Primary 选中本地库，无需改动 ChatSessionRepository /
     * AiAnalysisTaskRepository。
     */
    @Bean
    @Primary
    public MongoDatabaseFactory mongoDatabaseFactory(@Qualifier("localMongoClient") MongoClient localMongoClient,
            @Value("${spring.mongodb.uri}") String uri) {
        return new SimpleMongoClientDatabaseFactory(localMongoClient, databaseName(uri, LOCAL_FALLBACK_DB));
    }

    /** 本地 MongoTemplate（@Primary，bean 名保持 mongoTemplate 兼容既有默认注入）。 */
    @Bean
    @Primary
    public MongoTemplate mongoTemplate(@Qualifier("mongoDatabaseFactory") MongoDatabaseFactory mongoDatabaseFactory) {
        return new MongoTemplate(mongoDatabaseFactory);
    }

    // ===================== Atlas 向量库链路 =====================

    /** Atlas 专用客户端（向量库），连接 app.atlas.mongodb.uri；bean 名保持 atlasMongoClient。 */
    @Bean(destroyMethod = "close")
    public MongoClient atlasMongoClient(@Value("${app.atlas.mongodb.uri}") String uri) {
        log.info("[双数据源] 创建 Atlas 向量库 MongoClient：{}", maskUri(uri));
        return MongoClients.create(uri);
    }

    /** Atlas 专用 MongoTemplate（向量库），bean 名保持 atlasMongoTemplate 兼容既有 @Qualifier 注入。 */
    @Bean("atlasMongoTemplate")
    public MongoTemplate atlasMongoTemplate(@Qualifier("atlasMongoClient") MongoClient atlasMongoClient,
            @Value("${app.atlas.mongodb.uri}") String uri) {
        return new MongoTemplate(
                new SimpleMongoClientDatabaseFactory(atlasMongoClient, databaseName(uri, ATLAS_COLLECTION)));
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

    private static String maskUri(String uri) {
        if (uri == null || uri.isBlank() || !uri.contains("@")) {
            return uri;
        }
        return uri.replaceAll("://[^@]+@", "://****:****@");
    }
}
