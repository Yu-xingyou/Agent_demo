package com.habit.agent.aigc.memory;

import lombok.Data;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.content.Media;

import java.util.List;
import java.util.Map;

/**
 * 消息序列化载体（MongoDB 记忆存储格式）
 */
@Data
public class MyMessage {

    /** 消息类型（对应 MessageType 枚举名，如 USER/ASSISTANT/SYSTEM/TOOL） */
    private String messageType;
    /** 消息元数据（如会话 id、来源等） */
    private Map<String, Object> metadata = Map.of();
    /** 用户消息携带的多媒体内容列表 */
    private List<Media> media = List.of();
    /** 助手消息携带的工具调用列表 */
    private List<AssistantMessage.ToolCall> toolCalls = List.of();
    /** 消息文本内容 */
    private String textContent;
    /** 工具响应消息携带的响应结果列表 */
    private List<ToolResponseMessage.ToolResponse> toolResponses = List.of();
    /** 附加参数字段（用于扩展） */
    private Map<String, Object> params = Map.of();
}
