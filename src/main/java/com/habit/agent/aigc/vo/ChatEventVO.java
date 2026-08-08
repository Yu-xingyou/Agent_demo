package com.habit.agent.aigc.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天 SSE 事件 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatEventVO {

    /**
     * 事件数据（流式文本片段或附加参数）
     */
    private Object eventData;

    /**
     * 事件类型：1001-数据事件，1002-停止事件，1003-参数事件
     */
    private int eventType;
}
