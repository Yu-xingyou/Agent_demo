package com.habit.agent.aigc.memory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 基于 MongoDB 实现的 ChatMemoryRepository（本地 MongoDB chatMemory 集合）
 * 将 LIST 存储改为 MongoDB 文档：
 * 每文档 _id = conversationId，messages 字段为消息 JSON 字符串数组
 */
@Slf4j
@RequiredArgsConstructor
public class MongoChatMemoryRepository implements ChatMemoryRepository {

    /** MongoDB 集合名 */
    public static final String COLLECTION = "chatMemory";

    private final MongoTemplate mongoTemplate;

    @Override
    public List<String> findConversationIds() {
        return mongoTemplate.findAll(Document.class, COLLECTION).stream()
                .map(doc -> doc.getString("_id"))
                .toList();
    }

    /**
     * 根据会话 id 查询全部历史消息
     *
     * @param conversationId 会话标识（格式为 userId_sessionId）
     * @return 反序列化后的消息列表；会话不存在时返回空列表
     */
    @Override
    public List<Message> findByConversationId(String conversationId) {
        Query query = Query.query(Criteria.where("_id").is(conversationId));
        Document doc = mongoTemplate.findOne(query, Document.class, COLLECTION);
        if (doc == null) {
            return Collections.emptyList();
        }
        List<String> jsonList = doc.getList("messages", String.class);
        return Optional.ofNullable(jsonList).orElse(Collections.emptyList()).stream()
                .map(MessageUtil::toMessage)
                .toList();
    }

    /**
     * 全量覆盖保存某会话的消息列表
     *
     * @param conversationId 会话标识（格式为 userId_sessionId）
     * @param messages       该会话的完整消息列表（非增量，直接覆盖写入）
     */
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        // 注意：messages 是全量列表，直接覆盖写入
        List<String> jsonList = messages.stream()
                .map(MessageUtil::toJson)
                .toList();
        Query query = Query.query(Criteria.where("_id").is(conversationId));
        Update update = new Update()
                .set("messages", jsonList)
                .set("updateTime", LocalDateTime.now());
        mongoTemplate.upsert(query, update, COLLECTION);
    }

    /**
     * 根据会话 id 删除整个会话的记忆文档
     *
     * @param conversationId 会话标识（格式为 userId_sessionId）
     */
    @Override
    public void deleteByConversationId(String conversationId) {
        Query query = Query.query(Criteria.where("_id").is(conversationId));
        mongoTemplate.remove(query, COLLECTION);
    }
}
