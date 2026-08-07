package com.habit.agent.aigc.entity.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天会话文档（本地 MongoDB chatSession 集合）
 * TTL 7 天自动过期，过期字段 expireAt 由服务写入
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chatSession")
public class ChatSessionDoc implements Serializable {

    @Id
    private String id;

    /** 会话 id（业务唯一，对外暴露） */
    @Indexed(unique = true)
    private String sessionId;

    /** 用户 id（单用户演示场景固定为 1） */
    @Indexed
    private Long userId;

    /** 会话标题（首轮对话后由 AI 生成，未生成为空） */
    private String title;

    /** 会话描述（助手说明） */
    private String describe;

    /** 最后一条消息时间（会话列表按此倒序） */
    @Field("last_message_time")
    private LocalDateTime lastMessageTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** TTL 过期时间（创建时间 + 7 天），由 @PostPersist 逻辑写入 */
    @Indexed(expireAfterSeconds = 604800) // 7 * 24 * 3600
    private LocalDateTime expireAt;
}
