package com.habit.agent.aigc.config;

import com.habit.agent.aigc.memory.MongoChatMemoryRepository;
import com.habit.agent.aigc.tools.HabitTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

/**
 * Spring AI 装配配置（参照天机学堂 tj-aigc SpringAIConfig）
 * <p>
 * ChatClient 装配：SimpleLoggerAdvisor（日志）+ MessageChatMemoryAdvisor（MongoDB 记忆）+ RAG Advisor（远程 Atlas 向量库，可用时）+ defaultTools(HabitTools)
 */
@Configuration
public class SpringAIConfig {

    @Value("${habit.ai.memory.max:100}")
    private Integer maxMessages;

    /**
     * 配置 ChatClient
     * 收集容器内全部 Advisor（logger/memory/rag，rag 在向量库不可用时为 null 自动跳过）
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder,
                                 ObjectProvider<Advisor> advisorProvider,
                                 HabitTools habitTools) {
        List<Advisor> advisors = advisorProvider.orderedStream()
                .filter(java.util.Objects::nonNull)
                .toList();
        return chatClientBuilder
                .defaultAdvisors(advisors.toArray(new Advisor[0]))
                .defaultTools(habitTools)
                .build();
    }

    /**
     * 日志记录器
     */
    @Bean
    public Advisor loggerAdvisor() {
        return new SimpleLoggerAdvisor();
    }

    /**
     * MongoDB 聊天记忆仓储（本地 MongoDB chatMemory 集合）
     */
    @Bean
    public ChatMemoryRepository chatMemoryRepository(MongoTemplate mongoTemplate) {
        return new MongoChatMemoryRepository(mongoTemplate);
    }

    /**
     * 记忆窗口
     */
    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(maxMessages)
                .build();
    }

    /**
     * 记忆增强 Advisor
     */
    @Bean
    public Advisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    /**
     * RAG 检索增强 Advisor（远程 Atlas 向量库可用时装配，否则返回 null 由 ChatClient 忽略）
     */
    @Bean
    public Advisor ragAdvisor(ObjectProvider<VectorStore> vectorStoreProvider) {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            return null;
        }
        VectorStoreDocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.6)
                .topK(6)
                .build();
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(retriever)
                .build();
    }
}
