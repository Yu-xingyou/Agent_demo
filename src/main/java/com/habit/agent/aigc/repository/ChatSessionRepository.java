package com.habit.agent.aigc.repository;

import com.habit.agent.aigc.entity.mongo.ChatSessionDoc;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * 会话仓储（MongoDB chatSession）
 */
public interface ChatSessionRepository extends MongoRepository<ChatSessionDoc, String> {

    /**
     * 按会话 id 查询会话文档
     *
     * @param sessionId 会话 id
     * @return 命中的会话文档；不存在时返回空 Optional
     */
    Optional<ChatSessionDoc> findBySessionId(String sessionId);

    /**
     * 按用户 id 查询其全部会话（按最后消息时间倒序）
     *
     * @param userId 用户 id
     * @return 该用户的会话文档列表（倒序）
     */
    List<ChatSessionDoc> findByUserIdOrderByLastMessageTimeDesc(Long userId);

    /**
     * 删除指定会话（仅删除会话文档，消息与记忆由业务层级联清理）
     *
     * @param sessionId 会话 id
     */
    void deleteBySessionId(String sessionId);
}
