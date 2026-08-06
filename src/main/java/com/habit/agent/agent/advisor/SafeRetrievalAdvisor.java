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
 * <p>执行顺序 {@code HIGHEST_PRECEDENCE + 150}，位于安全/日志（+10/+20）之后、
 * 记忆 Advisor（+200）与框架自动注册的 {@code ToolCallingAdvisor}（+300）之前。
 *
 * <p><b>为何是 +150 而非 +300</b>：Spring AI 2.0.0 的 {@code ToolCallingAdvisor}
 * 默认 order 恰为 {@code HIGHEST_PRECEDENCE + 300}。若本 RAG Advisor 也取 +300，
 * 两者 order 相同、相对顺序未定义；一旦本 Advisor 排在工具 Advisor 之前，
 * 整个工具调用循环会被包进本类的 {@code onErrorResume} 中——工具循环内部任何异常
 * 都会被静默吞掉并以 {@code chain.nextStream(request)} 重入下游链，造成流式输出
 * 错乱/重复/提前终止（这正是此前被误判为「ChunkMerger 崩溃、真流式不可用」的表象）。
 * 前移到 +150 后，本 Advisor 仅包裹「检索增强」阶段，工具循环位于其后，
 * 异常兜底范围不再覆盖工具循环，从根本上消除重入风险。
 *
 * <p>知识片段属于「本轮临时增强」，不应被写入长期对话记忆（否则 20 条记忆窗口
 * 会被大段知识文本迅速挤满），故置于记忆 Advisor（+200）之前是刻意设计。
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
        // 仅捕获「检索增强阶段」自身的异常：降级为跳过知识注入、继续后续链（记忆→工具→模型）。
        // 注意：因本 Advisor 已前移至 +150（位于 ToolCallingAdvisor +300 之前），
        // 工具调用循环不在本 onErrorResume 的包裹范围内，故不会重入下游链造成重复输出。
        return delegate.adviseStream(request, chain)
                .onErrorResume(e -> {
                    log.warn("[Advisor:SafeRetrieval] 流式检索失败，已降级为无检索对话：{}", e.getMessage());
                    return chain.nextStream(request);
                });
    }

    @Override
    public int getOrder() {
        // 关键修复：由 +300 改为 +150，消除与 ToolCallingAdvisor（+300）的 order 冲突。
        return Ordered.HIGHEST_PRECEDENCE + 150;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}
