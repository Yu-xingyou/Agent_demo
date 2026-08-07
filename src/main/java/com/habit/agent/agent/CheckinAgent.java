package com.habit.agent.agent;

import com.habit.agent.aigc.config.SystemPromptConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

/**
 * 打卡助手子智能体（CHECKIN）
 */
@Component
public class CheckinAgent extends AbstractAgent {

    public CheckinAgent(ChatClient chatClient, ChatMemory chatMemory, SystemPromptConfig systemPromptConfig) {
        super(chatClient, chatMemory, systemPromptConfig);
    }

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.CHECKIN;
    }

    @Override
    public String systemMessage() {
        return systemPromptConfig.getCheckinAgentSystemMessage().get();
    }
}
