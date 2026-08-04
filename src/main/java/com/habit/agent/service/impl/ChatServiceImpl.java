package com.habit.agent.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.entity.mongo.ChatSession;
import com.habit.agent.repository.mongo.ChatSessionRepository;
import com.habit.agent.service.ChatService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

/**
 * 阶段五（对话记忆与流式输出）ChatService 实现。
 *
 * <p>流式策略结论（2026-08 验证）：
 * 经 {@code StreamingProbeTest} 实测，DashScope 在 Spring AI 2.0.0 下，纯文本轮次真流式稳定，
 * 但一旦触发工具调用（业务对话必然命中 HabitQueryTools 等）即因
 * {@code OpenAiChatModel$ChunkMerger} 缺陷崩溃（NoSuchElementException）。
 * 依需求「流式跑不通则退回非流式、删除伪流式代码」，《方案 B》落地：
 * <ul>
 *   <li>SSE 端点 {@code GET /api/chat/stream} 形态保留，符合 PRD 契约与前端解析逻辑。</li>
 *   <li>{@code stream()} 改以 {@code chatOnce()} 一次性取得完整文本，再用 {@code Flux.just} 单元素发射，
 *       由 Controller 以单条 chunk + done 事件推送——一次性输出，不做伪流式分片。</li>
 *   <li>原 __TC__ 伪流式降级（onErrorResume + 分片模拟）已彻底删除。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final ChatSessionRepository chatSessionRepository;

    @Override
    public String chat(String userMessage, String conversationId) {
        final String cid = ensureSession(userMessage, conversationId);
        return chatOnce(userMessage, cid);
    }

    @Override
    public Flux<String> stream(String userMessage, String conversationId) {
        final String cid = ensureSession(userMessage, conversationId);
        // 方案 B：退回非流式——一次性取完整文本，经 SSE 端点单条 chunk 推送（不做伪流式分片）。
        String full = chatOnce(userMessage, cid);
        return Flux.just(full);
    }

    private String chatOnce(String userMessage, String conversationId) {
        return chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .options(OpenAiChatOptions.builder().parallelToolCalls(false))
                .call()
                .content();
    }

    @Override
    public List<Map<String, String>> getMessages(String conversationId) {
        List<org.springframework.ai.chat.messages.Message> msgs =
                chatMemory.get(conversationId);
        List<Map<String, String>> result = new ArrayList<>();
        for (org.springframework.ai.chat.messages.Message m : msgs) {
            // 跳过 SystemMessage，只返回用户和助手的消息
            if (m instanceof org.springframework.ai.chat.messages.SystemMessage) continue;
            Map<String, String> item = new HashMap<>();
            String role = "ai";
            if (m instanceof org.springframework.ai.chat.messages.UserMessage) {
                role = "user";
            } else if (m instanceof org.springframework.ai.chat.messages.AssistantMessage) {
                role = "ai";
            }
            item.put("role", role);
            item.put("text", m.getText());
            result.add(item);
        }
        return result;
    }

    @Override
    public String generateTitle(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return "新会话";
        }
        try {
            String title = chatClient.prompt()
                    .user("用不超过15个字总结这段对话的主题：" + userMessage)
                    .options(OpenAiChatOptions.builder().temperature(0.3))
                    .call()
                    .content();
            if (title == null || title.isBlank() || title.length() > 30) {
                return userMessage.length() > 15 ? userMessage.substring(0, 15) + "…" : userMessage;
            }
            return title.trim();
        } catch (Exception e) {
            log.warn("[标题生成失败] 降级为截断首条消息", e);
            return userMessage.length() > 15 ? userMessage.substring(0, 15) + "…" : userMessage;
        }
    }

    private String ensureSession(String userMessage, String conversationId) {
        String cid = (conversationId == null || conversationId.isBlank())
                ? java.util.UUID.randomUUID().toString()
                : conversationId;
        // 会话已存在则直接复用（保留已有标题/时间）
        if (chatSessionRepository.existsByConversationId(cid)) {
            touchSession(cid);
            return cid;
        }
        // 首次进入该会话时持久化元数据，供历史会话侧边栏展示
        ChatSession session = new ChatSession();
        LocalDateTime now = LocalDateTime.now();
        session.setConversationId(cid);
        session.setUserId(AgentConstants.DEFAULT_USER_ID);
        session.setTitle(userMessage.length() > 20 ? userMessage.substring(0, 20) : userMessage);
        session.setStatus("ACTIVE");
        session.setMessageCount(1);
        session.setCreateTime(now);
        session.setLastMessageTime(now);
        session.setExpireAt(now.plusDays(30));
        chatSessionRepository.save(session);
        return cid;
    }

    /** 更新会话的最后消息时间与消息条数。 */
    private void touchSession(String conversationId) {
        chatSessionRepository.findByConversationId(conversationId).ifPresent(s -> {
            s.setLastMessageTime(LocalDateTime.now());
            s.setMessageCount(s.getMessageCount() + 1);
            chatSessionRepository.save(s);
        });
    }
}
