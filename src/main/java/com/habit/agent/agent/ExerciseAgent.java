package com.habit.agent.agent;

import com.habit.agent.aigc.config.SystemPromptConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

/**
 * 运动顾问子智能体（EXERCISE）
 */
@Component
public class ExerciseAgent extends AbstractAgent {

    public ExerciseAgent(ChatClient chatClient, ChatMemory chatMemory, SystemPromptConfig systemPromptConfig) {
        super(chatClient, chatMemory, systemPromptConfig);
    }

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.EXERCISE;
    }

    @Override
    public String systemMessage() {
        return systemPromptConfig.getExerciseAgentSystemMessage().get();
    }
}
