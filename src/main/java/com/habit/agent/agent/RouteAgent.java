package com.habit.agent.agent;

import com.habit.agent.aigc.config.SystemPromptConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 意图路由智能体（ROUTE）
 * <p>
 * 使用独立 ChatClient（不挂记忆 Advisor、不挂工具）调用 LLM 做意图识别，
 * 输出 AgentTypeEnum.agentName 分类名（SLEEP/DIET/EXERCISE/CHECKIN/KNOWLEDGE/HEALTH），
 * 供 AgentRouter 分发到对应子智能体。
 */
@Slf4j
@Component
public class RouteAgent extends AbstractAgent {

    /** 独立路由识别 ChatClient（干净实例，避免污染记忆与工具） */
    private final ChatClient routeChatClient;

    public RouteAgent(ChatClient chatClient, ChatMemory chatMemory,
                      SystemPromptConfig systemPromptConfig, ChatClient.Builder chatClientBuilder) {
        super(chatClient, chatMemory, systemPromptConfig);
        this.routeChatClient = chatClientBuilder.build();
    }

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.ROUTE;
    }

    @Override
    public String systemMessage() {
        return systemPromptConfig.getRouteAgentSystemMessage().get();
    }

    /**
     * 意图识别：返回最匹配的 AgentTypeEnum
     */
    public AgentTypeEnum route(String question) {
        String answer = routeChatClient.prompt()
                .system(promptSpec -> promptSpec
                        .text(systemMessage())
                        .params(Map.of("question", question)))
                .user("请判断分类")
                .call()
                .content();
        log.info("路由识别：question={} -> result={}", question, answer);
        return AgentTypeEnum.of(answer);
    }
}
