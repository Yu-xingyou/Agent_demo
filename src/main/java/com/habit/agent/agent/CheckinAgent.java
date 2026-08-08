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

    /**
     * 构造打卡助手子智能体
     *
     * @param chatClient          共享的 ChatClient 实例
     * @param chatMemory          会话记忆组件
     * @param systemPromptConfig 系统提示词配置来源
     */
    public CheckinAgent(ChatClient chatClient, ChatMemory chatMemory, SystemPromptConfig systemPromptConfig) {
        super(chatClient, chatMemory, systemPromptConfig);
    }

    /**
     * 获取智能体类型
     *
     * @return {@link AgentTypeEnum#CHECKIN}
     */
    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.CHECKIN;
    }

    /**
     * 获取系统提示词文本
     *
     * @return 打卡助手专用系统提示词
     */
    @Override
    public String systemMessage() {
        return systemPromptConfig.getCheckinAgentSystemMessage().get();
    }
}
