package com.habit.agent.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 历史会话列表项视图对象（ChatSessionVO）。
 *
 * <p>用于 {@code GET /api/sessions/history} 接口返回，仅包含会话列表展示所需字段。
 * 历史会话按更新时间划分到「当天 / 最近30天 / 最近1年 / 1年以上」四组。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionVO {

    /**
     * 会话 id
     */
    private String sessionId;

    /**
     * 会话标题（首条消息生成或用户自定义）
     */
    private String title;

    /**
     * 最后更新时间
     */
    private LocalDateTime updateTime;
}
