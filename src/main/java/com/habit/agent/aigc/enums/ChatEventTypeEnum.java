package com.habit.agent.aigc.enums;

import lombok.Getter;

/**
 * 聊天消息事件类型（SSE 事件）
 */
@Getter
public enum ChatEventTypeEnum {

    /** 数据事件：流式输出的一段文本内容 */
    DATA(1001, "数据事件"),
    /** 停止事件：对话正常结束 */
    STOP(1002, "停止事件"),
    /** 参数事件：携带结构化的工具调用参数等信息 */
    PARAM(1003, "参数事件");

    private final int value;
    private final String desc;

    ChatEventTypeEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @Override
    public String toString() {
        return this.name();
    }
}
