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

    /**
     * 构造睡眠顾问子智能体
     *
     * @param chatClient          共享的 ChatClient 实例
     * @param chatMemory          会话记忆组件
     * @param systemPromptConfig 系统提示词配置来源
     */
    public SleepAgent(ChatClient chatClient, ChatMemory chatMemory, SystemPromptConfig systemPromptConfig) {
        super(chatClient, chatMemory, systemPromptConfig);
    }

    /**
     * 获取智能体类型
     *
     * @return {@link AgentTypeEnum#SLEEP}
     */
    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.SLEEP;
    }

    /**
     * 获取系统提示词文本
     *
     * @return 睡眠顾问专用系统提示词
     */
    @Override
    public String systemMessage() {
        return systemPromptConfig.getSleepAgentSystemMessage().get();
    }
}
