package com.habit.agent.agent.router;

import com.habit.agent.common.constant.AgentConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * 阶段九（多智能体路由）通用闲聊子 Agent。
 *
 * <p>职责：承接问候、情感陪伴、通用问答等无需专项分析的诉求，提供有温度的陪伴式对话。
 * system 提示词固化其「闲聊为主、适度引导至打卡/分析」的边界。
 */
@Slf4j
@Component
public class ChatAgent implements SubAgent {

    private final ChatClient chatClient;

    public ChatAgent(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public String handle(String message, String conversationId) {
        String system = """
                你是「陪伴助手」子智能体，隶属于生活习惯助手的多智能体系统。
                你的职责：承接用户的问候、情感表达与通用闲聊，提供有温度的陪伴式对话。
                规则：
                1. 语气亲切、自然，像朋友一样交流；
                2. 在合适时机温和引导用户完成每日打卡或查看分析报告；
                3. 不编造数据、不越界做专业医疗/诊断结论；
                4. 涉及具体改善方案或数据解读时，提示用户可切换到对应专项智能体。
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
            log.warn("[ChatAgent] 调用失败，降级为提示", e);
            return "我在的，有什么想聊的或者需要帮忙记录的吗？";
        }
    }

    @Override
    public IntentRouter.Intent intent() {
        return IntentRouter.Intent.CHAT;
    }

    @Override
    public String roleName() {
        return "ChatAgent";
    }
}
