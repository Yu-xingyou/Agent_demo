package com.habit.agent.agent.router;

import com.habit.agent.common.constant.AgentConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * 阶段九（多智能体路由）数据分析子 Agent。
 *
 * <p>职责：针对趋势、报告、达标率、雷达等数据类诉求，结合可用工具与记忆给出
 * 客观、数据驱动的总结。system 提示词固化其「只做分析、不下指令性建议」的边界，
 * 改善建议由 {@code SuggestionAgent} 负责，避免职责混淆。
 */
@Slf4j
@Component
public class DataAnalysisAgent implements SubAgent {

    private final ChatClient chatClient;

    public DataAnalysisAgent(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public String handle(String message, String conversationId) {
        String system = """
                你是「数据分析师」子智能体，隶属于生活习惯助手的多智能体系统。
                你的职责：仅基于用户的真实打卡数据，输出客观的趋势总结、达标率解读与数据异常提示。
                规则：
                1. 优先调用数据查询工具（趋势/概览/达成率/雷达）获取真实数据，不要编造数字；
                2. 只做「分析」，不下「指令性改善建议」（那由改善建议子智能体负责）；
                3. 结论需简洁、点列式，必要时用百分比与对比说明；
                4. 若数据不足，如实说明并提示用户先完成打卡。
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
            log.warn("[DataAnalysisAgent] 调用失败，降级为提示", e);
            return "抱歉，数据分析暂时不可用，请稍后再试或先到「每日打卡」页完成记录。";
        }
    }

    @Override
    public IntentRouter.Intent intent() {
        return IntentRouter.Intent.DATA_ANALYSIS;
    }

    @Override
    public String roleName() {
        return "DataAnalysisAgent";
    }
}
