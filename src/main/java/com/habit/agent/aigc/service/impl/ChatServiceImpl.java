package com.habit.agent.aigc.service.impl;

import com.habit.agent.aigc.entity.mongo.ChatMessageDoc;
import com.habit.agent.aigc.enums.ChatEventTypeEnum;
import com.habit.agent.aigc.repository.ChatMessageRepository;
import com.habit.agent.aigc.repository.ChatSessionRepository;
import com.habit.agent.aigc.service.ChatService;
import com.habit.agent.aigc.vo.ChatEventVO;
import com.habit.agent.agent.Agent;
import com.habit.agent.agent.AgentRouter;
import com.habit.agent.agent.AgentTypeEnum;
import com.habit.agent.agent.RouteAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对话服务实现
 * <p>
 * 数据链路：用户消息即时写 chatMessage → RouteAgent 意图识别 → AgentRouter 分发 →
 * 子智能体流式对话（SSE 1001 数据事件）→ 流结束写 AI 消息入 chatMessage（完整历史）→ 输出 1002 停止事件。
 * 生成状态用 ConcurrentHashMap 标记（GENERATE_STATUS），stop() 移除标记使 takeWhile 截断并触发中断补写。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final RouteAgent routeAgent;
    private final AgentRouter agentRouter;

    /**
     * 生成状态标记容器：sessionId -> true
     * stop() 移除标记，takeWhile 检测到无标记即截断当前流式输出
     */
    private static final Map<String, Boolean> GENERATE_STATUS = new ConcurrentHashMap<>();

    @Override
    public Flux<ChatEventVO> chat(String question, String sessionId) {
        var outputBuilder = new StringBuilder();
        return Flux.defer(() -> {
            // 1. 路由识别意图，选择子智能体
            AgentTypeEnum type = routeAgent.route(question);
            Agent agent = agentRouter.get(type);
            log.info("对话分发：sessionId={}, question={}, agentType={}", sessionId, question, type);

            return agent.processStream(question, sessionId)
                    .doFirst(() -> GENERATE_STATUS.put(sessionId, Boolean.TRUE))
                    .doOnNext(event -> {
                        if (event.getEventType() == ChatEventTypeEnum.DATA.getValue()) {
                            String text = String.valueOf(event.getEventData());
                            outputBuilder.append(text == null ? "" : text);
                        }
                    })
                    .doOnError(throwable -> {
                        GENERATE_STATUS.remove(sessionId);
                        log.error("对话流式生成出错：sessionId={}", sessionId, throwable);
                    })
                    .doOnComplete(() -> {
                        GENERATE_STATUS.remove(sessionId);
                        saveAssistantMessage(sessionId, outputBuilder.toString());
                    })
                    .doOnCancel(() -> {
                        // 用户手动停止：补写已生成内容
                        GENERATE_STATUS.remove(sessionId);
                        saveAssistantMessage(sessionId, outputBuilder.toString());
                    })
                    .takeWhile(event -> GENERATE_STATUS.containsKey(sessionId))
                    .concatWith(Flux.just(ChatEventVO.builder()
                            .eventType(ChatEventTypeEnum.STOP.getValue())
                            .build()));
        }).doFirst(() -> {
            // 2. 用户消息即时入库（完整历史，供前端回显）
            saveUserMessage(sessionId, question);
        });
    }

    @Override
    public void stop(String sessionId) {
        GENERATE_STATUS.remove(sessionId);
        log.info("停止生成：sessionId={}", sessionId);
    }

    /**
     * 保存用户消息（chatMessage）并刷新会话最后消息时间；会话标题为空时自动生成
     *
     * @param sessionId 会话 id
     * @param question  用户提问文本
     */
    private void saveUserMessage(String sessionId, String question) {
        LocalDateTime now = LocalDateTime.now();
        ChatMessageDoc message = ChatMessageDoc.builder()
                .sessionId(sessionId)
                .role("USER")
                .content(question)
                .createTime(now)
                .build();
        chatMessageRepository.save(message);

        chatSessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            if (!StringUtils.hasText(session.getTitle())) {
                session.setTitle(genTitle(question));
            }
            session.setLastMessageTime(now);
            chatSessionRepository.save(session);
        });
    }

    /**
     * 保存 AI 消息（chatMessage）
     *
     * @param sessionId 会话 id
     * @param content   AI 回复文本（为空则不落库）
     */
    private void saveAssistantMessage(String sessionId, String content) {
        if (!StringUtils.hasText(content)) {
            return;
        }
        chatMessageRepository.save(ChatMessageDoc.builder()
                .sessionId(sessionId)
                .role("ASSISTANT")
                .content(content)
                .createTime(LocalDateTime.now())
                .build());
    }

    /**
     * 由用户首条消息生成会话标题（前 20 字）
     *
     * @param question 用户提问文本
     * @return 截取前 20 字并追加省略号的标题；问题为空时返回空串
     */
    private String genTitle(String question) {
        String text = question == null ? "" : question.trim();
        return text.length() > 20 ? text.substring(0, 20) + "…" : text;
    }
}
