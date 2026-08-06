package com.habit.agent.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.habit.agent.common.result.Result;
import com.habit.agent.common.stream.StopSignalRegistry;
import com.habit.agent.common.vo.ChatRequestVO;
import com.habit.agent.common.vo.ChatResponseVO;
import com.habit.agent.common.vo.ChatStreamEvent;
import com.habit.agent.service.ChatService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * 阶段五（AI 对话）ChatController：多端点。
 *
 * <ul>
 *   <li>POST /api/chat — 非流式对话；</li>
 *   <li>GET /api/chat/stream — 真流式 SSE 对话（阶段五 5-2 改造为响应式 Flux&lt;ServerSentEvent&gt;）；</li>
 *   <li>POST /api/chat/stop — 停止生成（触发响应式停止信号，真正取消上游订阅）；</li>
 *   <li>GET /api/chat/history — 会话历史；</li>
 *   <li>POST /api/chat/title — 生成会话标题。</li>
 * </ul>
 *
 * <p>真流式改造（2026-08）：原 {@code SseEmitter} + 手工拼 JSON 字符串的实现已移除，
 * 改用 Spring AI 2.0.0 推荐的响应式写法——接口返回 {@code Flux<ServerSentEvent<ChatStreamEvent>>}，
 * 事件体由 Jackson 序列化（不再手工拼接）。停止生成改用 {@link StopSignalRegistry} 的响应式信号，
 * 由服务层 {@code takeUntilOther} 真正取消与模型的上游订阅（切断连接、停止计费）。
 *
 * <p>SSE 事件协议（向后兼容前端解析）：
 * <ul>
 *   <li><b>meta</b>：流开始，携带 conversationId、timestamp、model、intent；</li>
 *   <li><b>chunk</b>：文本增量分片（真流式逐字）；</li>
 *   <li><b>tool_call</b>：工具调用状态（start/end + message + toolName）；</li>
 *   <li><b>done</b>：流结束，含真实 Token 用量、耗时、首字延迟、streaming_mode="streaming"；</li>
 *   <li><b>error</b>：流出错，含 errorCode/message/retryable。</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "AI 对话", description = "生活习惯助手对话接口（非流式 / 流式 SSE / 停止）")
public class ChatController {

    private final ChatService chatService;
    private final StopSignalRegistry stopSignalRegistry;

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

    @Operation(summary = "真流式 SSE 对话（逐字输出）")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatStreamEvent>> stream(
            @RequestParam String message,
            @RequestParam(required = false) String conversationId) {

        final String cid = (conversationId == null || conversationId.isBlank())
                ? UUID.randomUUID().toString()
                : conversationId;

        return chatService.stream(message, cid)
                .map(event -> ServerSentEvent.<ChatStreamEvent>builder()
                        .event(event.eventName())
                        .data(event)
                        .build())
                .doOnSubscribe(s -> log.debug("[流式对话开始] conversationId={}", cid))
                .doOnError(e -> log.error("[流式对话异常] conversationId={}", cid, e));
    }

    @Operation(summary = "停止生成")
    @PostMapping("/stop")
    public Result<Void> stop(@RequestParam String conversationId) {
        stopSignalRegistry.stop(conversationId);
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
}
