package com.habit.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 生活习惯助手 Agent 启动类
 *
 * 子模块 1-1：Git 仓库初始化与项目骨架
 * 仅包含最基本的 Spring Boot 启动注解，无额外配置。
 * 数据库和 AI 配置在后续子模块中逐步添加。
 */
@SpringBootApplication
public class HabitAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(HabitAgentApplication.class, args);
    }
}
