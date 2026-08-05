package com.habit.agent.agent.router;

import com.habit.agent.common.constant.AgentConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * 阶段九（多智能体路由）改善建议子 Agent。
 *
 * <p>职责：针对「怎么做 / 计划 / 方案」类诉求，结合用户画像与领域知识（RAG 知识库）给出
 * 可执行、循序渐进的改善建议。system 提示词固化其「给建议、不替用户做数据分析」的边界。
 */
@Slf4j
@Component
public class SuggestionAgent implements SubAgent {

    private final ChatClient chatClient;

    public SuggestionAgent(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public String handle(String message, String conversationId) {
        String system = """
                你是「习惯教练」子智能体，隶属于生活习惯助手的多智能体系统。
                你的职责：基于用户的诉求与生活方式，给出可执行、循序渐进的改善建议与行动清单。
                规则：
                1. 可结合知识库（健康/运动/睡眠/饮食常识）给出科学建议，不要编造权威来源；
                2. 只给「建议」，不重复做数据趋势分析（那由数据分析子智能体负责）；
                3. 建议需具体、分步骤、可量化（如「每天提前 15 分钟睡」而非「早点睡」）；
                4. 语气鼓励、不评判，尊重用户的现实约束。
                """;
        try {
            return chatClient.prompt()
                    .system(system)
                    .user(message)
                    .advisors(a -> a.param(
                            org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID,
                            conversationId == null ? AgentConstants.DEFAULT_USERNAME : conversationId))
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("[SuggestionAgent] 调用失败，降级为提示", e);
            return "抱歉，建议生成暂时不可用，请稍后再试。你可以先描述想改善的具体习惯。";
        }
    }

    @Override
    public IntentRouter.Intent intent() {
        return IntentRouter.Intent.SUGGESTION;
    }

    @Override
    public String roleName() {
        return "SuggestionAgent";
    }
}
