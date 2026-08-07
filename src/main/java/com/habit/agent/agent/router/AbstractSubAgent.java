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
 *   <li>同步 {@link #handle}：沿用原 try-catch 返回兜底文案，内部已吞掉异常不会抛出；</li>
 *   <li>流式 {@link #handleStream}：异常在订阅后异步抛出，无法用 try-catch 包裹，
 *       改用 {@code onErrorResume} 降级为<b>同步完整调用 {@link #handle}</b>。
 *       原因：① 同步 {@code call()} 路径不经过有缺陷的 DashScope 流式聚合器
 *       （{@code OpenAiChatModel$ChunkMerger} 的无保护 {@code Optional.get()}），可拿到
 *       完整工具调用结果而非静默断流只给兜底文案；② {@link #handle} 与流式路径都走
 *       同一套 {@code IdempotentChatMemoryAdvisor}，user/assistant 写入均幂等，
 *       降级重入不会造成记忆重复。</li>
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
                // 降级：流式路径触发 DashScope 流式聚合器已知缺陷（NoSuchElementException）时，
                // 改用同步完整调用 handle() —— 同步 call() 不经 ChunkMerger 可正常聚合工具调用；
                // 且 IdempotentChatMemoryAdvisor 保证 user/assistant 幂等写入，不会重复。
                // handle() 内部已 try-catch，异常时返回兜底文案，此处不会抛出。
                log.warn("[{}] 流式调用失败（{}），降级为同步完整调用", roleName(), e.toString());
                String content = handle(message, conversationId);
                ChatResponse resp = new ChatResponse(
                        List.of(new Generation(new AssistantMessage(content))));
                return Flux.just(ChatClientResponse.builder()
                        .chatResponse(resp)
                        .build());
            });
    }
}
