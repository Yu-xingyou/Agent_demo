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

    /**
     * 构造健康知识顾问子智能体（RAG 科普）
     *
     * @param chatClient          共享的 ChatClient 实例
     * @param chatMemory          会话记忆组件
     * @param systemPromptConfig 系统提示词配置来源
     */
    public KnowledgeAgent(ChatClient chatClient, ChatMemory chatMemory, SystemPromptConfig systemPromptConfig) {
        super(chatClient, chatMemory, systemPromptConfig);
    }

    /**
     * 获取智能体类型
     *
     * @return {@link AgentTypeEnum#KNOWLEDGE}
     */
    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.KNOWLEDGE;
    }

    /**
     * 获取系统提示词文本
     *
     * @return 健康知识顾问专用系统提示词
     */
    @Override
    public String systemMessage() {
        return systemPromptConfig.getKnowledgeAgentSystemMessage().get();
    }
}
