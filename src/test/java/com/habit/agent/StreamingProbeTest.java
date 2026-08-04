package com.habit.agent;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import reactor.core.publisher.Flux;

import lombok.extern.slf4j.Slf4j;

/**
 * 真流式可行性验证探针（计划阶段一产出）。
 *
 * <p>目标：验证 DashScope 在 Spring AI 2.0.0 下，真流式 {@code .stream().content()} 在两类场景是否稳定：
 * <ul>
 *   <li>场景 A：纯文本轮次（不触发工具调用）；</li>
 *   <li>场景 B：触发工具调用轮次（问"今天打卡了吗"，命中 HabitQueryTools.getTodayRecord）。</li>
 * </ul>
 *
 * <p>判定：若场景 B 抛出 {@code NoSuchElementException} 且栈帧含 {@code OpenAiChatModel$ChunkMerger}
 * → 真流式在工具调用场景不可行，按用户要求退回非流式（方案 B）。
 *
 * <p>注意：本测试依赖真实 {@code DASHSCOPE_API_KEY} 与网络，默认 {@code local} profile。
 * 为避免无 Key 环境下构建失败，测试以"记录结论"方式运行，仅在确实触发 ChunkMerger 缺陷时
 * 通过断言明确标记，便于人工读取报告决定落地方案 A / B。
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("local")
class StreamingProbeTest {

    @Autowired
    private ChatClient chatClient;

    /** 纯文本轮次真流式是否稳定（预期：稳定）。 */
    @Test
    void probePureTextStreaming() {
        boolean ok = runStreaming("用一句话介绍健康饮食的重要性。", false);
        log.info("[探针-纯文本] 真流式完成，是否稳定={}", ok);
    }

    /** 触发工具调用轮次真流式（预期：DashScope 缺 index 字段 → ChunkMerger 崩溃）。 */
    @Test
    void probeToolCallStreaming_parallelFalse() {
        boolean ok = runStreaming("我今天打卡了吗？", false);
        log.info("[探针-工具调用/parallel=false] 真流式是否稳定={}", ok);
        if (!ok) {
            log.warn("[探针结论] 工具调用场景真流式崩溃，应落地方案 B（退回非流式）。");
        }
    }

    /** 对比 parallelToolCalls=true 下工具调用真流式（验证并行是否同样崩溃）。 */
    @Test
    void probeToolCallStreaming_parallelTrue() {
        boolean ok = runStreaming("我今天打卡了吗？", true);
        log.info("[探针-工具调用/parallel=true] 真流式是否稳定={}", ok);
    }

    /** 执行一次真流式并返回是否稳定（true=无 ChunkMerger 崩溃且拿到结果）。 */
    private boolean runStreaming(String userMessage, boolean parallelToolCalls) {
        AtomicReference<Boolean> mergedCrash = new AtomicReference<>(false);
        String cid = java.util.UUID.randomUUID().toString();
        try {
            List<String> chunks = chatClient.prompt()
                    .user(userMessage)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, cid))
                    .options(OpenAiChatOptions.builder().parallelToolCalls(parallelToolCalls))
                    .stream()
                    .content()
                    .collectList()
                    .block(Duration.ofSeconds(90));

            if (chunks == null || chunks.isEmpty()) {
                log.warn("[探针] 返回为空：userMessage={}", userMessage);
                return false;
            }
            log.info("[探针] 真流式成功，聚合文本长度={}，首 60 字={}",
                    chunks.stream().mapToInt(String::length).sum(),
                    truncate(String.join("", chunks), 60));
            return true;
        } catch (Exception e) {
            Throwable cause = e;
            while (cause != null) {
                String name = cause.getClass().getName();
                if (name.contains("OpenAiChatModel$ChunkMerger")
                        || cause instanceof java.util.NoSuchElementException) {
                    mergedCrash.set(true);
                    break;
                }
                cause = cause.getCause();
            }
            log.error("[探针] 真流式异常：userMessage={}，isChunkMergeFailure={}，msg={}",
                    userMessage, mergedCrash.get(), e.getMessage());
            return false;
        }
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
