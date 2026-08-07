package com.habit.agent.entity.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * 会话（Session）文档 —— 对应 MongoDB {@code chatSession} 集合。
 *
 * <p>会话是用户与助手之间的一段持续交互上下文，一个会话可包含多轮对话（conversation）。
 * 与「对话」（conversationId，存于 {@code chatMessage}）是不同概念，见开发文档 1.6 节。</p>
 *
 * <p>集合索引（与 {@code sql/mongo-init.js} 对齐）：
 * <ul>
 *     <li>{@code uk_session_id}：sessionId 唯一索引</li>
 *     <li>{@code idx_user_status}：userId + status 复合索引（查询用户活跃会话）</li>
 * </ul>
 * </p>
 *
 * <p><b>持久化说明：</b>本地场景下会话<b>不自动删除</b>，故<b>不设置 TTL 过期索引</b>，
 * 也不保留 {@code expireAt} 字段（详见开发文档 1.6 与 mongo-init.js 注释）。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chatSession")
@CompoundIndex(name = "idx_user_status", def = "{'userId': 1, 'status': 1}")
public class ChatSession {

    /** MongoDB 文档主键（由 MongoDB 自动生成） */
    @Id
    private String id;

    /** 会话 ID（业务标识，唯一索引 uk_session_id）。一般在创建会话时由后端生成 */
    @Indexed(name = "uk_session_id", unique = true)
    @Field("sessionId")
    private String sessionId;

    /** 所属用户 ID（单用户演示场景默认 {@code AgentConstants.DEFAULT_USER_ID}） */
    @Field("userId")
    private Long userId;

    /** 会话标题（侧边栏展示，可由首条消息或 AI 生成） */
    @Field("title")
    private String title;

    /**
     * 会话状态：ACTIVE（活跃）/ CLOSED（已关闭）
     * @see #STATUS_ACTIVE
     * @see #STATUS_CLOSED
     */
    @Field("status")
    private String status;

    /** 创建时间 */
    @Field("createTime")
    private LocalDateTime createTime;

    /** 最后更新时间（每次收发消息刷新，用于会话列表倒序） */
    @Field("updateTime")
    private LocalDateTime updateTime;

    // ===== 状态常量 =====
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_CLOSED = "CLOSED";
}
