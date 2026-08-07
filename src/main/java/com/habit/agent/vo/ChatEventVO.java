package com.habit.agent.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流式对话事件视图对象（SSE 单行 payload）。
 *
 * <p>协议（见 PRD 1.4「流式响应协议」）：响应媒体类型为 {@code text/event-stream}，
 * 每一行形如 {@code data:{"eventData":...,"eventType":1001}${'\n'}${'\n'}}，
 * 不含具名 event 类型、不含 token 统计字段。</p>
 *
 * <p>eventType 取值见 {@link com.habit.agent.enums.ChatEventTypeEnum}：
 * 1001=数据事件（回答分片）/ 1002=停止事件（流式结束，eventData 为 null）/ 1003=参数事件（本期不使用）。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatEventVO {

    /**
     * 事件数据。
     * <ul>
     *   <li>eventType=1001 时为回答文本分片（String）</li>
     *   <li>eventType=1002 时为 null</li>
     * </ul>
     */
    private Object eventData;

    /**
     * 事件类型，取值 1001/1002/1003（见 {@code ChatEventTypeEnum}）。
     */
    private int eventType;
}
