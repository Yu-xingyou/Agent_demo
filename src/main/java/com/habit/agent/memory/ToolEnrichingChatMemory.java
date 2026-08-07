package com.habit.agent.memory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;

/**
 * 装饰器：在消息落库前，把本轮 {@code TOOL} 响应合并进 {@code ASSISTANT} 消息，
 * 使工具查出的结构化数据随助手消息一起持久化（修复"工具结果不入库/查询看不到"问题）。
 *
 * <p>对应天机 1.0 示例中 "ToolResultHolder + MyAssistantMessage(params)" 的思路，
 * 在本项目 Spring AI 2.0 + Mongo 架构下的等价实现：不依赖 Redis/ToolResultHolder，
 * 而是利用消息自带的 {@code metadata} 承载 params。</p>
 */
public class ToolEnrichingChatMemory implements ChatMemory {

    private final ChatMemory delegate;

    public ToolEnrichingChatMemory(ChatMemory delegate) {
        this.delegate = delegate;
    }

    @Override
    public void add(String conversationId, Message message) {
        delegate.add(conversationId, message);
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        // 提取本轮所有 TOOL 响应，按出现顺序合并为 params
        Map<String, Object> toolParams = new LinkedHashMap<>();
        for (Message m : messages) {
            if (m instanceof ToolResponseMessage trm) {
                trm.getResponses().forEach(resp ->
                        toolParams.put(resp.name(), resp.responseData()));
            }
        }

        // 找到最后一个 ASSISTANT 消息，把 toolParams 挂上去
        List<Message> enriched = new ArrayList<>(messages);
        if (!toolParams.isEmpty()) {
            for (int i = enriched.size() - 1; i >= 0; i--) {
                if (enriched.get(i) instanceof AssistantMessage am) {
                    enriched.set(i, new MyAssistantMessage(
                            am.getText(),
                            am.getMetadata(),
                            am.getToolCalls(),
                            am.getMedia(),
                            toolParams));
                    break;
                }
            }
        }
        delegate.add(conversationId, enriched);
    }

    @Override
    public List<Message> get(String conversationId) {
        return delegate.get(conversationId);
    }

    @Override
    public void clear(String conversationId) {
        delegate.clear(conversationId);
    }
}
