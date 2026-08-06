package com.habit.agent.service;

import java.util.List;
import java.util.Map;

import com.habit.agent.common.vo.ChatStreamEvent;

import reactor.core.publisher.Flux;

/**
 * 阶段五/六（对话记忆与流式输出 + Tool Calling）ChatService 接口。
 */
public interface ChatService {

    /** 非流式对话：调用 ChatClient，注入对话记忆（按 conversationId 隔离）。 */
    String chat(String userMessage, String conversationId);

    /**
     * 真流式对话（阶段五 5-2 改造）：返回结构化 SSE 事件流
     * （meta / chunk / tool_call / done / error），由 Controller 映射为 {@code ServerSentEvent}。
     */
    Flux<ChatStreamEvent> stream(String userMessage, String conversationId);

    /** 获取指定会话的历史消息（角色 + 文本），供前端加载历史记录。 */
    List<Map<String, String>> getMessages(String conversationId);

    /** 用 AI 生成会话标题（首轮对话完成后调用）。 */
    String generateTitle(String userMessage);
}
