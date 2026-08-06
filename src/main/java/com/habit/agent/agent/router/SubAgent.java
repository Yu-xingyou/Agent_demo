package com.habit.agent.agent.router;

import org.springframework.ai.chat.client.ChatClientResponse;

import reactor.core.publisher.Flux;

/**
 * 阶段九（多智能体路由）子 Agent 统一接口。
 *
 * <p>每个子 Agent 接收路由后的用户消息与可选的会话上下文（conversationId），
 * 返回该领域专家视角的回复文本。真实 LLM 调用由实现类基于全局 {@code ChatClient} 发起，
 * 通过各自的 system 提示词固化角色边界，避免越界。
 *
 * <p>阶段五 5-2 真流式改造：在原有同步 {@link #handle(String, String)} 之外，
 * 新增 {@link #handleStream(String, String)} 以响应式 {@link Flux} 返回增量结果，
 * 供对话主流程逐字下发。两者共用 {@code AbstractSubAgent} 的 prompt 构建与降级逻辑。
 */
public interface SubAgent {

    /**
     * 处理一条会话消息（同步、一次性返回）。
     *
     * @param message 用户原始消息
     * @param conversationId 会话隔离 ID（用于记忆隔离，可为 null）
     * @return 子 Agent 的回复文本
     */
    String handle(String message, String conversationId);

    /**
     * 处理一条会话消息（真流式、增量返回）。
     *
     * @param message 用户原始消息
     * @param conversationId 会话隔离 ID（用于记忆隔离，可为 null）
     * @return 流式响应分片（含文本增量与工具/元数据）
     */
    Flux<ChatClientResponse> handleStream(String message, String conversationId);

    /** 该子 Agent 负责的意图类型。 */
    IntentRouter.Intent intent();

    /** 子 Agent 角色名（用于日志与可观测）。 */
    String roleName();
}
