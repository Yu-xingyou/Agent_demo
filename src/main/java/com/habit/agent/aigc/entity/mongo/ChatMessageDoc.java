package com.habit.agent.aigc.entity.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天消息文档（本地 MongoDB chatMessage 集合）
 * 保存完整对话历史，供前端历史回显（与 chatMemory 模型记忆职责分离）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chatMessage")
@CompoundIndex(name = "idx_conv_time", def = "{'sessionId': 1, 'createTime': 1}")
public class ChatMessageDoc implements Serializable {

    @Id
    private String id;

    /** 会话 id（关联 chatSession.sessionId） */
    @Indexed
    private String sessionId;

    /** 消息角色：USER / ASSISTANT（与 MessageTypeEnum 对齐） */
    private String role;

    /** 消息内容 */
    private String content;

    /** 消息类型附加值（预留：如工具调用状态） */
    private String extra;

    /** 创建时间 */
    private LocalDateTime createTime;
}
