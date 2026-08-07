package com.habit.agent.aigc.repository;

import com.habit.agent.aigc.entity.mongo.ChatSessionDoc;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * 会话仓储（MongoDB chatSession）
 */
public interface ChatSessionRepository extends MongoRepository<ChatSessionDoc, String> {

    Optional<ChatSessionDoc> findBySessionId(String sessionId);

    List<ChatSessionDoc> findByUserIdOrderByLastMessageTimeDesc(Long userId);

    void deleteBySessionId(String sessionId);
}
