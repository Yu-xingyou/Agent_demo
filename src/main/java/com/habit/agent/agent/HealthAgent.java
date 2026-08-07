package com.habit.agent.agent;

import com.habit.agent.aigc.config.SystemPromptConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

/**
 * 综合健康助手主对话智能体（HEALTH）
 * <p>
 * 当路由智能体无法匹配特定维度时兜底使用，处理综合咨询与闲聊。
 */
@Component
public class HealthAgent extends AbstractAgent {

    public HealthAgent(ChatClient chatClient, ChatMemory chatMemory, SystemPromptConfig systemPromptConfig) {
        super(chatClient, chatMemory, systemPromptConfig);
    }

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.HEALTH;
    }

    @Override
    public String systemMessage() {
        return systemPromptConfig.getChatSystemMessage().get();
    }
}
