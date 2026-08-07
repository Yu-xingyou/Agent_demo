package com.habit.agent.agent.router;

import com.habit.agent.common.constant.AgentConstants;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import reactor.core.publisher.Flux;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * 子 Agent 抽象基类（阶段五 5-2 / 阶段九）。
 *
 * <p>消除三个子 Agent 的重复代码（prompt 构建、conversationId 注入、call/stream 双模板与降级）。
 * 子类只需提供 {@link #systemPrompt()} 与 {@link #roleName()}，其余由本基类统一处理。
 *
 * <p>降级语义：
 * <ul>
 *   <li>同步 {@link #handle}：沿用原 try-catch 返回兜底文案；</li>
 *   <li>流式 {@link #handleStream}：异常在订阅后异步抛出，无法用 try-catch 包裹，
 *       改用 {@code onErrorResume} 返回一条包含兜底文案的 {@link ChatClientResponse}，
 *       保证前端始终能收到可读内容而非静默断流。</li>
 * </ul>
 */
@Slf4j
public abstract class AbstractSubAgent implements SubAgent {

    protected final ChatClient chatClient;

    protected AbstractSubAgent(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /** 子 Agent 的 system 提示词（角色边界）。 */
    protected abstract String systemPrompt();

    /** 调用失败时的兜底文案。 */
    protected abstract String fallbackMessage();

    @Override
    public String handle(String message, String conversationId) {
        try {
            return chatClient.prompt()
                    .system(systemPrompt())
                    .user(message)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID,
                            conversationId == null ? AgentConstants.DEFAULT_USERNAME : conversationId))
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("[{}] 调用失败，降级为提示", roleName(), e);
            return fallbackMessage();
        }
    }

    @Override
    public Flux<ChatClientResponse> handleStream(String message, String conversationId) {
        return chatClient.prompt()
                .system(systemPrompt())
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID,
                        conversationId == null ? AgentConstants.DEFAULT_USERNAME : conversationId))
                .stream()
                .chatClientResponse()
                .onErrorResume(e -> {
                    // 降级修复：不再调用同步 handle()（那会再次触发完整 advisor 链，把同一轮用户消息
                    // 重复写入记忆）。直接返回兜底文案的 ChatClientResponse —— 该响应会流经
                    // 聚合器并触发一次 after()，正常写入本轮助手回复，记忆保持完整。
                    log.warn("[{}] 流式调用失败（{}），直接返回兜底文案", roleName(), e.toString());
                    ChatResponse resp = new ChatResponse(
                            List.of(new Generation(new AssistantMessage(fallbackMessage()))));
                    return Flux.just(ChatClientResponse.builder()
                            .chatResponse(resp)
                            .build());
                });
    }
}
