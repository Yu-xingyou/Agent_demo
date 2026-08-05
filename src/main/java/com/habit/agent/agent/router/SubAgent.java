package com.habit.agent.agent.router;

import org.springframework.ai.chat.client.ChatClient;

/**
 * 阶段九（多智能体路由）子 Agent 统一接口。
 *
 * <p>每个子 Agent 接收路由后的用户消息与可选的会话上下文（conversationId），
 * 返回该领域专家视角的回复文本。真实 LLM 调用由实现类基于全局 {@code ChatClient} 发起，
 * 通过各自的 system 提示词固化角色边界，避免越界。
 */
public interface SubAgent {

    /**
     * 处理一条会话消息。
     *
     * @param message 用户原始消息
     * @param conversationId 会话隔离 ID（用于记忆隔离，可为 null）
     * @return 子 Agent 的回复文本
     */
    String handle(String message, String conversationId);

    /** 该子 Agent 负责的意图类型。 */
    IntentRouter.Intent intent();

    /** 子 Agent 角色名（用于日志与可观测）。 */
    String roleName();
}
