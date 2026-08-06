package com.habit.agent.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import com.habit.agent.agent.router.ChatAgent;
import com.habit.agent.agent.router.DataAnalysisAgent;
import com.habit.agent.agent.router.IntentRouter;
import com.habit.agent.agent.router.SubAgent;
import com.habit.agent.agent.router.SuggestionAgent;
import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.common.exception.ChatStreamException;
import com.habit.agent.common.stream.StopSignalRegistry;
import com.habit.agent.common.vo.ChatStreamEvent;
import com.habit.agent.entity.mongo.ChatSession;
import com.habit.agent.repository.mongo.ChatSessionRepository;
import com.habit.agent.service.ChatService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 阶段五（对话记忆与流式输出）+ 阶段九（多智能体路由）ChatService 实现。
 *
 * <p>阶段九：引入 {@link IntentRouter} 作为 Director 协调者，将用户消息按意图分发到三个子 Agent
 * （{@link DataAnalysisAgent} / {@link SuggestionAgent} / {@link ChatAgent}）。各子 Agent 持有
 * 独立的 system 提示词以固化角色边界，共享全局 {@code ChatClient} 与对话记忆。
 *
 * <p>真流式改造（阶段五 5-2，2026-08）：
 * 经核对 Spring AI 2.0.0 官方文档与源码，原先认为「工具调用轮次下真流式必然崩溃（ChunkMerger）」
 * 的结论<b>不成立</b>。真实根因是 {@code SafeRetrievalAdvisor} 的 order（+300）与框架自动注册的
 * {@code ToolCallingAdvisor}（+300）冲突，导致工具循环被包进 RAG 的 {@code onErrorResume} 中，
 * 任意瞬时异常都会静默重入下游链造成错乱。该 order 冲突已修复（RAG 前移至 +150），同时
 * 2.0.0 已移除 {@code streamToolCallResponses}：工具调用轮次「中间无文本分片、仅最终回复流式输出」
 * 属<b>官方预期行为</b>，并非故障。
 *
 * <p>因此本实现直接采用官方 Framework-Controlled 模式：路由到子 Agent 的
 * {@code handleStream()}（底层 {@code ChatClient.stream().chatClientResponse()}），由 {@code ToolCallingAdvisor}
 * 透明驱动工具循环，最终回复真流式逐字下发。服务层负责：
 * <ul>
 *   <li>路由意图通过 {@code meta} 事件告知前端；</li>
 *   <li>文本增量经 {@code chunk} 事件逐字下发；</li>
 *   <li>工具调用状态经 {@code tool_call} 事件下发（start 在首个工具消息出现时、end 在工具循环结束后）；</li>
 *   <li>结束事件 {@code done} 携带真实 Token 用量（需 application.yml 开启 {@code stream-usage}）、耗时与首字延迟；</li>
 *   <li>停止生成由 {@link StopSignalRegistry} 信号真正取消上游订阅；</li>
 *   <li>可重试异常在「首个 chunk 发出前」做有限次退避重试，已产出内容后则仅转 {@code error} 事件。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    private static final String MODEL = "qwen-plus";

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final ChatSessionRepository chatSessionRepository;
    private final DataAnalysisAgent dataAnalysisAgent;
    private final SuggestionAgent suggestionAgent;
    private final ChatAgent chatAgent;
    private final StopSignalRegistry stopSignalRegistry;
    private final IntentRouter intentRouter;

    @Override
    public String chat(String userMessage, String conversationId) {
        final String cid = ensureSession(userMessage, conversationId);
        return chatOnce(userMessage, cid);
    }

    @Override
    public Flux<ChatStreamEvent> stream(String userMessage, String conversationId) {
        final String cid = ensureSession(userMessage, conversationId);
        final long startNanos = System.nanoTime();
        final IntentRouter.Intent intent = intentRouter.route(userMessage);
        final SubAgent agent = resolveAgent(intent);

        // 是否已向客户端推送过内容：用于「首字前可重试 / 首字后仅转 error」的边界控制
        final AtomicBoolean hasEmittedContent = new AtomicBoolean(false);
        final AtomicLong firstTokenNanos = new AtomicLong(-1L);
        final AtomicInteger chunkIndex = new AtomicInteger(0);
        final AtomicReference<Usage> lastUsage = new AtomicReference<>(null);
        final AtomicBoolean toolPhaseActive = new AtomicBoolean(false);

        // 1) meta 事件：路由意图、会话元信息
        ChatStreamEvent meta = new ChatStreamEvent.Meta(cid,
                LocalDateTime.now().toString(), MODEL, intent.name());

        // 2) 注册停止信号（takeUntilOther 真正取消上游订阅）
        Flux<Void> stopSignal = stopSignalRegistry.register(cid);

        // 3) 子 Agent 真流式分片流
        Flux<ChatStreamEvent> contentFlow = agent.handleStream(userMessage, cid)
                .flatMap(response -> mapResponseToEvents(response, hasEmittedContent,
                        firstTokenNanos, chunkIndex, lastUsage, toolPhaseActive))
                .takeUntilOther(stopSignal);

        // 4) 重试：仅在首个 chunk 发出前对可重试异常退避重试（避免内容重复）
        Flux<ChatStreamEvent> withRetry = contentFlow
                .onErrorResume(e -> {
                    if (!hasEmittedContent.get() && ChatStreamException.isRetryable(e)) {
                        return contentFlow
                                .retryWhen(reactor.util.retry.Retry.backoff(2, Duration.ofSeconds(1))
                                        .filter(ChatStreamException::isRetryable))
                                .onErrorResume(ChatStreamException::isRetryable,
                                        retryErr -> Flux.just(errorEvent(cid, retryErr, startNanos)));
                    }
                    return Flux.just(errorEvent(cid, e, startNanos));
                });

        // 5) 超时控制（120s）：转为 AI_TIMEOUT 错误事件
        Flux<ChatStreamEvent> withTimeout = withRetry
                .timeout(Duration.ofSeconds(120))
                .onErrorResume(e -> e instanceof java.util.concurrent.TimeoutException
                        ? Flux.just(new ChatStreamEvent.Error("AI_TIMEOUT",
                                "对话生成超时，请稍后重试", cid, true))
                        : Flux.error(e));

        // 6) 收尾：在 onComplete 时下发 done 事件（含真实 Token），并清理停止信号
        return withTimeout
                .doOnComplete(() -> {
                    stopSignalRegistry.release(cid);
                    log.debug("[流式完成] conversationId={} 意图={} 分片数={} 首字延迟={}ms",
                            cid, intent, chunkIndex.get(),
                            firstTokenNanos.get() > 0
                                    ? (firstTokenNanos.get() - startNanos) / 1_000_000 : -1);
                })
                .doOnCancel(() -> stopSignalRegistry.release(cid))
                .doOnError(e -> stopSignalRegistry.release(cid))
                .concatWith(Mono.fromCallable(() -> buildDoneEvent(cid, lastUsage.get(),
                        startNanos, firstTokenNanos, chunkIndex)))
                .startWith(meta);
    }

    /** 将单个 ChatClientResponse 映射为 SSE 事件序列（chunk / tool_call）。 */
    private Flux<ChatStreamEvent> mapResponseToEvents(ChatClientResponse response,
            AtomicBoolean hasEmittedContent, AtomicLong firstTokenNanos,
            AtomicInteger chunkIndex, AtomicReference<Usage> lastUsage,
            AtomicBoolean toolPhaseActive) {
        // 暂存最后一次非空 Usage（流式下通常仅末分片携带）
        if (response.chatResponse() != null
                && response.chatResponse().getMetadata() != null
                && response.chatResponse().getMetadata().getUsage() != null) {
            lastUsage.set(response.chatResponse().getMetadata().getUsage());
        }
        // 文本增量
        String text = resolveText(response);
        if (text != null && !text.isEmpty()) {
            hasEmittedContent.set(true);
            if (firstTokenNanos.get() < 0) {
                firstTokenNanos.set(System.nanoTime());
            }
            if (toolPhaseActive.getAndSet(false)) {
                // 工具阶段刚结束，先补一条 tool_call end
                return Flux.just(
                        new ChatStreamEvent.ToolCall("end", "数据查询完成", null),
                        new ChatStreamEvent.Chunk(text, chunkIndex.getAndIncrement()));
            }
            return Flux.just(new ChatStreamEvent.Chunk(text, chunkIndex.getAndIncrement()));
        }
        // 工具调用消息（AssistantMessage 含 toolCalls 且无文本）→ 下发 tool_call start
        if (hasToolCalls(response) && !toolPhaseActive.getAndSet(true)) {
            String toolName = resolveToolName(response);
            return Flux.just(new ChatStreamEvent.ToolCall("start",
                    "正在查询数据…", toolName));
        }
        return Flux.empty();
    }

    private boolean hasToolCalls(ChatClientResponse response) {
        if (response.chatResponse() == null || response.chatResponse().getResult() == null) {
            return false;
        }
        var output = response.chatResponse().getResult().getOutput();
        return output != null && output.getToolCalls() != null && !output.getToolCalls().isEmpty();
    }

    private String resolveToolName(ChatClientResponse response) {
        var output = response.chatResponse().getResult().getOutput();
        if (output != null && output.getToolCalls() != null && !output.getToolCalls().isEmpty()) {
            return output.getToolCalls().get(0).name();
        }
        return null;
    }

    private String resolveText(ChatClientResponse response) {
        if (response.chatResponse() == null || response.chatResponse().getResult() == null
                || response.chatResponse().getResult().getOutput() == null) {
            return null;
        }
        String text = response.chatResponse().getResult().getOutput().getText();
        return (text == null || text.isEmpty()) ? null : text;
    }

    private ChatStreamEvent.Done buildDoneEvent(String cid, Usage usage,
            long startNanos, AtomicLong firstTokenNanos, AtomicInteger chunkIndex) {
        Integer prompt = null, completion = null, total = null;
        if (usage != null) {
            prompt = usage.getPromptTokens() > 0 ? (int) usage.getPromptTokens() : null;
            completion = usage.getCompletionTokens() > 0 ? (int) usage.getCompletionTokens() : null;
            total = usage.getTotalTokens() > 0 ? (int) usage.getTotalTokens() : null;
        }
        long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
        Long firstTokenLatency = firstTokenNanos.get() > 0
                ? (firstTokenNanos.get() - startNanos) / 1_000_000 : null;
        return new ChatStreamEvent.Done(cid, prompt, completion, total, durationMs,
                firstTokenLatency, "streaming");
    }

    private ChatStreamEvent.Error errorEvent(String cid, Throwable e, long startNanos) {
        ChatStreamException ex = (e instanceof ChatStreamException)
                ? (ChatStreamException) e : ChatStreamException.of(e);
        return new ChatStreamEvent.Error(ex.getErrorCode(), ex.getMessage(), cid, ex.isRetryable());
    }

    /** 阶段九：先路由意图，再交由对应子 Agent 处理（同步接口复用）。 */
    private String chatOnce(String userMessage, String conversationId) {
        IntentRouter.Intent intent = intentRouter.route(userMessage);
        SubAgent agent = resolveAgent(intent);
        log.debug("[多智能体路由] 意图={} 分发至={}", intent, agent.roleName());
        return agent.handle(userMessage, conversationId);
    }

    private SubAgent resolveAgent(IntentRouter.Intent intent) {
        return switch (intent) {
            case DATA_ANALYSIS -> dataAnalysisAgent;
            case SUGGESTION -> suggestionAgent;
            default -> chatAgent;
        };
    }

    @Override
    public List<Map<String, String>> getMessages(String conversationId) {
        List<org.springframework.ai.chat.messages.Message> msgs =
                chatMemory.get(conversationId);
        List<Map<String, String>> result = new ArrayList<>();
        for (org.springframework.ai.chat.messages.Message m : msgs) {
            // 跳过 SystemMessage，只返回用户和助手的消息
            if (m instanceof org.springframework.ai.chat.messages.SystemMessage) continue;
            Map<String, String> item = new HashMap<>();
            String role = "ai";
            if (m instanceof org.springframework.ai.chat.messages.UserMessage) {
                role = "user";
            } else if (m instanceof org.springframework.ai.chat.messages.AssistantMessage) {
                role = "ai";
            }
            item.put("role", role);
            item.put("text", m.getText());
            result.add(item);
        }
        return result;
    }

    @Override
    public String generateTitle(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return "新会话";
        }
        try {
            String title = chatClient.prompt()
                    .user("用不超过15个字总结这段对话的主题：" + userMessage)
                    .options(OpenAiChatOptions.builder().temperature(0.3))
                    .call()
                    .content();
            if (title == null || title.isBlank() || title.length() > 30) {
                return userMessage.length() > 15 ? userMessage.substring(0, 15) + "…" : userMessage;
            }
            return title.trim();
        } catch (Exception e) {
            log.warn("[标题生成失败] 降级为截断首条消息", e);
            return userMessage.length() > 15 ? userMessage.substring(0, 15) + "…" : userMessage;
        }
    }

    /**
     * 确保会话存在并返回最终 conversationId。
     * 注意：含 MongoDB 同步写，须在非 Reactor 线程或 boundedElastic 调度下调用。
     */
    private String ensureSession(String userMessage, String conversationId) {
        String cid = (conversationId == null || conversationId.isBlank())
                ? java.util.UUID.randomUUID().toString()
                : conversationId;
        // 会话已存在则直接复用（保留已有标题/时间）
        if (chatSessionRepository.existsByConversationId(cid)) {
            touchSession(cid);
            return cid;
        }
        // 首次进入该会话时持久化元数据，供历史会话侧边栏展示
        ChatSession session = new ChatSession();
        LocalDateTime now = LocalDateTime.now();
        session.setConversationId(cid);
        session.setUserId(AgentConstants.DEFAULT_USER_ID);
        session.setTitle(userMessage.length() > 20 ? userMessage.substring(0, 20) : userMessage);
        session.setStatus("ACTIVE");
        session.setMessageCount(1);
        session.setCreateTime(now);
        session.setLastMessageTime(now);
        session.setExpireAt(now.plusDays(30));
        chatSessionRepository.save(session);
        return cid;
    }

    /** 更新会话的最后消息时间与消息条数（同步写，由调用方在 boundedElastic 下执行）。 */
    private void touchSession(String conversationId) {
        chatSessionRepository.findByConversationId(conversationId).ifPresent(s -> {
            s.setLastMessageTime(LocalDateTime.now());
            s.setMessageCount(s.getMessageCount() + 1);
            chatSessionRepository.save(s);
        });
    }
}
