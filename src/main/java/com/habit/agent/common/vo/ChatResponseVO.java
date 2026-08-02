package com.habit.agent.common.vo;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话响应（POST /api/chat 非流式）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseVO {

    /** 会话 ID。 */
    private String conversationId;

    /** 角色：ASSISTANT。 */
    private String role;

    /** 回复内容。 */
    private String content;

    /** 元信息（tokensUsed / model / duration）。 */
    private Metadata metadata;

    /** 创建时间。 */
    private LocalDateTime createTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metadata {
        private Integer tokensUsed;
        private String model;
        private Long duration;
    }
}
