package com.habit.agent;

import org.springframework.ai.vectorstore.mongodb.autoconfigure.MongoDBAtlasVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 生活习惯助手 Agent 启动类
 *
 * 子模块 1-1：Git 仓库初始化与项目骨架
 * 仅包含最基本的 Spring Boot 启动注解，无额外配置。
 * 数据库和 AI 配置在后续子模块中逐步添加。
 *
 * <p>排除 {@code MongoDBAtlasVectorStoreAutoConfiguration}：该自动配置会用主 MongoTemplate
 * （本地聊天库）构建 VectorStore，与 {@code AtlasVectorStoreConfig} 自定义的 Atlas VectorStore
 * 冲突。排除后由自定义 Bean 全权负责向量库连接（远端 Atlas）。
 */
@SpringBootApplication(exclude = MongoDBAtlasVectorStoreAutoConfiguration.class)
public class HabitAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(HabitAgentApplication.class, args);
    }
}
