package com.habit.agent.agent;

import cn.hutool.core.util.StrUtil;
import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.config.SystemPromptConfig;
import com.habit.agent.enums.ChatEventTypeEnum;
import com.habit.agent.service.ChatService;
import com.habit.agent.service.ChatSessionService;
import com.habit.agent.vo.ChatEventVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 代理抽象基类，提供流式/非流式对话的公共实现。
 *
 * <p>参照示例 {@code com.tianji.aigc.agent.AbstractAgent} 设计，适配本项目：
 * <ul>
 *   <li>用户标识采用固定的 {@link AgentConstants#DEFAULT_USER_ID}（单用户演示，无 UserContext）；</li>
 *   <li>默认系统提示词由 {@link SystemPromptConfig} 提供（身份：健康习惯管理助手）；</li>
 *   <li>默认注入 RAG 检索增强（{@link RetrievalAugmentationAdvisor} + MongoDB 向量库）；</li>
 *   <li>会话标题在首条消息到达时异步生成（{@code chatSessionService.update}）；</li>
 *   <li>生成状态用 {@link #GENERATE_STATUS} 标记位控制（stop 即终止流），保持与现有 ChatServiceImpl 一致。</li>
 * </ul>
 * 具体 Agent 只需覆写 {@link #getAgentType()} 指定类型，并可选地覆写
 * {@link #systemMessage()}/{@link #advisors()}/{@link #tools()} 定制行为。</p>
 */
@Slf4j
public abstract class AbstractAgent implements Agent {

    /** 输出结束的标记事件 */
    public static final ChatEventVO STOP_EVENT =
            ChatEventVO.builder().eventType(ChatEventTypeEnum.STOP.getValue()).build();

    /**
     * 存储大模型的生成状态，采用 ConcurrentHashMap 确保线程安全。
     * 当前为单体实现；若需分布式环境，可替换为 Redis。
     */
    public static final Map<String, Boolean> GENERATE_STATUS = new ConcurrentHashMap<>();

    protected final ChatSessionService chatSessionService;
    protected final ChatClient chatClient;
    protected final ChatMemory chatMemory;
    protected final SystemPromptConfig systemPromptConfig;
    protected final VectorStore vectorStore;

    protected AbstractAgent(ChatSessionService chatSessionService,
                            ChatClient chatClient,
                            ChatMemory chatMemory,
                            SystemPromptConfig systemPromptConfig,
                            VectorStore vectorStore) {
        this.chatSessionService = chatSessionService;
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.systemPromptConfig = systemPromptConfig;
        this.vectorStore = vectorStore;
    }

    @Override
    public String process(String question, String sessionId) {
        var userId = AgentConstants.DEFAULT_USER_ID;

        // 更新会话标题（首条消息触发）
        this.chatSessionService.update(sessionId, question, userId);

        return getChatClientRequest(sessionId, question)
                .call()
                .content();
    }

    @Override
    public Flux<ChatEventVO> processStream(String question, String sessionId) {
        var userId = AgentConstants.DEFAULT_USER_ID;
        StringBuilder outputBuilder = new StringBuilder();
        String conversationId = ChatService.getConversationId(sessionId);
        // 请求内幂等标记：确保部分回答最多只落库一次（cancel 与截断可能同时触发）
        var saved = new java.util.concurrent.atomic.AtomicBoolean(false);

        // 更新会话标题（首条消息触发）
        this.chatSessionService.update(sessionId, question, userId);

        return getChatClientRequest(sessionId, question)
                .stream()
                .chatResponse()
                .doFirst(() -> GENERATE_STATUS.put(sessionId, true)) // 首次输出内容时执行
                .doOnError(throwable -> GENERATE_STATUS.remove(sessionId)) // 出现异常时，删除标识
                .doOnComplete(() -> GENERATE_STATUS.remove(sessionId)) // 完成时执行，删除标识
                .doOnCancel(() -> {
                    // 输出被取消（用户停止/前端断连）：补写已生成的部分回答
                    GENERATE_STATUS.remove(sessionId);
                    onPartialOutput(conversationId, outputBuilder.toString(), saved);
                })
                .takeWhile(response -> { // 标记被移除（stop）则终止流
                    boolean generating = GENERATE_STATUS.getOrDefault(sessionId, false);
                    if (!generating) {
                        // 被截断（用户主动停止）：补写部分回答
                        onPartialOutput(conversationId, outputBuilder.toString(), saved);
                    }
                    return generating;
                })
                .map(chatResponse -> {
                    String text = chatResponse.getResult().getOutput().getText();
                    outputBuilder.append(text); // 累积供中断落库使用
                    return ChatEventVO.builder()
                            .eventData(text)
                            .eventType(ChatEventTypeEnum.DATA.getValue())
                            .build();
                })
                .concatWith(Flux.defer(() -> {
                    // 允许具体 Agent 追加参数事件（如工具调用结果）；默认仅结束事件
                    ChatEventVO paramEvent = buildParamEvent();
                    if (paramEvent != null) {
                        return Flux.just(paramEvent, STOP_EVENT);
                    }
                    return Flux.just(STOP_EVENT);
                }));
    }

    /**
     * 构建统一的 ChatClient 请求（系统提示词 + advisor + tools + toolContext + user）。
     * 子类共享此方法，保证各 Agent 行为一致。
     */
    protected ChatClient.ChatClientRequestSpec getChatClientRequest(String sessionId, String question) {
        return this.chatClient.prompt()
                .system(promptSystem -> promptSystem
                        .text(this.systemMessage())
                        .params(this.systemMessageParams()))
                .advisors(advisor -> advisor
                        .advisors(this.advisors())
                        .params(this.advisorParams(sessionId)))
                .tools(this.tools())
                .toolContext(this.toolContext(sessionId))
                .user(question);
    }

    /**
     * 保存「停止/中断输出」时已生成的部分内容到会话记忆（供具体 Agent 覆写实现幂等补写）。
     *
     * <p>默认实现在内容非空时直接写入；本项目具体 Agent（健康助手）通过 {@code saved} CAS 标记
     * 保证最多落库一次，并区分「模型自然结束」与「用户主动停止」，避免与记忆 Advisor 重复落库。</p>
     *
     * @param conversationId 对话 id
     * @param content        大模型已输出的内容
     * @param saved          幂等标记，保证本次请求的补写只执行一次
     */
    protected void onPartialOutput(String conversationId, String content, java.util.concurrent.atomic.AtomicBoolean saved) {
        if (StrUtil.isBlank(content)) {
            return;
        }
        if (!saved.compareAndSet(false, true)) {
            return;
        }
        this.chatMemory.add(conversationId, new AssistantMessage(content));
        log.info("已保存中断输出的部分回答: conversationId={}, length={}", conversationId, content.length());
    }

    /**
     * 构建参数事件（如工具调用结果），默认返回 null（本期不返回 PARAM）。
     * 子类可按需覆写以返回 {@link ChatEventTypeEnum#PARAM} 事件。
     *
     * @return 参数事件，或 null
     */
    protected ChatEventVO buildParamEvent() {
        return null;
    }

    @Override
    public Map<String, Object> advisorParams(String sessionId) {
        var conversationId = ChatService.getConversationId(sessionId);
        return Map.of(ChatMemory.CONVERSATION_ID, conversationId);
    }

    @Override
    public void stop(String sessionId) {
        GENERATE_STATUS.remove(sessionId);
    }

    /**
     * 默认 RAG 检索增强 Advisor（基于 MongoDB 向量库 habit_knowledge）。
     * 子类可覆写 {@link #advisors()} 自定义。
     */
    protected RetrievalAugmentationAdvisor buildRagAdvisor() {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(this.vectorStore)
                        .similarityThreshold(0.6)
                        .topK(6)
                        .build())
                .build();
    }
}
