package com.habit.agent.aigc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话请求 DTO（参照天机学堂 tj-aigc ChatDTO）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatDTO {

    /**
     * 用户的问题
     */
    private String question;

    /**
     * 会话 id
     */
    private String sessionId;
}
