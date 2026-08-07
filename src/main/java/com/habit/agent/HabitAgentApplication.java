package com.habit.agent;

import org.springframework.ai.vectorstore.mongodb.autoconfigure.MongoDBAtlasVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 生活习惯助手后端启动类（单体应用，不使用 Nacos/微服务）
 *
 * 排除 MongoDBAtlasVectorStoreAutoConfiguration：向量库由 VectorStoreMongoConfig
 * 通过独立 REMOTE_MONGO_URI 连接远程 Atlas，避免自动配置使用本地 spring.data.mongodb 连接。
 */
@SpringBootApplication(exclude = MongoDBAtlasVectorStoreAutoConfiguration.class)
public class HabitAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(HabitAgentApplication.class, args);
    }
}
