package com.habit.agent.aigc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI 提示词配置绑定（habit.ai.prompt，参照天机学堂 tj-aigc AIProperties）
 * 提示词全部本地配置（单体应用，不使用 Nacos）
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "habit.ai.prompt")
public class AIProperties {

    /** 系统提示词配置 */
    private System system;

    @Data
    public static class System {
        /** 主对话智能体提示词 */
        private Chat chat;
        /** 意图路由智能体提示词 */
        private Chat routeAgent;
        /** 睡眠顾问提示词 */
        private Chat sleepAgent;
        /** 饮食顾问提示词 */
        private Chat dietAgent;
        /** 运动顾问提示词 */
        private Chat exerciseAgent;
        /** 打卡助手提示词 */
        private Chat checkinAgent;
        /** 健康知识顾问提示词 */
        private Chat knowledgeAgent;

        @Data
        public static class Chat {
            /** 提示词文本 */
            private String text;
        }
    }
}
