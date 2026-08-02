package com.habit.agent.service;

import java.time.LocalDateTime;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.stereotype.Service;

import com.habit.agent.entity.mongo.ChatSession;
import com.habit.agent.repository.mongo.ChatSessionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * 阶段五（对话记忆与流式输出）ChatService。
 *
 * <p>职责：
 * <ul>
 *   <li>非流式对话 {@link #chat(String, String)}：调用 ChatClient，注入对话记忆（按 conversationId 隔离）；</li>
 *   <li>流式对话 {@link #stream(String, String)}：返回响应式 token 流（供 SSE 端点）；</li>
 *   <li>维护会话元数据（消息计数、最后时间、标题提取）。</li>
 * </ul>
 *
 * <p>工具调用、多智能体路由分别在阶段六 / 阶段九扩展，本阶段保持基础对话能力。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;
    private final ChatSessionRepository sessionRepository;

    /** 单用户 demo 固定 userId。 */
    private static final Long DEMO_USER_ID = 1L;

    /**
     * 非流式对话。
     *
     * @param userMessage     用户输入
     * @param conversationId  会话 ID（null 时自动创建）
     * @return 模型完整回复
     */
    public String chat(String userMessage, String conversationId) {
        final String cid = ensureSession(conversationId, userMessage);
        String content = chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, cid))
                .call()
                .content();
        touchSession(cid, userMessage);
        return content;
    }

    /**
     * 流式对话：返回响应式文本流（阶段五接入 SSE 端点 {@code GET /api/chat/stream}）。
     *
     * @param userMessage     用户输入
     * @param conversationId  会话 ID（null 时自动创建）
     * @return 模型回复的响应式文本流
     */
    public Flux<String> stream(String userMessage, String conversationId) {
        final String cid = ensureSession(conversationId, userMessage);
        Flux<String> flux = chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, cid))
                .stream()
                .content();
        // 流式场景下在订阅开始即更新会话元信息（实际完整消息由调用方落库可选）
        touchSession(cid, userMessage);
        return flux;
    }

    /**
     * 确保会话存在：conversationId 为空则新建并持久化 ChatSession 元数据。
     */
    private String ensureSession(String conversationId, String firstMessage) {
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = java.util.UUID.randomUUID().toString();
            ChatSession session = new ChatSession();
            session.setConversationId(conversationId);
            session.setUserId(DEMO_USER_ID);
            session.setTitle(defaultTitle(firstMessage));
            session.setStatus("ACTIVE");
            session.setMessageCount(0);
            session.setCreateTime(LocalDateTime.now());
            sessionRepository.save(session);
            log.debug("[阶段五] 新建会话 {}", conversationId);
        }
        return conversationId;
    }

    /**
     * 更新会话元信息：消息计数 +1、刷新最后消息时间、首次时根据首条消息提炼标题。
     */
    private void touchSession(String conversationId, String userMessage) {
        sessionRepository.findById(conversationId).ifPresent(session -> {
            session.setMessageCount(session.getMessageCount() + 1);
            session.setLastMessageTime(LocalDateTime.now());
            if (session.getTitle() == null || session.getTitle().isBlank()) {
                session.setTitle(defaultTitle(userMessage));
            }
            sessionRepository.save(session);
        });
    }

    private String defaultTitle(String message) {
        if (message == null || message.isBlank()) {
            return "新对话";
        }
        return message.length() <= 20 ? message : message.substring(0, 20) + "…";
    }
}
