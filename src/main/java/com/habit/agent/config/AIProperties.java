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

        /** 路由（route-agent）智能体系统提示语 */
        private Chat routeAgent;

        /** 睡眠建议（sleep-agent）智能体系统提示语 */
        private Chat sleepAgent;

        /** 饮食建议（diet-agent）智能体系统提示语 */
        private Chat dietAgent;

        /** 运动建议（exercise-agent）智能体系统提示语 */
        private Chat exerciseAgent;

        /** 习惯打卡（checkin-agent）智能体系统提示语 */
        private Chat checkinAgent;

        /** 健康知识讲解（knowledge-agent）智能体系统提示语 */
        private Chat knowledgeAgent;

        @Data
        public static class Chat {
            /** 系统提示词文本 */
            private String text;
        }
    }
}
