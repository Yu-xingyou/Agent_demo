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

    public DietAgent(ChatClient chatClient, ChatMemory chatMemory, SystemPromptConfig systemPromptConfig) {
        super(chatClient, chatMemory, systemPromptConfig);
    }

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.DIET;
    }

    @Override
    public String systemMessage() {
        return systemPromptConfig.getDietAgentSystemMessage().get();
    }
}
