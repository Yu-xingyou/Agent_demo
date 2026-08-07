package com.habit.agent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;

import com.habit.agent.advisor.RecordOptimizationAdvisor;
import com.habit.agent.memory.MyChatMemoryRepository;
import com.habit.agent.memory.ToolEnrichingChatMemory;
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
                                 Advisor recordOptimizationAdvisor,
                                 Advisor messageChatMemoryAdvisor,
                                 HabitTools habitTools,
                                 GoalTools goalTools,
                                 ReminderTools reminderTools,
                                 AnalysisTools analysisTools,
                                 KnowledgeTools knowledgeTools,
                                 SessionTools sessionTools,
                                 CommonTools commonTools) {
        return chatClientBuilder
                // 注意 Advisor 顺序：recordOptimizationAdvisor 的 order 比 messageChatMemoryAdvisor 更小，
                // 因此它在链中先 before、后 after —— 即「大模型响应先经记忆 Advisor 落库，
                // 再由其清理路由智能体写入的内部转发记录」。详见 RecordOptimizationAdvisor 类注释。
                .defaultAdvisors(loggerAdvisor, recordOptimizationAdvisor, messageChatMemoryAdvisor)
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
     *
     * <p>用 {@link ToolEnrichingChatMemory} 装饰，使工具（Tool）调用结果随助手消息
     * 一起持久化，修复"工具查出的数据不入库/历史查询看不到"的问题。</p>
     */
    @Bean
    public ChatMemory chatMemory(MongoChatMemoryRepository mongoChatMemoryRepository) {
        ChatMemory window = MessageWindowChatMemory.builder()
                .chatMemoryRepository(mongoChatMemoryRepository)
                .maxMessages(maxMessages) // 最多保留 maxMessages 条，超出则淘汰最旧轮次
                .build();
        return new ToolEnrichingChatMemory(window);
    }

    /**
     * 基于 MongoDB 会话记忆的 Advisor，实现多轮对话上下文注入。
     */
    @Bean
    public Advisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    /**
     * 记录优化 Advisor：清理路由智能体内部转发记录。
     *
     * <p>当大模型输出恰好是某个智能体名称（如 {@code SLEEP}/{@code DIET}/{@code ROUTE}）
     * 时，说明这是路由分类的内部结果、不应展示给用户，故移除记忆仓库中最近写入的两条消息。
     * order 刻意小于 {@link #messageChatMemoryAdvisor(ChatMemory)}，确保其在记忆落库之后执行清理。</p>
     */
    @Bean
    public Advisor recordOptimizationAdvisor(MyChatMemoryRepository myChatMemoryRepository) {
        return new RecordOptimizationAdvisor(myChatMemoryRepository);
    }
}
