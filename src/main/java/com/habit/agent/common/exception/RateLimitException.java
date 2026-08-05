package com.habit.agent.common.exception;

/**
 * 请求限流异常（429）。由 {@link RateLimitInterceptor} 在超出令牌桶配额时抛出，
 * 由 {@link GlobalExceptionHandler} 统一转换为 429 响应。
 */
public class RateLimitException extends RuntimeException {

    private final int code;

    public RateLimitException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
