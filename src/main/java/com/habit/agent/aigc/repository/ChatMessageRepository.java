package com.habit.agent.aigc.repository;

import com.habit.agent.aigc.entity.mongo.ChatMessageDoc;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * 聊天消息仓储（MongoDB chatMessage）
 */
public interface ChatMessageRepository extends MongoRepository<ChatMessageDoc, String> {

    /**
     * 按会话 id 查询全部历史消息（按创建时间升序）
     *
     * @param sessionId 会话 id
     * @return 该会话下的消息文档列表
     */
    List<ChatMessageDoc> findBySessionIdOrderByCreateTimeAsc(String sessionId);

    /**
     * 删除指定会话的全部消息
     *
     * @param sessionId 会话 id
     * @return 被删除的文档数量
     */
    long deleteBySessionId(String sessionId);
}
