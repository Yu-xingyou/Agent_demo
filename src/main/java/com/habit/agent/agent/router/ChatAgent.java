package com.habit.agent.agent.router;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * 阶段九子智能体：闲聊智能体。
 *
 * <p>兜底路由目标：处理与习惯管理无关的日常对话、情感陪伴类请求。同样走真流式输出。
 *
 * @see AbstractSubAgent 基类封装了 call/stream 模板与降级逻辑。
 */
@Component
public class ChatAgent extends AbstractSubAgent {

    public ChatAgent(ChatClient chatClient) {
        super(chatClient);
    }

    @Override
    public IntentRouter.Intent intent() {
        return IntentRouter.Intent.CHAT;
    }

    @Override
    public String roleName() {
        return "闲聊智能体";
    }

    @Override
    protected String systemPrompt() {
        return """
                你是一个温暖、善解人意的「闲聊智能体」，负责处理与习惯管理无关的日常对话、
                情感陪伴、闲聊。请自然、友好地回应，像朋友一样交流。
                如果用户的话题转向了习惯养成或数据分析，可以温和地引导回产品主题，
                但不要在闲聊中强行推销。使用中文回应。
                """;
    }

    @Override
    protected String fallbackMessage() {
        return "抱歉，我暂时有点忙不过来，请稍后再聊～";
    }
}
