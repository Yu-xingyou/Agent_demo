package com.habit.agent.repository.mongo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.habit.agent.entity.mongo.ChatSession;

/**
 * 会话元数据 Repository（自管集合 {@code chatSession}）。
 *
 * <p>注意：对话消息本身由 Spring AI {@code MessageWindowChatMemory}
 * （底层 {@code MongoChatMemoryRepository}）管理，集合自动创建，
 * 不在此 Repository 范围内。
 */
public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {

    List<ChatSession> findByUserIdOrderByLastMessageTimeDesc(Long userId);

    List<ChatSession> findByUserIdAndStatus(Long userId, String status);

    Optional<ChatSession> findByConversationId(String conversationId);
}
