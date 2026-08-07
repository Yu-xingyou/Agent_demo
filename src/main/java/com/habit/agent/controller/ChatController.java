package com.habit.agent.controller;

import com.habit.agent.service.ChatService;
import com.habit.agent.vo.ChatEventVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * 流式对话 REST API（PRD 第 3 节）。
 *
 * <p>端点 {@code GET /api/chat/stream}：以 text/event-stream 逐行下发
 * {@link ChatEventVO}（{@code data:{eventData,eventType}} 格式），不被 {@code Result} 包裹。
 * 会话 ID 经响应头 {@code X-Conversation-Id} 下发（PRD 1.4）。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "流式对话", description = "Agent 流式问答（SSE，eventType 行式协议）")
public class ChatController {

    /** 会话 ID 响应头名（PRD 1.4） */
    private static final String HEADER_CONVERSATION_ID = "X-Conversation-Id";

    private final ChatService chatService;

    /**
     * GET /api/chat/stream — 流式对话
     *
     * @param message        用户问题
     * @param conversationId 会话 ID（首轮可省略，由后端生成）
     */
    @Operation(summary = "流式对话", description = "以 SSE 流式返回 Agent 回答，会话 ID 经 X-Conversation-Id 响应头下发")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<ChatEventVO>> stream(
            @Parameter(description = "用户问题", example = "如何改善睡眠质量？")
            @RequestParam("message") String message,
            @Parameter(description = "会话ID（首轮可省略）")
            @RequestParam(value = "conversationId", required = false) String conversationId) {

        // 首轮无会话 ID 时生成，并通过响应头下发（控制面信息，不污染事件流）
        String convId = (conversationId == null || conversationId.isBlank())
                ? UUID.randomUUID().toString()
                : conversationId;

        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_CONVERSATION_ID, convId);

        Flux<ChatEventVO> events = chatService.chat(message, convId);
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(events);
    }
}
