package com.habit.agent.agent.router;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * 阶段九子智能体：数据分析智能体。
 *
 * <p>负责处理需要查询/分析习惯数据的请求，可借助工具（打卡查询、统计分析）获取真实数据。
 *
 * @see AbstractSubAgent 基类封装了 call/stream 模板与降级逻辑。
 */
@Component
public class DataAnalysisAgent extends AbstractSubAgent {

    public DataAnalysisAgent(ChatClient chatClient) {
        super(chatClient);
    }

    @Override
    public IntentRouter.Intent intent() {
        return IntentRouter.Intent.DATA_ANALYSIS;
    }

    @Override
    public String roleName() {
        return "数据分析智能体";
    }

    @Override
    protected String systemPrompt() {
        return """
                你是一个「数据分析智能体」，专门负责处理需要查询或分析用户习惯数据的请求。
                当用户的问题涉及以下任意情况时，请优先调用工具获取真实数据：
                - 查询某习惯的打卡记录、连续打卡天数；
                - 统计某时间段的完成率、趋势、环比；
                - 对比多个习惯的表现。
                你有可用的工具（打卡查询、统计分析），请主动使用，不要凭空编造数据。
                在给出分析结论时，用简洁、有条理的中文说明数据含义与可执行建议。
                """;
    }

    @Override
    protected String fallbackMessage() {
        return "抱歉，数据分析服务暂时不可用，请稍后再试。";
    }
}
