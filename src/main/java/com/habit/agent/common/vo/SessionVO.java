package com.habit.agent.common.vo;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话列表/详情视图对象（SessionController）。映射 MongoDB {@code chatSession} 实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionVO {

    private String conversationId;
    private Long userId;
    private String title;
    private String status;
    private int messageCount;
    private LocalDateTime lastMessageTime;
    private LocalDateTime createTime;
    private LocalDateTime expireAt;
}
