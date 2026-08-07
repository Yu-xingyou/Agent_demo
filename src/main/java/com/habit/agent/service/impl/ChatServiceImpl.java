package com.habit.agent.service.impl;

import com.habit.agent.enums.ChatEventTypeEnum;
import com.habit.agent.service.ChatService;
import com.habit.agent.vo.ChatEventVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 流式对话服务实现（Spring AI 2.0）。
 *
 * <p>参照 1.0 参考代码思路，适配本项目：使用 PRD 3.2 的入参（message / conversationId），
 * 通过 {@link ChatClient} 流式拉取大模型输出，逐分片封装为 {@code 1001} 数据事件，
 * 结束时发送 {@code 1002} 停止事件。</p>
 *
 * <p>本期不落库、不调用路由/子智能体/RAG（PRD 3.2 蓝图中的后续能力），
 * 仅打通「问题 → 流式回答」主链路；后续可在本类扩展会话记忆与检索增强。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;

    @Override
    public Flux<ChatEventVO> chat(String message, String conversationId) {
        // Spring AI 2.0：stream().content() 返回逐 token 的 Flux<String>
        return chatClient.prompt()
                .user(message)
                .stream()
                .content()
                .map(text -> ChatEventVO.builder()
                        .eventData(text)
                        .eventType(ChatEventTypeEnum.DATA.getValue())
                        .build())
                .concatWith(Flux.just(ChatEventVO.builder()
                        .eventType(ChatEventTypeEnum.STOP.getValue())
                        .build()));
    }
}
