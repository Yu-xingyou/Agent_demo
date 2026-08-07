package com.habit.agent.vo;

import com.habit.agent.enums.MessageTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 历史消息视图对象（PRD 2.2 会话详情）。
 *
 * <p>由会话记忆（MongoDB {@code ai_chat_memory}）中的 Spring AI
 * {@code Message} 转换而来，仅保留用户提问与 AI 回答两类消息，
 * 供前端回显历史对话。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageVO {

    /**
     * 消息类型，USER 表示用户提问，ASSISTANT 表示 AI 的回答。
     */
    private MessageTypeEnum type;

    /**
     * 消息内容。
     */
    private String content;

    /**
     * 附加参数（预留：如工具调用参数、引用来源等，本期为空）。
     */
    private Map<String, Object> params;
}
