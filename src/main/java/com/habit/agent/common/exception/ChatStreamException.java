package com.habit.agent.common.exception;

import lombok.Getter;

/**
 * 流式对话异常（阶段五 5-2 真流式改造）。
 *
 * <p>对齐现有 {@link AiCallException} 体系，额外携带 {@code retryable} 标记，
 * 供 {@code ChatServiceImpl} 重试过滤与 {@code error} 事件生成使用。
 *
 * <p>错误码约定：
 * <ul>
 *   <li>{@code AI_TIMEOUT}：模型响应超时（120s）。</li>
 *   <li>{@code AI_UPSTREAM}：上游模型/网络不可用（5xx、连接重置等，可重试）。</li>
 *   <li>{@code AI_SAFETY}：安全拦截（不可重试）。</li>
 *   <li>{@code AI_UNKNOWN}：其他未知错误。</li>
 * </ul>
 */
@Getter
public class ChatStreamException extends RuntimeException {

    private final String errorCode;
    private final boolean retryable;

    public ChatStreamException(String errorCode, String message, boolean retryable) {
        super(message);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    public ChatStreamException(String errorCode, String message, boolean retryable, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    /** 是否可重试异常（超时、5xx、连接类）。 */
    public static boolean isRetryable(Throwable t) {
        if (t == null) {
            return false;
        }
        String msg = t.getMessage() == null ? "" : t.getMessage().toLowerCase();
        return msg.contains("timed out")
                || msg.contains("timeout")
                || msg.contains("504")
                || msg.contains("502")
                || msg.contains("503")
                || msg.contains("connection reset")
                || msg.contains("connection refused")
                || msg.contains("unexpected end of stream")
                || (t instanceof java.net.SocketTimeoutException)
                || (t instanceof java.io.IOException);
    }

    public static ChatStreamException of(Throwable t) {
        boolean retryable = isRetryable(t);
        String code = retryable ? "AI_UPSTREAM" : "AI_UNKNOWN";
        return new ChatStreamException(code, t.getMessage() == null ? "对话生成失败" : t.getMessage(),
                retryable, t);
    }
}
