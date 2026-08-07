package com.habit.agent.service.impl;

import cn.hutool.core.date.DateUtil;
import com.habit.agent.agent.AbstractAgent;
import com.habit.agent.agent.AgentTypeEnum;
import com.habit.agent.config.SystemPromptConfig;
import com.habit.agent.service.ChatService;
import com.habit.agent.service.ChatSessionService;
import com.habit.agent.vo.ChatEventVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * 健康习惯助手 Agent 实现（具体 Agent）。
 *
 * <p>继承 {@link AbstractAgent} 复用流式/非流式对话、会话标题异步生成、生成状态控制、
 * 中断补写记忆等公共能力，仅覆写本 Agent 特有的部分：
 * <ul>
 *   <li>{@link #getAgentType()} = {@code HEALTH}（健康习惯助手）；</li>
 *   <li>{@link #systemMessage()} 使用 {@link SystemPromptConfig} 的提示词（携带 {@code now} 参数）；</li>
 *   <li>{@link #advisors()} 注入 RAG 检索增强（基于 MongoDB 向量库 habit_knowledge）。</li>
 * </ul>
 * </p>
 *
 * <p>同时实现 {@link ChatService}：{@code chat} 委托给 {@code processStream}，
 * {@code stop} 委托给父类 {@code stop}，保持与前端/路由层现有契约一致。</p>
 */
@Slf4j
@Service
public class ChatServiceImpl extends AbstractAgent implements ChatService {

    /**
     * 构造器：将所有依赖透传给 {@link AbstractAgent} 父类。
     */
    public ChatServiceImpl(ChatSessionService chatSessionService,
                           ChatClient chatClient,
                           ChatMemory chatMemory,
                           SystemPromptConfig systemPromptConfig,
                           VectorStore vectorStore) {
        super(chatSessionService, chatClient, chatMemory, systemPromptConfig, vectorStore);
    }

    @Override
    public Flux<ChatEventVO> chat(String message, String sessionId) {
        // 委托给抽象基类提供的流式处理
        return processStream(message, sessionId);
    }

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.HEALTH;
    }

    @Override
    public String systemMessage() {
        // 健康习惯助手系统提示词（{now} 由 systemMessageParams 注入）
        return systemPromptConfig.getChatSystemMessage().get();
    }

    @Override
    public Map<String, Object> systemMessageParams() {
        // 携带当前时间，供提示词模板中的 {now} 占位符使用
        return Map.of("now", DateUtil.now());
    }

    @Override
    public List<Advisor> advisors() {
        // 注入 RAG 检索增强（基于 MongoDB 向量库 habit_knowledge）
        return List.of(buildRagAdvisor());
    }
}
