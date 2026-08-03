package com.habit.agent.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.entity.mongo.ChatSession;
import com.habit.agent.repository.mongo.ChatSessionRepository;
import com.habit.agent.service.ChatService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

/**
 * 阶段五（对话记忆与流式输出）ChatService 实现。
 *
 * <p>Spring AI 2.0.0 流式策略：
 * <ul>
 *   <li><b>纯文本轮次</b>：`.stream().content()` 真逐字输出（SSE chunk 事件逐 token 推送）。</li>
 *   <li><b>工具调用轮次</b>：ChunkMerger 因 DashScope 流式 tool_calls 缺 index 字段崩溃
 *       → onErrorResume 捕获 → 降级为 .call() 非流式获取完整回复
 *       → 以分片 Flux 模拟流式 + 前置 {@code __TC__} 标记通知 Controller 发送 tool_call SSE 事件。</li>
 * </ul>
 *
 * <p>ChunkMerger bug 根因：Spring AI 2.0.0 的 {@code OpenAiChatModel$ChunkMerger} 对流式
 * tool_calls 分片的 Optional 直接 .get() 未检查 isPresent()。通义千问 DashScope 的流式响应
 * 不总是返回 index 字段，导致 NoSuchElementException。该缺陷需等 Spring AI 2.0.1 修复。
 * 全网截至 2026-08 无兼容 DashScope 流式工具调用的完整攻略。
 */
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    /** 工具调用降级信号前缀：Controller 检测此前缀发送 tool_call SSE 事件而非 chunk 事件。 */
    static final String TC_MARKER = "__TC__";

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final ChatSessionRepository chatSessionRepository;

    @Override
    public String chat(String userMessage, String conversationId) {
        final String cid = ensureSession(userMessage, conversationId);
        return chatOnce(userMessage, cid);
    }

    @Override
    public Flux<String> stream(String userMessage, String conversationId) {
        final String cid = ensureSession(userMessage, conversationId);
        return chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, cid))
                .options(OpenAiChatOptions.builder().parallelToolCalls(false))
                .stream()
                .content()
                .onErrorResume(t -> {
                    if (isChunkMergeFailure(t)) {
                        log.warn("[流式降级] conversationId={}，触发工具调用，降级为分片模拟流式，原因：{}",
                                cid, t.getMessage());
                        String full = chatOnce(userMessage, cid);
                        return Flux.concat(
                                Flux.just(TC_MARKER + "{\"status\":\"executing\",\"message\":\"正在查询你的数据…\"}"),
                                chunkedFlux(full)
                        );
                    }
                    return Flux.error(t);
                });
    }

    private String chatOnce(String userMessage, String conversationId) {
        return chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .options(OpenAiChatOptions.builder().parallelToolCalls(false))
                .call()
                .content();
    }

    @Override
    public List<Map<String, String>> getMessages(String conversationId) {
        List<org.springframework.ai.chat.messages.Message> msgs =
                chatMemory.get(conversationId, Integer.MAX_VALUE);
        List<Map<String, String>> result = new ArrayList<>();
        for (org.springframework.ai.chat.messages.Message m : msgs) {
            // 跳过 SystemMessage，只返回用户和助手的消息
            if (m instanceof org.springframework.ai.chat.messages.SystemMessage) continue;
            Map<String, String> item = new HashMap<>();
            String role = "ai";
            if (m instanceof org.springframework.ai.chat.messages.UserMessage) {
                role = "user";
            } else if (m instanceof org.springframework.ai.chat.messages.AssistantMessage) {
                role = "ai";
            }
            item.put("role", role);
            item.put("text", m.getText());
            result.add(item);
        }
        return result;
    }

    @Override
    public String generateTitle(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return "新会话";
        }
        try {
            String title = chatClient.prompt()
                    .user("用不超过15个字总结这段对话的主题：" + userMessage)
                    .options(OpenAiChatOptions.builder().temperature(0.3).build())
                    .call()
                    .content();
            if (title == null || title.isBlank() || title.length() > 30) {
                return userMessage.length() > 15 ? userMessage.substring(0, 15) + "…" : userMessage;
            }
            return title.trim();
        } catch (Exception e) {
            log.warn("[标题生成失败] 降级为截断首条消息", e);
            return userMessage.length() > 15 ? userMessage.substring(0, 15) + "…" : userMessage;
        }
    }

    /**
     * 将完整响应文本按每 3 字符一组切分，通过 Flux.fromIterable 逐片发射，
     * 在工具调用轮次中模拟流式逐字输出体验。
     */
    private Flux<String> chunkedFlux(String text) {
        if (text == null || text.isEmpty()) {
            return Flux.empty();
        }
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += 3) {
            chunks.add(text.substring(i, Math.min(i + 3, text.length())));
        }
        return Flux.fromIterable(chunks);
    }

    /**
     * 判断异常是否来自 Spring AI 流式 chunk 合并器（ChunkMerger）对工具调用 chunk 的不兼容缺陷。
     * 沿 cause 链遍历，匹配 ChunkMerger 类名、NoSuchElementException，或
     * IllegalArgumentException 且消息含 "tool call"。
     */
    private boolean isChunkMergeFailure(Throwable t) {
        Throwable cause = t;
        while (cause != null) {
            String className = cause.getClass().getName();
            if (className.contains("OpenAiChatModel$ChunkMerger")
                    || cause instanceof java.util.NoSuchElementException) {
                return true;
            }
            if (cause instanceof IllegalArgumentException
                    && cause.getMessage() != null
                    && cause.getMessage().contains("tool call")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private String ensureSession(String userMessage, String conversationId) {
        if (conversationId != null && !conversationId.isBlank()) {
            return conversationId;
        }
        // 暂无会话时自动创建，并记录首条消息用于标题
        String cid = java.util.UUID.randomUUID().toString();
        ChatSession session = new ChatSession();
        session.setConversationId(cid);
        session.setUserId(AgentConstants.DEFAULT_USER_ID);
        session.setTitle(userMessage.length() > 20 ? userMessage.substring(0, 20) : userMessage);
        session.setStatus("ACTIVE");
        session.setExpireAt(LocalDateTime.now().plusDays(30));
        chatSessionRepository.save(session);
        return cid;
    }
}
