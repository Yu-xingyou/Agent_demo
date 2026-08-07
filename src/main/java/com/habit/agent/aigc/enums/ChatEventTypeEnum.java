package com.habit.agent.aigc.enums;

import lombok.Getter;

/**
 * 聊天消息事件类型（SSE 事件，参照天机学堂 tj-aigc ChatEventTypeEnum）
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

    @Override
    public String toString() {
        return this.name();
    }
}
