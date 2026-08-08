package com.habit.agent.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 统一 API 响应封装（子模块 2-2）
 *
 * @param <T> 数据载荷类型
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /** 状态码（200 成功，非 200 为错误） */
    private int code;
    /** 提示信息 */
    private String message;
    /** 数据载荷 */
    private T data;

    /**
     * 成功响应（带数据）
     *
     * @param data 业务数据载荷
     * @param <T>  数据载荷类型
     * @return 状态码 200、消息 success 的 Result
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    /**
     * 成功响应（无数据）
     *
     * @param <T> 数据载荷类型
     * @return 状态码 200、消息 success、数据为空的成功 Result
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    /**
     * 错误响应
     *
     * @param code    错误码
     * @param message 错误提示信息
     * @param <T>     数据载荷类型
     * @return 携带错误码与提示的 Result
     */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}
