package com.habit.agent.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.habit.agent.common.vo.SessionVO;
import com.habit.agent.entity.mongo.ChatSession;
import com.habit.agent.repository.mongo.ChatSessionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 阶段五（会话管理）SessionService：维护 MongoDB 中的会话元数据（{@code chatSession} 集合）。
 *
 * <p>对话消息本身由 ChatService 通过 Spring AI 记忆库管理，本 Service 仅管会话维度的
 * 列表 / 创建 / 重命名 / 关闭 / 删除 / 清理过期，不含消息内容读写。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final ChatSessionRepository sessionRepository;

    private static final Long DEMO_USER_ID = 1L;

    /** 1. 用户会话列表（按最后消息时间倒序）。 */
    public List<SessionVO> listSessions() {
        return sessionRepository.findByUserIdOrderByLastMessageTimeDesc(DEMO_USER_ID).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /** 2. 创建新会话（指定或默认标题）。 */
    public SessionVO createSession(String title) {
        ChatSession session = new ChatSession();
        session.setConversationId(java.util.UUID.randomUUID().toString());
        session.setUserId(DEMO_USER_ID);
        session.setTitle(title == null || title.isBlank() ? "新对话" : title);
        session.setStatus("ACTIVE");
        session.setMessageCount(0);
        session.setCreateTime(LocalDateTime.now());
        sessionRepository.save(session);
        return toVO(session);
    }

    /** 3. 查看指定会话详情。 */
    public SessionVO getSession(String conversationId) {
        return sessionRepository.findById(conversationId)
                .map(this::toVO)
                .orElse(null);
    }

    /** 4. 重命名会话。 */
    public SessionVO renameSession(String conversationId, String title) {
        ChatSession session = sessionRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在: " + conversationId));
        session.setTitle(title);
        sessionRepository.save(session);
        return toVO(session);
    }

    /** 5. 关闭会话（状态置 CLOSED，记忆保留可重新打开）。 */
    public void closeSession(String conversationId) {
        ChatSession session = sessionRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在: " + conversationId));
        session.setStatus("CLOSED");
        sessionRepository.save(session);
    }

    /** 6. 删除会话（仅删元数据；记忆消息由 ChatMemory 窗口策略自行清理）。 */
    public void deleteSession(String conversationId) {
        sessionRepository.deleteById(conversationId);
    }

    /** 7. 清理过期会话（status=CLOSED 且 expireAt 早于 now）。 */
    public int cleanExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        List<ChatSession> expired = sessionRepository.findByUserIdAndStatus(DEMO_USER_ID, "CLOSED").stream()
                .filter(s -> s.getExpireAt() != null && s.getExpireAt().isBefore(now))
                .collect(Collectors.toList());
        expired.forEach(s -> sessionRepository.deleteById(s.getConversationId()));
        return expired.size();
    }

    private SessionVO toVO(ChatSession s) {
        return SessionVO.builder()
                .conversationId(s.getConversationId())
                .userId(s.getUserId())
                .title(s.getTitle())
                .status(s.getStatus())
                .messageCount(s.getMessageCount())
                .lastMessageTime(s.getLastMessageTime())
                .createTime(s.getCreateTime())
                .expireAt(s.getExpireAt())
                .build();
    }
}
