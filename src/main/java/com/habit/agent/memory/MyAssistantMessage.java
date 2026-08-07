package com.habit.agent.memory;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolCall;
import org.springframework.ai.content.Media;

/**
 * 扩展的助手消息，额外携带工具调用结果（params）。
 *
 * <p>问题背景：Agent 调用工具（Tool）查出的结构化结果，原始仅以 {@code TOOL} 类型
 * 消息存在于记忆中，历史查询时通常被过滤，导致前端看不到"工具查到了什么"。
 * 本类把工具结果挂到助手消息上，随会话记忆一起持久化。</p>
 *
 * <p>实现方式：将 {@code params} 并入父类 {@code metadata}（Spring AI 2.0 的
 * {@code MongoChatMemoryRepository} 会序列化 metadata，可原样回读），
 * 从而无需自定义序列化器即可保留 params。</p>
 */
public class MyAssistantMessage extends AssistantMessage {

    public static final String PARAMS_KEY = "tool_params";

    public MyAssistantMessage(String content, Map<String, Object> metadata,
                             List<ToolCall> toolCalls, List<Media> media,
                             Map<String, Object> params) {
        super(content, metadata, toolCalls, media);
        if (params != null && !params.isEmpty()) {
            // 并入 metadata，确保被持久化（metadata 为可变 Map）
            this.getMetadata().put(PARAMS_KEY, params);
        }
    }

    /**
     * 从消息中提取工具结果 params（兼容普通 AssistantMessage 从 metadata 回读）。
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> extractParams(Message message) {
        if (message == null || message.getMetadata() == null) {
            return null;
        }
        Object raw = message.getMetadata().get(PARAMS_KEY);
        return raw instanceof Map ? (Map<String, Object>) raw : null;
    }
}
