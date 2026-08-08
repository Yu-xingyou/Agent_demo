package com.habit.agent.aigc.controller;

import com.habit.agent.aigc.dto.ChatDTO;
import com.habit.agent.aigc.service.ChatService;
import com.habit.agent.aigc.vo.ChatEventVO;
import com.habit.agent.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 对话接口（SSE 端点直接返回 Flux 不包 Result）
 */
@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 流式对话（SSE，text/event-stream）
     */
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatEventVO> chat(@RequestBody ChatDTO chatDTO) {
        return this.chatService.chat(chatDTO.getQuestion(), chatDTO.getSessionId());
    }

    /**
     * 停止生成
     */
    @PostMapping("/stop")
    public Result<Void> stop(@RequestParam("sessionId") String sessionId) {
        this.chatService.stop(sessionId);
        return Result.success();
    }
}
