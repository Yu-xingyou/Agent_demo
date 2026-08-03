package com.habit.agent.service.impl;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.entity.mongo.ChatSession;
import com.habit.agent.repository.mongo.ChatSessionRepository;
import com.habit.agent.service.ChatService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

/**
 * 阶段五（对话记忆与流式输出）ChatService 实现
 */
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    private final ChatClient chatClient;
    private final ChatSessionRepository chatSessionRepository;

    @Override
    public String chat(String userMessage, String conversationId) {
        final String cid = ensureSession(userMessage, conversationId);
        return chatOnce(userMessage, cid);
    }

    @Override
    public Flux<String> stream(String userMessage, String conversationId) {
        final String cid = ensureSession(userMessage, conversationId);
        // 流式优先：纯文本轮次走 .stream().content() 真逐字输出。
        //
        // 背景：Spring AI 2.0.0 的 OpenAiChatModel.ChunkMerger 在流式合并"工具调用 chunk"时，
        // 对通义千问（OpenAI 兼容）返回格式不健壮——其流式 tool_calls 缺 index 字段，会在 Flux
        // 数据流内部（onComplete 阶段）抛 NoSuchElementException。该异常发生在 Reactor 流内部，
        // 外层 try/catch 无法捕获，且 .content() 与 .chatClientResponse() 共用同一底层 ChunkMerger，
        // 故只要本轮模型发起工具调用，走 .stream() 必崩（该缺陷需 Spring AI 2.0.1 才修复）。
        //
        // 策略：纯文本轮次正常真流式；一旦 ChunkMerger 崩溃（即本轮涉及工具调用），onErrorResume
        // 在订阅层面拦截，降级为非流式 .call() 一次性返回完整文本（不触发 ChunkMerger），保证接口
        // 不崩、工具能力不丢。工具调用轮次以整段形式返回（非逐字）。
        return chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, cid))
                .options(OpenAiChatOptions.builder().parallelToolCalls(false))
                .stream()
                .content()
                .onErrorResume(t -> {
                    if (isChunkMergeFailure(t)) {
                        log.warn("[流式降级非流式] conversationId={}，本轮触发工具调用，降级为整段返回，原因：{}",
                                cid, t.getMessage());
                        return Flux.just(chatOnce(userMessage, cid));
                    }
                    return Flux.error(t);
                });
    }

    private String chatOnce(String userMessage, String conversationId) {
        // 兜底：调用处再次关闭并行工具调用，防止全局 defaultOptions 被局部覆盖而失效。
        return chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .options(OpenAiChatOptions.builder().parallelToolCalls(false))
                .call()
                .content();
    }

    /**
     * 判断异常是否来自 Spring AI 流式 chunk 合并器（ChunkMerger）对工具调用 chunk 的不兼容缺陷。
     * 沿 cause 链遍历，匹配 ChunkMerger 类名、NoSuchElementException，或
     * IllegalArgumentException 且消息含 "tool call"。
     */
    private boolean isChunkMergeFailure(Throwable t) {
        Throwable cause = t;
        while (cause != null) {
            String className = cause.getClass().getName();
            if (className.contains("OpenAiChatModel$ChunkMerger")
                    || cause instanceof java.util.NoSuchElementException) {
                return true;
            }
            if (cause instanceof IllegalArgumentException
                    && cause.getMessage() != null
                    && cause.getMessage().contains("tool call")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private String ensureSession(String userMessage, String conversationId) {
        if (conversationId != null && !conversationId.isBlank()) {
            return conversationId;
        }
        // 暂无会话时自动创建，并记录首条消息用于标题
        String cid = java.util.UUID.randomUUID().toString();
        ChatSession session = new ChatSession();
        session.setConversationId(cid);
        session.setUserId(AgentConstants.DEFAULT_USER_ID);
        session.setTitle(userMessage.length() > 20 ? userMessage.substring(0, 20) : userMessage);
        session.setStatus("ACTIVE");
        session.setExpireAt(LocalDateTime.now().plusDays(30));
        chatSessionRepository.save(session);
        return cid;
    }
}
