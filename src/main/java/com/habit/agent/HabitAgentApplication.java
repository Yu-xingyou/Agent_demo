package com.habit.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 生活习惯助手后端启动类
 *
 * 仅包含最基本的 Spring Boot 启动注解，无额外配置。
 * 数据库配置在后续模块中逐步添加。
 */
@EnableAsync // 启用 @Async，支持会话标题异步更新等异步任务
@SpringBootApplication
public class HabitAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(HabitAgentApplication.class, args);
    }
}
