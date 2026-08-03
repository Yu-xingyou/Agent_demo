package com.habit.agent.service.impl;

import java.time.LocalDateTime;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.entity.mongo.ChatSession;
import com.habit.agent.repository.mongo.ChatSessionRepository;
import com.habit.agent.service.ChatService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

/**
 * 阶段五（对话记忆与流式输出）ChatService 实现
 */
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final ChatSessionRepository chatSessionRepository;

    @Override
    public String chat(String userMessage, String conversationId) {
        final String cid = ensureSession(userMessage, conversationId);
        return chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, cid))
                .call()
                .content();
    }

    @Override
    public Flux<String> stream(String userMessage, String conversationId) {
        final String cid = ensureSession(userMessage, conversationId);
        return chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, cid))
                .stream()
                .content();
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
