package com.habit.agent.common.constant;

/**
 * 全局常量定义（子模块 2-2）
 */
public final class AgentConstants {

    private AgentConstants() {
    }

    /** 默认用户 ID（单用户演示场景） */
    public static final Long DEFAULT_USER_ID = 1L;

    /** 默认用户名 */
    public static final String DEFAULT_USERNAME = "demo";

    /** 默认用户昵称 */
    public static final String DEFAULT_NICKNAME = "演示用户";

    // ===== 错误码 =====
    /** 参数校验异常 */
    public static final int CODE_PARAM_ERROR = 40001;
    /** 日期范围异常 */
    public static final int CODE_DATE_RANGE_ERROR = 40002;
    /** 枚举值异常 */
    public static final int CODE_ENUM_ERROR = 40003;

    /** 用户不存在 */
    public static final int CODE_USER_NOT_FOUND = 40401;
    /** 习惯记录不存在 */
    public static final int CODE_RECORD_NOT_FOUND = 40402;
    /** 习惯目标不存在 */
    public static final int CODE_GOAL_NOT_FOUND = 40403;
    /** 提醒不存在 */
    public static final int CODE_REMINDER_NOT_FOUND = 40404;

    /** 重复目标 */
    public static final int CODE_DUPLICATE_GOAL = 40901;

    /** 系统内部错误 */
    public static final int CODE_SYSTEM_ERROR = 50001;
    /** 数据库错误 */
    public static final int CODE_DB_ERROR = 50002;
    /** 未知系统异常 */
    public static final int CODE_UNKNOWN_ERROR = 50003;
}
