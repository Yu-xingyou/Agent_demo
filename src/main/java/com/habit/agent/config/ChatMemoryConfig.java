package com.habit.agent.config;

import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.mongo.MongoChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import lombok.extern.slf4j.Slf4j;

/**
 * 阶段五（对话记忆）：基于 MongoDB 的对话记忆配置。
 *
 * <p>使用 Spring AI 2.0 的 {@link MongoChatMemoryRepository} 持久化消息到 MongoDB
 * 集合 {@code ai_chat_memory}（默认集合名，不可配置），配合 {@link MessageWindowChatMemory}
 * （默认保留最近 20 条消息窗口，始终保留 SystemMessage）实现多轮对话上下文保持。
 *
 * <p>暴露 {@link MessageChatMemoryAdvisor} Bean 供 ChatClient 注入，
 * 调用方通过 {@code advisorParams(ChatMemory.CONVERSATION_ID, ...)} 按会话隔离记忆。
 */
@Slf4j
@Configuration
public class ChatMemoryConfig {

    /** 记忆窗口大小：保留最近 N 条消息。 */
    private static final int MEMORY_WINDOW_SIZE = 20;

    @Bean
    public MongoChatMemoryRepository mongoChatMemoryRepository(MongoTemplate mongoTemplate) {
        return MongoChatMemoryRepository.builder()
                .mongoTemplate(mongoTemplate)
                .build();
    }

    /**
     * 自定义对话记忆（MongoDB 持久化 + 20 条消息窗口）。
     *
     * <p>方法名刻意不叫 {@code chatMemory}：Spring AI 的 {@code ChatMemoryAutoConfiguration}
     * 会注册同名 {@code chatMemory} Bean，虽带 {@code @ConditionalOnMissingBean}，
     * 但该条件按<b>类型</b>匹配、注册时仍按<b>名称</b>校验，当自动配置先于本类加载时
     * 会触发 "bean already defined & overriding is disabled" 启动失败。
     * 改名后自动配置能正确按类型退让，避免名称冲突。
     */
    @Bean
    public ChatMemory mongoWindowChatMemory(MongoChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(MEMORY_WINDOW_SIZE)
                .build();
    }

    @Bean
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }
}
