package com.habit.agent.memory;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.ai.chat.memory.repository.mongo.MongoChatMemoryRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 基于 MongoDB 的 {@link MyChatMemoryRepository} 实现。
 *
 * <p>底层复用 Spring AI 官方 {@link MongoChatMemoryRepository} 的同一集合
 * （默认 {@code ai_chat_memory}，文档字段 {@code conversation_id} / {@code messages}），
 * 通过原生 Mongo Driver 直接执行聚合更新管道，实现「删除最近 2 条消息」的优化逻辑。</p>
 *
 * <p>由于官方仓库未暴露按位置删除消息的 API，这里用 {@code updateMany} + 聚合管道
 * （{@code $set} + {@code $slice}）将 {@code messages} 数组重写为「去掉末尾 2 个元素」的切片
 * （不足 2 条时清空），从而移除路由智能体写入的内部转发记录。</p>
 */
@Repository
public class MongoMyChatMemoryRepository implements MyChatMemoryRepository {

    /** 与官方 MongoChatMemoryRepository 保持一致的集合名 */
    private static final String COLLECTION_NAME = "ai_chat_memory";
    /** 文档会话标识字段（Spring AI MongoChatMemoryRepository 约定） */
    private static final String FIELD_CONVERSATION_ID = "conversation_id";
    /** 文档消息数组字段 */
    private static final String FIELD_MESSAGES = "messages";
    /** 单次优化移除的消息条数（用户提问 + 路由分类结果） */
    private static final int REMOVE_COUNT = 2;

    private final MongoTemplate mongoTemplate;

    public MongoMyChatMemoryRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void optimization(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        MongoCollection<Document> collection = mongoTemplate.getCollection(COLLECTION_NAME);

        Bson filter = Filters.eq(FIELD_CONVERSATION_ID, conversationId);

        // messages = messages[0 .. size-REMOVE_COUNT]（不足 REMOVE_COUNT 条时 $slice 自动取空）
        Document slice = new Document("$slice",
                List.of("$" + FIELD_MESSAGES,
                        new Document("$subtract",
                                List.of(new Document("$size", "$" + FIELD_MESSAGES), REMOVE_COUNT))));

        collection.updateMany(filter, List.of(new Document("$set",
                new Document(FIELD_MESSAGES, slice))));
    }
}
