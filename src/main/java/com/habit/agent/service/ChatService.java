package com.habit.agent.service;

import reactor.core.publisher.Flux;

/**
 * 阶段五（对话记忆与流式输出）ChatService 接口。
 */
public interface ChatService {

    /** 非流式对话：调用 ChatClient，注入对话记忆（按 conversationId 隔离）。 */
    String chat(String userMessage, String conversationId);

    /** 流式对话：返回响应式 token 流（供 SSE 端点）。 */
    Flux<String> stream(String userMessage, String conversationId);
}
