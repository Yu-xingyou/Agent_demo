package com.habit.agent.common.exception;

import lombok.Getter;

/**
 * 业务异常基类（子模块 2-2）
 *
 * 携带错误码，由 GlobalExceptionHandler 统一捕获并转换为 Result 响应。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
