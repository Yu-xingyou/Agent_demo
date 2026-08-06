package com.habit.agent.common.stream;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import lombok.extern.slf4j.Slf4j;

/**
 * 响应式「停止生成」信号注册表（阶段五 5-2 真流式改造）。
 *
 * <p>取代原先 {@code ChatController} 中基于 {@code ConcurrentHashMap<String, Boolean>} 的
 * 轮询标志位：本实现以 {@link Sinks.Many} 按 conversationId 注册一个停止信号源，
 * 流式主链通过 {@link #register(String)} 取得信号 Flux，配合 {@code takeUntilOther} 在信号到达时
 * <b>真正取消上游订阅</b>（切断与 DashScope 的 HTTP 连接、停止计费），而非消费后丢弃。
 *
 * <p>生命周期：{@link #register(String)} 在对话开始时调用，返回信号 Flux；
 * {@link #stop(String)} 由 {@code /api/chat/stop} 调用以触发中断；
 * {@link #release(String)} 在流结束/异常/取消的 {@code doFinally} 中调用以清理条目，避免内存泄漏。
 */
@Slf4j
@Component
public class StopSignalRegistry {

    private final ConcurrentHashMap<String, Sinks.Many<Void>> signals = new ConcurrentHashMap<>();

    /** 为指定会话注册停止信号源，返回信号 Flux（多个订阅者安全）。 */
    public Flux<Void> register(String conversationId) {
        Sinks.Many<Void> sink = Sinks.many().multicast().onBackpressureBuffer();
        signals.put(conversationId, sink);
        return sink.asFlux();
    }

    /** 触发指定会话的停止信号；若会话不存在则忽略。 */
    public void stop(String conversationId) {
        Sinks.Many<Void> sink = signals.get(conversationId);
        if (sink != null) {
            sink.tryEmitNext(null);
            log.debug("[StopSignal] 已触发停止信号：conversationId={}", conversationId);
        }
    }

    /** 对话完全结束后清理信号源条目，防止内存泄漏。 */
    public void release(String conversationId) {
        Sinks.Many<Void> removed = signals.remove(conversationId);
        if (removed != null) {
            removed.tryEmitComplete();
        }
    }
}
