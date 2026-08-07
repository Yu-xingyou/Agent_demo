package com.habit.agent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.mongo.MongoChatMemoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.habit.agent.tools.AnalysisTools;
import com.habit.agent.tools.CommonTools;
import com.habit.agent.tools.GoalTools;
import com.habit.agent.tools.HabitTools;
import com.habit.agent.tools.KnowledgeTools;
import com.habit.agent.tools.ReminderTools;
import com.habit.agent.tools.SessionTools;

/**
 * Spring AI 2.0 配置（流式对话 + 会话记忆能力）。
 *
 * <p>spring-ai-starter-model-openai 已自动配置 {@link ChatClient.Builder}，
 * 此处注册 Advisor 增强（日志 + 会话记忆）并组装默认 {@link ChatClient}。</p>
 *
 * <p>会话记忆参照示例思路：示例用 Redis（{@code RedisChatMemoryRepository}），
 * 本项目改用 Spring AI 2.0 官方提供的 MongoDB 实现 {@link MongoChatMemoryRepository}，
 * 由 {@link MessageWindowChatMemory} 控制滑动窗口，并由 {@link MessageChatMemoryAdvisor}
 * 接入 {@link ChatClient}，从而持久化多轮对话到 MongoDB（集合 {@code ai_chat_memory}）。</p>
 */
@Configuration
public class SpringAIConfig {

    /** 记忆窗口最大消息数（超过则淘汰最旧的轮次） */
    @Value("${habit.ai.memory.max:100}")
    private Integer maxMessages;

    /**
     * 配置默认 ChatClient（日志 Advisor + 会话记忆 Advisor）。
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder,
                                 Advisor loggerAdvisor,
                                 Advisor messageChatMemoryAdvisor,
                                 HabitTools habitTools,
                                 GoalTools goalTools,
                                 ReminderTools reminderTools,
                                 AnalysisTools analysisTools,
                                 KnowledgeTools knowledgeTools,
                                 SessionTools sessionTools,
                                 CommonTools commonTools) {
        return chatClientBuilder
                .defaultAdvisors(loggerAdvisor, messageChatMemoryAdvisor)
                .defaultTools(habitTools, goalTools, reminderTools,
                        analysisTools, knowledgeTools, sessionTools, commonTools)
                .build();
    }

    /**
     * 请求/响应日志 Advisor。
     */
    @Bean
    public Advisor loggerAdvisor() {
        return new SimpleLoggerAdvisor();
    }

    /**
     * MongoDB 聊天记忆仓库（替代示例的 Redis 实现）。
     */
    @Bean
    public MongoChatMemoryRepository mongoChatMemoryRepository(MongoTemplate mongoTemplate) {
        return MongoChatMemoryRepository.builder()
                .mongoTemplate(mongoTemplate)
                .build();
    }

    /**
     * 滑动窗口式会话记忆。
     */
    @Bean
    public ChatMemory chatMemory(MongoChatMemoryRepository mongoChatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(mongoChatMemoryRepository)
                .maxMessages(maxMessages) // 最多保留 maxMessages 条，超出则淘汰最旧轮次
                .build();
    }

    /**
     * 基于 MongoDB 会话记忆的 Advisor，实现多轮对话上下文注入。
     */
    @Bean
    public Advisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }
}
