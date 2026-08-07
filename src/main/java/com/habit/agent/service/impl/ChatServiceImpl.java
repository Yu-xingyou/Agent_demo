package com.habit.agent.service.impl;

import cn.hutool.core.date.DateUtil;
import com.habit.agent.config.SystemPromptConfig;
import com.habit.agent.enums.ChatEventTypeEnum;
import com.habit.agent.service.ChatService;
import com.habit.agent.vo.ChatEventVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 流式对话服务实现（Spring AI 2.0）。
 *
 * <p>参照示例思路，适配本项目：使用 PRD 3.2 的入参（message / sessionId），
 * 通过 {@link ChatClient} 流式拉取大模型输出，逐分片封装为 {@code 1001} 数据事件，
 * 结束时发送 {@code 1002} 停止事件。</p>
 *
 * <p>系统提示词由 {@link SystemPromptConfig} 注入（身份：健康习惯管理助手），
 * 并携带当前时间参数 {@code now}（提示词模板中用 {@code {now}} 占位）。</p>
 *
 * <p>多轮记忆由 {@code MessageChatMemoryAdvisor}（已注册到 {@link ChatClient}）自动注入，
 * 通过 {@link ChatMemory#CONVERSATION_ID} 指定会话；会话标识规则见
 * {@link ChatService#getConversationId(String)}，记忆最终由 MongoDB
 * （{@code MongoChatMemoryRepository}）持久化。</p>
 *
 * <p>流式过程由 {@link #GENERATE_STATUS} 标记位控制：{@code stop(sessionId)} 移除标记后，
 * 流的 {@code takeWhile} 判定为 false 而终止输出（对应 PRD 3.3 停止生成）。</p>
 *
 * <p><b>中断落库</b>：{@code MessageChatMemoryAdvisor} 只在流「正常跑完」时才把回答写入记忆，
 * 因此点击停止生成后这一轮回答会丢失，导致后续对话缺失上下文。此处用 {@code outputBuilder}
 * 累积已输出分片，并在 {@code doOnCancel}（前端断连）与 {@code takeWhile} 截断两条路径上
 * 调用 {@link #saveStopHistoryRecord} 补写；两者可能同时触发，故以 CAS 标记保证只写一次，
 * 且模型自然结束时不补写，避免与 Advisor 重复落库。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final SystemPromptConfig systemPromptConfig;
    /** 会话记忆，用于「停止生成」时补写已输出的部分回答 */
    private final ChatMemory chatMemory;
    /** 向量库，用于 RAG 知识检索增强 */
    private final VectorStore vectorStore;

    /** 会话生成状态标记（true=正在生成）。ConcurrentHashMap 保证线程安全；
     * 当前为单体实现，分布式场景可替换为 Redis。 */
    private static final Map<String, Boolean> GENERATE_STATUS = new ConcurrentHashMap<>();

    @Override
    public Flux<ChatEventVO> chat(String message, String sessionId) {
        // 获取对话 id（用户id_会话id），用于记忆仓库隔离不同用户的会话
        String conversationId = ChatService.getConversationId(sessionId);
        // 大模型输出内容的缓存器，用于在输出中断后的数据存储
        StringBuilder outputBuilder = new StringBuilder();

        // RAG 知识检索增强：基于 MongoDB 向量库(habit_knowledge)做语义检索，融入上下文
        QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(this.vectorStore)
                .searchRequest(SearchRequest.builder()
                        .similarityThreshold(0.6d) // 相似度阈值，低于此分不纳入上下文
                        .topK(6) // 最多取 6 条相关知识
                        .build())
                .build();
        // 中断补写的幂等标记：确保部分回答最多只落库一次
        AtomicBoolean stopRecordSaved = new AtomicBoolean(false);
        // 是否被 takeWhile 提前截断（true=用户点了停止，而非模型自然输出结束）
        AtomicBoolean truncated = new AtomicBoolean(false);

        return chatClient.prompt()
                .system(promptSystem -> promptSystem
                        .text(systemPromptConfig.getChatSystemMessage().get()) // 系统提示词
                        .param("now", DateUtil.now()) // 当前时间参数
                )
                .advisors(advisor -> advisor
                        .advisors(qaAdvisor) // 注入 RAG 检索增强
                        .param(ChatMemory.CONVERSATION_ID, conversationId)) // 注入会话记忆
                .user(message)
                .stream()
                .chatResponse()
                .doFirst(() -> GENERATE_STATUS.put(sessionId, true)) // 首次输出时标记生成中
                .doOnError(throwable -> GENERATE_STATUS.remove(sessionId)) // 异常时清除标记
                .doOnComplete(() -> GENERATE_STATUS.remove(sessionId)) // 正常完成时清除标记（记忆由 Advisor 自动落库）
                .doOnCancel(() -> {
                    // 流被取消（用户点击「停止生成」或前端断开连接）：
                    // 此时 MessageChatMemoryAdvisor 不会写入回答，需手动补写已输出的部分内容
                    GENERATE_STATUS.remove(sessionId);
                    saveStopHistoryRecord(conversationId, outputBuilder.toString(), stopRecordSaved);
                })
                // 输出过程中判断是否仍在生成：标记被移除（stop）则终止流
                .takeWhile(response -> {
                    boolean generating = Optional.ofNullable(GENERATE_STATUS.get(sessionId)).orElse(false);
                    if (!generating) {
                        // 断言失败即为「被截断」，据此与模型自然输出结束区分开
                        truncated.set(true);
                    }
                    return generating;
                })
                // takeWhile 截断走的是正常完成信号，上游 Advisor 不会落库，需在此补写；
                // 模型自然结束时 truncated 为 false，交由 Advisor 落库，避免重复写入
                .doOnComplete(() -> {
                    if (truncated.get()) {
                        saveStopHistoryRecord(conversationId, outputBuilder.toString(), stopRecordSaved);
                    }
                })
                .map(chatResponse -> {
                    // 获取大模型的输出的内容
                    String text = chatResponse.getResult().getOutput().getText();
                    // 追加到输出内容中，供中断时落库使用
                    outputBuilder.append(text);
                    // 封装响应对象
                    return ChatEventVO.builder()
                            .eventData(text)
                            .eventType(ChatEventTypeEnum.DATA.getValue())
                            .build();
                })
                .concatWith(Flux.just(ChatEventVO.builder()  // 标记输出结束
                        .eventType(ChatEventTypeEnum.STOP.getValue())
                        .build()));
    }

    /**
     * 保存「停止输出」时已生成的部分内容到会话记忆。
     *
     * <p>正常输出结束时，{@code MessageChatMemoryAdvisor} 会自动把完整回答写入记忆；
     * 但流被中断（stop / 前端断连）时该写入不会发生，导致这一轮回答丢失，
     * 下一轮对话也就失去了上下文，故此处手动补写。</p>
     *
     * @param conversationId 对话 id
     * @param content        大模型已输出的内容
     * @param saved          幂等标记，防止 cancel 与 complete 信号同时触发造成重复落库
     */
    private void saveStopHistoryRecord(String conversationId, String content, AtomicBoolean saved) {
        // 未输出任何内容则无需落库，避免写入空的助手消息
        if (content == null || content.isBlank()) {
            return;
        }
        // CAS 保证只写一次
        if (!saved.compareAndSet(false, true)) {
            return;
        }
        chatMemory.add(conversationId, new AssistantMessage(content));
        log.info("已保存中断输出的部分回答: conversationId={}, length={}", conversationId, content.length());
    }

    @Override
    public void stop(String sessionId) {
        // 移除生成标记，使对应会话的流式 takeWhile 判定为 false 而终止
        GENERATE_STATUS.remove(sessionId);
        log.info("已请求停止会话生成: sessionId={}", sessionId);
    }
}

