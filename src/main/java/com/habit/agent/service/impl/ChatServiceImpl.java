package com.habit.agent.service.impl;

import cn.hutool.core.date.DateUtil;
import com.habit.agent.config.SystemPromptConfig;
import com.habit.agent.enums.ChatEventTypeEnum;
import com.habit.agent.service.ChatService;
import com.habit.agent.vo.ChatEventVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流式对话服务实现（Spring AI 2.0）。
 *
 * <p>参照示例思路，适配本项目：使用 PRD 3.2 的入参（message / sessionId），
 * 通过 {@link ChatClient} 流式拉取大模型输出，逐分片封装为 {@code 1001} 数据事件，
 * 结束时发送 {@code 1002} 停止事件。</p>
 *
 * <p>系统提示词由 {@link SystemPromptConfig} 注入（身份：健康习惯管理助手），
 * 并携带当前时间参数 {@code now}（提示词模板中用 {@code {now}} 占位）。</p>
 *
 * <p>多轮记忆由 {@code MessageChatMemoryAdvisor}（已注册到 {@link ChatClient}）自动注入，
 * 通过 {@link ChatMemory#CONVERSATION_ID} 指定会话；会话标识规则见
 * {@link ChatService#getConversationId(String)}，记忆最终由 MongoDB
 * （{@code MongoChatMemoryRepository}）持久化。</p>
 *
 * <p>流式过程由 {@link #GENERATE_STATUS} 标记位控制：{@code stop(sessionId)} 移除标记后，
 * 流的 {@code takeWhile} 判定为 false 而终止输出（对应 PRD 3.3 停止生成）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final SystemPromptConfig systemPromptConfig;

    /** 会话生成状态标记（true=正在生成）。ConcurrentHashMap 保证线程安全；
     * 当前为单体实现，分布式场景可替换为 Redis。 */
    private static final Map<String, Boolean> GENERATE_STATUS = new ConcurrentHashMap<>();

    @Override
    public Flux<ChatEventVO> chat(String message, String sessionId) {
        // 获取对话 id（用户id_会话id），用于记忆仓库隔离不同用户的会话
        String conversationId = ChatService.getConversationId(sessionId);

        return chatClient.prompt()
                .system(promptSystem -> promptSystem
                        .text(systemPromptConfig.getChatSystemMessage().get()) // 系统提示词
                        .param("now", DateUtil.now()) // 当前时间参数
                )
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId)) // 注入会话记忆
                .user(message)
                .stream()
                .chatResponse()
                .doFirst(() -> GENERATE_STATUS.put(sessionId, true)) // 首次输出时标记生成中
                .doOnError(throwable -> GENERATE_STATUS.remove(sessionId)) // 异常时清除标记
                .doOnComplete(() -> GENERATE_STATUS.remove(sessionId)) // 完成时清除标记
                .takeWhile(response -> Optional.ofNullable(GENERATE_STATUS.get(sessionId)).orElse(false)) // 标记被移除则终止流
                .map(chatResponse -> {
                    // 获取大模型的输出的内容
                    String text = chatResponse.getResult().getOutput().getText();
                    // 封装响应对象
                    return ChatEventVO.builder()
                            .eventData(text)
                            .eventType(ChatEventTypeEnum.DATA.getValue())
                            .build();
                })
                .concatWith(Flux.just(ChatEventVO.builder()  // 标记输出结束
                        .eventType(ChatEventTypeEnum.STOP.getValue())
                        .build()));
    }

    @Override
    public void stop(String sessionId) {
        // 移除生成标记，使对应会话的流式 takeWhile 判定为 false 而终止
        GENERATE_STATUS.remove(sessionId);
        log.info("已请求停止会话生成: sessionId={}", sessionId);
    }
}

