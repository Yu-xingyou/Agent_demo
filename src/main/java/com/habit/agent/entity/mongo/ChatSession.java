package com.habit.agent.entity.mongo;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Data;

/**
 * 会话元数据（MongoDB 集合 {@code chatSession}）。
 *
 * <p>与 Spring AI {@code MessageWindowChatMemory} 的对话记忆解耦：记忆库只存消息，
 * 本实体存会话维度的元数据（标题、状态、过期时间等），供 SessionController 管理。
 */
@Data
@Document(collection = "chatSession")
public class ChatSession {

    /** 会话 ID（UUID，作为消息记忆的 conversationId）。 */
    @Id
    private String conversationId;

    /** 用户 ID（当前为单用户 demo，固定 1）。 */
    @Field("user_id")
    private Long userId;

    /** 会话标题（前端展示，默认「新对话」）。 */
    @Field("title")
    private String title;

    /** 会话状态：ACTIVE / CLOSED。 */
    @Field("status")
    private String status = "ACTIVE";

    /** 消息条数（冗余计数，便于列表展示）。 */
    @Field("message_count")
    private int messageCount = 0;

    /** 最后一条消息时间。 */
    @Field("last_message_time")
    private LocalDateTime lastMessageTime;

    /** 创建时间。 */
    @Field("create_time")
    private LocalDateTime createTime;

    /** 过期时间（用于消息窗口自动清理，可选）。 */
    @Field("expire_at")
    private LocalDateTime expireAt;
}
