package com.habit.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流式对话请求体（POST /api/chat）。
 *
 * <p>作为 Controller 入参载体，对应请求 JSON：
 * {@code {"message":"...","sessionId":"..."}}。</p>
 *
 * <p>字段命名对齐示例：{@code message}=用户问题，
 * {@code sessionId}=会话 ID（首轮可省略，由后端生成并通过响应头下发）。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "流式对话请求")
public class ChatMessageDTO {

    @Schema(description = "用户的问题", example = "如何改善睡眠质量？")
    private String message;

    @Schema(description = "会话ID（首轮可省略）", example = "c8f0e1a2-3b4d-4e5f")
    private String sessionId;
}
