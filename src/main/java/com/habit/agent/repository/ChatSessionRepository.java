package com.habit.agent.repository;

import com.habit.agent.entity.mongo.ChatSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 会话（ChatSession）MongoDB 仓储。
 *
 * <p>对应集合 {@code chatSession}（见 {@code sql/mongo-init.js}）。
 * 会话持久化、不自动删除（无 TTL），见开发文档 1.6 / 8.4 节。</p>
 */
@Repository
public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {

    /** 按业务会话 ID 查询（uk_session_id 唯一索引） */
    Optional<ChatSession> findBySessionId(String sessionId);

    /** 按用户 ID 查询其全部会话（idx_user 索引），用于会话列表 */
    List<ChatSession> findByUserId(Long userId);
}
