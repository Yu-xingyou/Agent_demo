package com.habit.agent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.habit.agent.common.vo.ChatStreamEvent;
import com.habit.agent.service.ChatService;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import lombok.extern.slf4j.Slf4j;

/**
 * 真流式可行性验证探针（阶段五 5-2 改造后重写）。
 *
 * <p>目标：验证修复 advisor order 冲突后，DashScope 在 Spring AI 2.0.0 下真流式
 * {@code ChatService.stream()} 在两类场景是否稳定：
 * <ul>
 *   <li>场景 A：纯文本轮次（闲聊，不触发工具调用）；</li>
 *   <li>场景 B：触发工具调用轮次（问"我今天打卡了吗"，命中数据分析智能体的查询工具）。</li>
 * </ul>
 *
 * <p>核心判定（取代原"ChunkMerger 崩溃 → 方案 B"错误结论）：
 * <ul>
 *   <li>chunk 事件数 &gt; 1 → 确为真流式逐字输出（非一次性）；</li>
 *   <li>场景 B 能产出最终文本，且中间出现 {@code tool_call} 事件 → 工具循环由框架透明驱动；</li>
 *   <li>开启 {@code stream-usage: true} 后，done 事件的 totalTokens 应为非 null（真实用量），
 *       用于验证 Token 统计不再恒为 0。</li>
 * </ul>
 *
 * <p>注意：本测试依赖真实 {@code DASHSCOPE_API_KEY} 与网络，默认 {@code local} profile。
 * 无 Key / 离线环境下以"记录结论"方式运行，不强制 fail，便于人工读取报告。
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("local")
class StreamingProbeTest {

    @Autowired
    private ChatService chatService;

    /** 场景 A：纯文本轮次真流式。 */
    @Test
    void probePureTextStreaming() {
        String cid = java.util.UUID.randomUUID().toString();
        AtomicReference<ChatStreamEvent.Done> doneRef = new AtomicReference<>();
        List<String> chunks = new ArrayList<>();

        Flux<ChatStreamEvent> flux = chatService.stream("用一句话介绍健康饮食的重要性。", cid)
                .doOnNext(e -> {
                    if (e instanceof ChatStreamEvent.Chunk c) chunks.add(c.content());
                    if (e instanceof ChatStreamEvent.Done d) doneRef.set(d);
                });

        StepVerifier.create(flux)
                .thenConsumeWhile(e -> true, e -> {
                    if (e instanceof ChatStreamEvent.Meta) log.info("[探针-纯文本] meta: conversationId={} intent={}",
                            ((ChatStreamEvent.Meta) e).conversationId(), ((ChatStreamEvent.Meta) e).intent());
                })
                .verifyComplete();

        int chunkCount = chunks.size();
        log.info("[探针-纯文本] 完成 | chunk数={} | 聚合文本长度={} | done.totalTokens={}",
                chunkCount, chunks.stream().mapToInt(String::length).sum(),
                doneRef.get() == null ? "null" : doneRef.get().totalTokens());

        if (chunkCount > 1) {
            log.info("[探针结论] 纯文本真流式逐字输出 OK（分片数={}）", chunkCount);
        } else {
            log.warn("[探针结论] 纯文本疑似一次性输出（chunk数={}），需检查流式链路", chunkCount);
        }
    }

    /** 场景 B：触发工具调用轮次真流式（验证框架透明驱动工具循环 + Token 统计）。 */
    @Test
    void probeToolCallStreaming() {
        String cid = java.util.UUID.randomUUID().toString();
        AtomicReference<ChatStreamEvent.Done> doneRef = new AtomicReference<>();
        List<String> chunks = new ArrayList<>();
        boolean[] sawToolCall = {false};

        Flux<ChatStreamEvent> flux = chatService.stream("我今天打卡了吗？", cid)
                .doOnNext(e -> {
                    if (e instanceof ChatStreamEvent.Chunk c) chunks.add(c.content());
                    if (e instanceof ChatStreamEvent.ToolCall) sawToolCall[0] = true;
                    if (e instanceof ChatStreamEvent.Done d) doneRef.set(d);
                });

        StepVerifier.create(flux)
                .thenConsumeWhile(e -> true, e -> {
                    if (e instanceof ChatStreamEvent.ToolCall tc)
                        log.info("[探针-工具调用] tool_call: status={} message={} toolName={}",
                                tc.status(), tc.message(), tc.toolName());
                })
                .verifyComplete();

        int chunkCount = chunks.size();
        Integer totalTokens = doneRef.get() == null ? null : doneRef.get().totalTokens();
        boolean hasFinalText = chunks.stream().mapToInt(String::length).sum() > 0;

        log.info("[探针-工具调用] 完成 | 出现tool_call={} | chunk数={} | 最终文本非空={} | done.totalTokens={}",
                sawToolCall[0], chunkCount, hasFinalText, totalTokens);

        if (sawToolCall[0] && hasFinalText) {
            log.info("[探针结论] 工具调用轮次真流式 OK：工具循环由框架透明驱动，最终回复逐字输出。");
        } else {
            log.warn("[探针结论] 工具调用轮次异常：tool_call={}, 最终文本非空={}", sawToolCall[0], hasFinalText);
        }

        if (totalTokens != null && totalTokens > 0) {
            log.info("[探针结论] Token 统计 OK：stream-usage 生效，totalTokens={}", totalTokens);
        } else {
            log.warn("[探针结论] Token 统计为 null/0：DashScope 未回传 usage，需评估 getNativeUsage() 兜底。");
        }
    }

    /** 验证停止信号能真正中断流式输出（场景 A 基础上中途 stop）。 */
    @Test
    void probeStopSignal() {
        String cid = java.util.UUID.randomUUID().toString();
        List<ChatStreamEvent> events = new ArrayList<>();
        // 注意：ChatService.stream 内部注册了停止信号；此处仅验证链路可达且不抛异常。
        // 真正的"中途 stop"需通过 /api/chat/stop 端点或 StopSignalRegistry.stop 触发，
        // 该行为由控制器集成测试覆盖。
        chatService.stream("讲一个关于坚持运动的小故事。", cid)
                .doOnNext(events::add)
                .blockLast(Duration.ofSeconds(90));
        log.info("[探针-停止] 链路完成，事件总数={}（停止信号中断能力由集成测试覆盖）", events.size());
    }
}
