package com.habit.agent.agent;

import com.habit.agent.aigc.config.SystemPromptConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

/**
 * 饮食顾问子智能体（DIET）
 */
@Component
public class DietAgent extends AbstractAgent {

    /**
     * 构造饮食顾问子智能体
     *
     * @param chatClient          共享的 ChatClient 实例
     * @param chatMemory          会话记忆组件
     * @param systemPromptConfig 系统提示词配置来源
     */
    public DietAgent(ChatClient chatClient, ChatMemory chatMemory, SystemPromptConfig systemPromptConfig) {
        super(chatClient, chatMemory, systemPromptConfig);
    }

    /**
     * 获取智能体类型
     *
     * @return {@link AgentTypeEnum#DIET}
     */
    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.DIET;
    }

    /**
     * 获取系统提示词文本
     *
     * @return 饮食顾问专用系统提示词
     */
    @Override
    public String systemMessage() {
        return systemPromptConfig.getDietAgentSystemMessage().get();
    }
}
