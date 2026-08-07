package com.habit.agent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 2.0 配置（流式对话能力）。
 *
 * <p>spring-ai-starter-model-openai 已自动配置 {@link ChatClient.Builder}，
 * 此处仅注册 Advisor 增强（请求/响应日志）并组装默认 {@link ChatClient}。</p>
 */
@Configuration
public class SpringAIConfig {

    /**
     * 配置默认 ChatClient（带日志 Advisor）。
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder,
                                 Advisor loggerAdvisor) {
        return chatClientBuilder
                .defaultAdvisors(loggerAdvisor)
                .build();
    }

    /**
     * 请求/响应日志 Advisor。
     */
    @Bean
    public Advisor loggerAdvisor() {
        return new SimpleLoggerAdvisor();
    }
}
