package com.habit.agent.controller;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.habit.agent.common.result.Result;
import com.habit.agent.common.vo.ChatRequestVO;
import com.habit.agent.common.vo.ChatResponseVO;
import com.habit.agent.service.ChatService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * 阶段五（AI 对话）ChatController：3 端点。
 *
 * <ul>
 *   <li>POST /api/chat — 非流式对话；</li>
 *   <li>GET /api/chat/stream — SSE 流式对话；</li>
 *   <li>POST /api/chat/stop — 停止生成（阶段五基础版：清除会话记忆窗口，标记中断）。</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "AI 对话", description = "生活习惯助手对话接口（非流式 / 流式 SSE / 停止）")
public class ChatController {

    private final ChatService chatService;

    /** 停止信号：conversationId -> true 表示请求中断。基础版仅做标记位，流式循环检测。 */
    private final ConcurrentHashMap<String, Boolean> stopFlags = new ConcurrentHashMap<>();

    @Operation(summary = "非流式对话")
    @PostMapping
    public Result<ChatResponseVO> chat(@RequestBody ChatRequestVO request) {
        String conversationId = request.getConversationId();
        String content = chatService.chat(request.getMessage(), conversationId);
        ChatResponseVO vo = ChatResponseVO.builder()
                .conversationId(conversationId)
                .role("ASSISTANT")
                .content(content)
                .createTime(LocalDateTime.now())
                .metadata(ChatResponseVO.Metadata.builder()
                        .model("qwen-plus")
                        .build())
                .build();
        return Result.success(vo);
    }

    @Operation(summary = "SSE 流式对话")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestParam String message,
            @RequestParam(required = false) String conversationId) {

        final String cid = (conversationId == null || conversationId.isBlank())
                ? UUID.randomUUID().toString()
                : conversationId;
        stopFlags.remove(cid);

        SseEmitter emitter = new SseEmitter(120_000L);
        // meta 事件：会话元信息
        try {
            emitter.send(SseEmitter.event().name("meta")
                    .data("{\"conversationId\":\"" + cid + "\",\"timestamp\":\""
                            + LocalDateTime.now() + "\",\"model\":\"qwen-plus\"}"));
        } catch (Exception e) {
            emitter.completeWithError(e);
            return emitter;
        }

        Flux<String> flux = chatService.stream(message, cid);
        StringBuilder full = new StringBuilder();
        final int[] index = {0};

        flux.subscribe(
                chunk -> {
                    if (Boolean.TRUE.equals(stopFlags.get(cid))) {
                        sendDone(emitter, cid, 0, 0);
                        emitter.complete();
                        return;
                    }
                    full.append(chunk);
                    try {
                        emitter.send(SseEmitter.event().name("chunk")
                                .data("{\"content\":" + jsonStr(chunk) + ",\"index\":" + index[0]++ + "}"));
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                },
                error -> {
                    log.error("[流式对话异常] conversationId={}", cid, error);
                    try {
                        emitter.send(SseEmitter.event().name("error")
                                .data("{\"errorCode\":\"AI_TIMEOUT\",\"message\":\"AI 响应异常\","
                                        + "\"conversationId\":\"" + cid + "\"}"));
                    } catch (Exception ignored) {
                        // ignore
                    }
                    emitter.complete();
                },
                () -> {
                    sendDone(emitter, cid, full.length(), 0);
                    emitter.complete();
                });

        emitter.onTimeout(() -> emitter.complete());
        return emitter;
    }

    @Operation(summary = "停止生成")
    @PostMapping("/stop")
    public Result<Void> stop(@RequestParam String conversationId) {
        stopFlags.put(conversationId, true);
        return Result.success();
    }

    private void sendDone(SseEmitter emitter, String conversationId, int len, long duration) {
        try {
            emitter.send(SseEmitter.event().name("done")
                    .data("{\"conversationId\":\"" + conversationId
                            + "\",\"totalTokens\":0,\"duration\":" + duration + "}"));
        } catch (Exception ignored) {
            // ignore
        }
    }

    /** 简易 JSON 字符串转义（用于 SSE data 内联）。 */
    private String jsonStr(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
