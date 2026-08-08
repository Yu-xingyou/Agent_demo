package com.habit.agent.aigc.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话 VO（扩展会话列表所需字段）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionVO {

    /**
     * 会话 ID，用于唯一标识当前 AI 助手会话
     */
    private String sessionId;

    /**
     * AI 助手标题
     */
    private String title;

    /**
     * AI 助手描述
     */
    private String describe;

    /**
     * 示例列表
     */
    private List<Example> examples;

    /**
     * 最后一条消息时间（会话列表倒序展示）
     */
    private LocalDateTime lastMessageTime;

    /**
     * Example 类表示每个示例的标题和描述
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Example {

        /**
         * 示例标题
         */
        private String title;

        /**
         * 示例描述
         */
        private String describe;
    }
}
