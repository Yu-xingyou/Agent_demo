package com.habit.agent.agent;

import com.habit.agent.aigc.config.SystemPromptConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

/**
 * 睡眠顾问子智能体（SLEEP）
 */
@Component
public class SleepAgent extends AbstractAgent {

    public SleepAgent(ChatClient chatClient, ChatMemory chatMemory, SystemPromptConfig systemPromptConfig) {
        super(chatClient, chatMemory, systemPromptConfig);
    }

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.SLEEP;
    }

    @Override
    public String systemMessage() {
        return systemPromptConfig.getSleepAgentSystemMessage().get();
    }
}
