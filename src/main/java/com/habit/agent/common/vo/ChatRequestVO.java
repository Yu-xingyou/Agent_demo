package com.habit.agent.common.vo;

import lombok.Data;

/**
 * 对话请求（POST /api/chat）。
 */
@Data
public class ChatRequestVO {

    /** 用户消息。 */
    private String message;

    /** 会话 ID，不传则自动获取/创建。 */
    private String conversationId;
}
