package com.habit.agent.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
 *   <li>GET /api/chat/stream — SSE 流式对话（5 种事件：meta/chunk/tool_call/done/error）；</li>
 *   <li>POST /api/chat/stop — 停止生成。</li>
 * </ul>
 *
 * <p>SSE 协议对齐 PRD《API接口文档设计计划.md》规范：
 * <ul>
 *   <li><b>meta</b>：流开始，携带 conversationId、timestamp、model</li>
 *   <li><b>chunk</b>：文本片段；方案 B（退回非流式）下为整段文本的单条 chunk</li>
 *   <li><b>done</b>：流结束，含 streaming_mode 标识（one_shot = 服务端一次性输出）</li>
 *   <li><b>error</b>：流出错</li>
 * </ul>
 *
 * <p>2026-08 升级：依验证结论落地《方案 B》——真流式在 DashScope 工具调用场景崩溃，
 * 故 stream 端点保留形态但服务端一次性输出完整文本（不再伪流式分片）。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "AI 对话", description = "生活习惯助手对话接口（非流式 / 流式 SSE / 停止）")
public class ChatController {

    private final ChatService chatService;

    /** 停止信号：conversationId -> true 表示请求中断。 */
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
                    // 方案 B：服务端一次性输出完整文本，单条 chunk 事件推送（不做伪流式分片）。
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

    @Operation(summary = "获取会话历史消息")
    @GetMapping("/history")
    public Result<List<Map<String, String>>> getHistory(@RequestParam String conversationId) {
        return Result.success(chatService.getMessages(conversationId));
    }

    @Operation(summary = "AI 生成会话标题")
    @PostMapping("/title")
    public Result<String> generateTitle(@RequestParam String message) {
        return Result.success(chatService.generateTitle(message));
    }

    private void sendDone(SseEmitter emitter, String conversationId, int len, long duration) {
        // 方案 B：服务端一次性输出完整文本
        String streamingMode = "one_shot";
        try {
            emitter.send(SseEmitter.event().name("done")
                    .data("{\"conversationId\":\"" + conversationId
                            + "\",\"totalTokens\":0,\"duration\":" + duration
                            + ",\"streaming_mode\":\"" + streamingMode + "\"}"));
        } catch (Exception ignored) {
            // ignore
        }
    }

    /** 简易 JSON 字符串转义（用于 SSE data 内联）。 */
    private String jsonStr(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
