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

    /**
     * 构造综合健康助手主对话智能体
     *
     * @param chatClient          共享的 ChatClient 实例
     * @param chatMemory          会话记忆组件
     * @param systemPromptConfig 系统提示词配置来源
     */
    public HealthAgent(ChatClient chatClient, ChatMemory chatMemory, SystemPromptConfig systemPromptConfig) {
        super(chatClient, chatMemory, systemPromptConfig);
    }

    /**
     * 获取智能体类型
     *
     * @return {@link AgentTypeEnum#HEALTH}
     */
    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.HEALTH;
    }

    /**
     * 获取系统提示词文本
     *
     * @return 综合健康助手系统提示词
     */
    @Override
    public String systemMessage() {
        return systemPromptConfig.getChatSystemMessage().get();
    }
}
