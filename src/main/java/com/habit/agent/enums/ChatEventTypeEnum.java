package com.habit.agent.enums;

import lombok.Getter;

/**
 * 聊天消息事件类型（流式 SSE 协议）。
 *
 * <p>与 PRD 1.4 一致：1001=数据事件 / 1002=停止事件 / 1003=参数事件。</p>
 * <p>本期流式对话只下发 1001 + 1002（1003 即「工具调用参数」类消息，按需求不返回）。</p>
 */
@Getter
public enum ChatEventTypeEnum {

    DATA(1001, "数据事件"),
    STOP(1002, "停止事件"),
    PARAM(1003, "参数事件");

    private final int value;
    private final String desc;

    ChatEventTypeEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
