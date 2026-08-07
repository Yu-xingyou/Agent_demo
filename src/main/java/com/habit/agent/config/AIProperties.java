package com.habit.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI 相关配置属性（对应 application.yml 的 {@code habit.ai.prompt} 段）。
 *
 * <p>参照示例 {@code AIProperties} 的分层结构（system → chat），
 * 但本项目无 Nacos 配置中心，直接将系统提示词文本配置在 yml 中，
 * 由 {@link SystemPromptConfig} 在启动时加载到原子引用。</p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "habit.ai.prompt")
public class AIProperties {

    /** 系统提示语相关配置 */
    private System system;

    @Data
    public static class System {

        /** 对话（chat）系统提示语 */
        private Chat chat;

        @Data
        public static class Chat {
            /** 系统提示词文本（身份：健康习惯管理助手） */
            private String text;
        }
    }
}
