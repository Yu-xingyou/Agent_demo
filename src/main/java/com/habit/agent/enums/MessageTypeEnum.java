package com.habit.agent.enums;

import lombok.Getter;

/**
 * 消息类型枚举。
 *
 * <p>用于历史消息查询（PRD 2.2 会话详情）区分消息归属：
 * {@code USER} 表示用户提问，{@code ASSISTANT} 表示 AI 的回答。</p>
 *
 * <p>枚举名与 Spring AI {@code org.springframework.ai.chat.messages.MessageType}
 * 的 {@code USER} / {@code ASSISTANT} 保持一致，便于通过
 * {@code MessageTypeEnum.valueOf(messageType.name())} 直接转换。</p>
 *
 * <p>说明：本项目未引入天机课程模板的 {@code BaseEnum} 基础接口，
 * 故此处沿用与 {@link ChatEventTypeEnum} 相同的 {@code value/desc} 结构；
 * 序列化时因重写 {@link #toString()} 并配合 Jackson 默认行为输出枚举名（如 {@code "USER"}）。</p>
 */
@Getter
public enum MessageTypeEnum {

    USER(1, "用户提问"),
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
