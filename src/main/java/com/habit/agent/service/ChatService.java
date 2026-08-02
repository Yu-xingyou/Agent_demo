package com.habit.agent.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * 阶段四（Spring AI 基础）ChatService 骨架。
 *
 * <p>当前仅提供最基础的对话能力，验证 ChatClient 与通义千问链路可用：
 * <ul>
 *   <li>{@link #chat(String)} 非流式调用；</li>
 *   <li>{@link #stream(String)} 流式调用（返回 Flux，供阶段五 SSE 端点使用）。</li>
 * </ul>
 *
 * <p>后续阶段将在此 Service 注入对话记忆（阶段五）、业务工具（阶段六）、
 * 多智能体路由（阶段九）与 RAG Advisor（阶段七），本骨架保持零对外端点。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;

    /**
     * 非流式对话：直接返回模型完整回复（用于后端任务调用、测试调试）。
     *
     * @param userMessage 用户输入
     * @return 模型回复文本
     */
    public String chat(String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .call()
                .content();
    }

    /**
     * 流式对话：返回响应式 token 流（阶段五将接入 SSE 端点 {@code GET /api/chat/stream}）。
     *
     * @param userMessage 用户输入
     * @return 模型回复的响应式文本流
     */
    public Flux<String> stream(String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .stream()
                .content();
    }
}
