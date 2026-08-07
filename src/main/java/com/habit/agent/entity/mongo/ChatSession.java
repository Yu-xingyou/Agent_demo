package com.habit.agent.entity.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
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
 * <p>本地场景会话<b>持久化、不自动删除</b>，故无 {@code expireAt} 字段、不建 TTL 索引。
 * 会话「关闭」为轻量业务操作，不在实体中保留 status 冗余字段（按字段规范演示用）。</p>
 *
 * <p>集合索引（与 {@code sql/mongo-init.js} 对齐）：
 * <ul>
 *     <li>{@code uk_session_id}：sessionId 唯一索引</li>
 *     <li>{@code idx_user_status}：userId 复合索引（查询用户会话）</li>
 * </ul>
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chatSession")
public class ChatSession {

    /** MongoDB 文档主键（由 MongoDB 自动生成） */
    @Id
    private String id;

    /** 会话 ID（业务标识，唯一索引 uk_session_id）。一般在创建会话时由后端生成 */
    @Indexed(name = "uk_session_id", unique = true)
    @Field("sessionId")
    private String sessionId;

    /** 所属用户 ID（单用户演示场景默认 {@code AgentConstants.DEFAULT_USER_ID}） */
    @Indexed(name = "idx_user")
    @Field("userId")
    private Long userId;

    /** 会话标题（侧边栏展示，可由首条消息或 AI 生成） */
    @Field("title")
    private String title;

    /** 创建时间 */
    @Field("createTime")
    private LocalDateTime createTime;

    /** 最后更新时间（每次收发消息刷新，用于会话列表倒序） */
    @Field("updateTime")
    private LocalDateTime updateTime;

    /** 创建人（演示场景默认 DEFAULT_USER_ID） */
    @Field("creater")
    private Long creater;

    /** 更新人 */
    @Field("updater")
    private Long updater;
}
