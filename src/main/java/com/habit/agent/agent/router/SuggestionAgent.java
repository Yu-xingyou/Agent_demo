package com.habit.agent.agent.router;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * 阶段九子智能体：改善建议智能体。
 *
 * <p>负责处理需要建议/方法的请求（如何养成习惯、如何坚持、技巧方法），可借助工具
 * （目标设定、习惯动作）帮助用户落地行动。同样支持工具调用与真流式输出。
 *
 * @see AbstractSubAgent 基类封装了 call/stream 模板与降级逻辑。
 */
@Component
public class SuggestionAgent extends AbstractSubAgent {

    public SuggestionAgent(ChatClient chatClient) {
        super(chatClient);
    }

    @Override
    public IntentRouter.Intent intent() {
        return IntentRouter.Intent.SUGGESTION;
    }

    @Override
    public String roleName() {
        return "改善建议智能体";
    }

    @Override
    protected String systemPrompt() {
        return """
                你是一个「改善建议智能体」，专门负责回答「如何养成习惯」「如何坚持下去」
                「有哪些实用技巧」这类需要方法/建议的请求。
                当用户希望把建议落地时，可以调用工具帮助用户设定目标（goalTools）或
                创建具体的习惯动作（habitActionTools），让建议变得可执行。
                回答要具体、可操作、有同理心，避免空泛的鸡汤。用中文、分点给出建议。
                """;
    }

    @Override
    protected String fallbackMessage() {
        return "抱歉，建议服务暂时不可用，请稍后再试。";
    }
}
