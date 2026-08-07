package com.habit.agent.aigc.repository;

import com.habit.agent.aigc.entity.mongo.ChatMessageDoc;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * 聊天消息仓储（MongoDB chatMessage）
 */
public interface ChatMessageRepository extends MongoRepository<ChatMessageDoc, String> {

    List<ChatMessageDoc> findBySessionIdOrderByCreateTimeAsc(String sessionId);

    long deleteBySessionId(String sessionId);
}
