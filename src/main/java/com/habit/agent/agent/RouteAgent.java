package com.habit.agent.agent;

import com.habit.agent.config.SystemPromptConfig;
import com.habit.agent.service.ChatSessionService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

/**
 * 路由智能体（Route Agent）。
 *
 * <p>参照示例 {@code com.tianji.aigc.agent.RouteAgent} 设计，适配本项目：
 * 继承 {@link AbstractAgent}，仅覆写 {@link #getAgentType()} 与 {@link #systemMessage()}。
 * 其系统提示词由 {@link SystemPromptConfig#getRouteAgentSystemMessage()} 提供，
 * 负责将用户意图归类为 {@link AgentTypeEnum} 中的某一类（SLEEP / DIET / EXERCISE /
 * CHECKIN / KNOWLEDGE / HEALTH），供上层路由分发使用。</p>
 *
 * <p>注：父类 {@link AbstractAgent} 构造签名固定为 5 个依赖，
 * 此处使用显式构造器全量接收并按父类顺序透传。</p>
 */
@Component
public class RouteAgent extends AbstractAgent {

    private final SystemPromptConfig systemPromptConfig;

    /**
     * 构造器：将全部依赖透传给 {@link AbstractAgent} 父类，并持有路由提示词配置。
     */
    public RouteAgent(ChatSessionService chatSessionService,
                      ChatClient chatClient,
                      ChatMemory chatMemory,
                      SystemPromptConfig systemPromptConfig,
                      VectorStore vectorStore) {
        super(chatSessionService, chatClient, chatMemory, systemPromptConfig, vectorStore);
        this.systemPromptConfig = systemPromptConfig;
    }

    @Override
    public String systemMessage() {
        return this.systemPromptConfig.getRouteAgentSystemMessage().get();
    }

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.ROUTE;
    }
}
