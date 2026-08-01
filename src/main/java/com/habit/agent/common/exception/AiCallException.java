package com.habit.agent.common.exception;

import lombok.Getter;

/**
 * AI 调用异常（子模块 2-2）
 *
 * 错误码: 50301(AI服务不可用) / 50302(AI响应解析失败) / 50303(AI调用超时)
 */
@Getter
public class AiCallException extends RuntimeException {

    private final int code;

    public AiCallException(int code, String message) {
        super(message);
        this.code = code;
    }

    public AiCallException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
