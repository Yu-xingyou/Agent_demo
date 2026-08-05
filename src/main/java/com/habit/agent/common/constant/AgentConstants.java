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

    // ===== MongoDB Key 前缀 =====
    public static final String CHAT_HISTORY_PREFIX = "chat:";
    public static final String AI_ANALYSIS_PREFIX = "analysis:";

    // ===== TTL 时长（秒） =====
    /** 日报 TTL: 1 天 */
    public static final int DAILY_REPORT_TTL = 86400;
    /** 周报 TTL: 7 天 */
    public static final int WEEKLY_REPORT_TTL = 604800;
    /** 月报 TTL: 30 天 */
    public static final int MONTHLY_REPORT_TTL = 2592000;

    // ===== 错误码 =====
    /** 参数校验异常 */
    public static final int CODE_PARAM_ERROR = 40001;
    /** 日期范围异常 */
    public static final int CODE_DATE_RANGE_ERROR = 40002;
    /** 枚举值异常 */
    public static final int CODE_ENUM_ERROR = 40003;
    /** 知识库文档上传异常（格式/大小/编码不合法） */
    public static final int CODE_RAG_UPLOAD_ERROR = 40004;

    /** 用户不存在 */
    public static final int CODE_USER_NOT_FOUND = 40401;
    /** 习惯记录不存在 */
    public static final int CODE_RECORD_NOT_FOUND = 40402;
    /** 习惯目标不存在 */
    public static final int CODE_GOAL_NOT_FOUND = 40403;
    /** 提醒不存在 */
    public static final int CODE_REMINDER_NOT_FOUND = 40404;
    /** AI 分析不存在 */
    public static final int CODE_ANALYSIS_NOT_FOUND = 40405;
    /** 对话记录不存在 */
    public static final int CODE_SESSION_NOT_FOUND = 40406;
    /** 知识库文档不存在 */
    public static final int CODE_RAG_DOC_NOT_FOUND = 40407;

    /** 重复目标 */
    public static final int CODE_DUPLICATE_GOAL = 40901;

    /** AI 服务不可用 */
    public static final int CODE_AI_UNAVAILABLE = 50301;
    /** AI 响应解析失败 */
    public static final int CODE_AI_PARSE_ERROR = 50302;
    /** AI 调用超时 */
    public static final int CODE_AI_TIMEOUT = 50303;
    /** 知识库检索/写入失败（向量库不可用） */
    public static final int CODE_RAG_SEARCH_ERROR = 50304;

    // ===== RAG 知识库参数（阶段八） =====
    /** 预设知识库文档所在 classpath 目录 */
    public static final String RAG_PRESET_DOCS_PATTERN = "classpath:rag-docs/*.md";
    /** 检索默认返回条数 */
    public static final int RAG_DEFAULT_TOP_K = 3;
    /** 检索最大返回条数 */
    public static final int RAG_MAX_TOP_K = 10;
    /** 相似度阈值（低于该分数的片段视为无关，不返回） */
    public static final double RAG_SIMILARITY_THRESHOLD = 0.5;
    /** 文档分块目标 token 数 */
    public static final int RAG_CHUNK_SIZE = 500;
    /** 相邻分块重叠 token 数（保持语义连续） */
    public static final int RAG_CHUNK_OVERLAP = 100;
    /** 上传文档大小上限（字节，2MB） */
    public static final long RAG_UPLOAD_MAX_SIZE = 2L * 1024 * 1024;
    /** 向量库集合名（须与 spring.ai.vectorstore.mongodb.collection-name 一致） */
    public static final String RAG_COLLECTION_NAME = "habit_knowledge";

    /** 系统内部错误 */
    public static final int CODE_SYSTEM_ERROR = 50001;
    /** 数据库错误 */
    public static final int CODE_DB_ERROR = 50002;
    /** 未知系统异常 */
    public static final int CODE_UNKNOWN_ERROR = 50003;
}
