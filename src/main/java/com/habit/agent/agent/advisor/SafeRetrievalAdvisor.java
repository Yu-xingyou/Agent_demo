package com.habit.agent.agent.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import com.habit.agent.common.constant.AgentConstants;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * 阶段八：带降级保护的 RAG 检索增强 Advisor。
 *
 * <p>内部委托 Spring AI 官方 {@link RetrievalAugmentationAdvisor} 完成
 * 「向量检索 → 知识片段注入 Prompt」的标准流程，但官方实现<b>不做异常降级</b>：
 * Atlas 不可达、Embedding 调用超时或向量索引缺失都会直接抛出，
 * 导致 {@code /api/chat} 返回 500、{@code /api/chat/stream} 触发 error 事件。
 *
 * <p>本类在其外层包裹 try-catch，捕获任何异常后<b>放行原始请求</b>
 * （{@code chain.nextCall(request)}），即跳过知识注入但对话正常继续，
 * 从而保证知识库故障不影响助手基本可用性。
 *
 * <p>执行顺序 {@code HIGHEST_PRECEDENCE + 300}，位于记忆 Advisor（+200）之后。
 * 这是刻意设计：知识片段属于「本轮临时增强」，不应被写入长期对话记忆，
 * 否则 20 条的记忆窗口会被大段知识文本迅速挤满。
 */
@Slf4j
@Component
public class SafeRetrievalAdvisor implements CallAdvisor, StreamAdvisor {

    /** 官方 RAG Advisor，实际执行检索与增强。 */
    private final RetrievalAugmentationAdvisor delegate;

    public SafeRetrievalAdvisor(VectorStore vectorStore) {
        this.delegate = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .topK(AgentConstants.RAG_DEFAULT_TOP_K)
                        .similarityThreshold(AgentConstants.RAG_SIMILARITY_THRESHOLD)
                        .build())
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        // 允许空上下文：检索不到相关知识时仍按通用建议作答，而非拒答
                        .allowEmptyContext(true)
                        .build())
                .build();
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        try {
            return delegate.adviseCall(request, chain);
        } catch (Exception e) {
            log.warn("[Advisor:SafeRetrieval] 知识库检索失败，已降级为无检索对话：{}", e.getMessage());
            return chain.nextCall(request);
        }
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        try {
            // 同时覆盖两类失败：同步阶段直接抛出，以及响应式流中异步抛出
            return delegate.adviseStream(request, chain)
                    .onErrorResume(e -> {
                        log.warn("[Advisor:SafeRetrieval] 流式检索失败，已降级为无检索对话：{}", e.getMessage());
                        return chain.nextStream(request);
                    });
        } catch (Exception e) {
            log.warn("[Advisor:SafeRetrieval] 流式检索初始化失败，已降级为无检索对话：{}", e.getMessage());
            return chain.nextStream(request);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 300;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}
