package com.habit.agent.agent;

import com.habit.agent.aigc.config.SystemPromptConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

/**
 * 健康知识顾问子智能体（KNOWLEDGE，RAG 科普）
 */
@Component
public class KnowledgeAgent extends AbstractAgent {

    public KnowledgeAgent(ChatClient chatClient, ChatMemory chatMemory, SystemPromptConfig systemPromptConfig) {
        super(chatClient, chatMemory, systemPromptConfig);
    }

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.KNOWLEDGE;
    }

    @Override
    public String systemMessage() {
        return systemPromptConfig.getKnowledgeAgentSystemMessage().get();
    }
}
