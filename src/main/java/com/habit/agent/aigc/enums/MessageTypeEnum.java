package com.habit.agent.aigc.enums;

import lombok.Getter;

/**
 * 消息类型枚举
 */
@Getter
public enum MessageTypeEnum {

    /** 用户提问消息 */
    USER(1, "用户提问"),
    /** AI 的回答消息 */
    ASSISTANT(2, "AI的回答");

    private final int value;
    private final String desc;

    MessageTypeEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @Override
    public String toString() {
        return this.name();
    }
}
