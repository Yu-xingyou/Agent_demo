package com.habit.agent.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.common.exception.BusinessException;
import com.habit.agent.common.vo.SessionVO;
import com.habit.agent.entity.mongo.ChatSession;
import com.habit.agent.repository.mongo.ChatSessionRepository;
import com.habit.agent.service.SessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 阶段五（会话管理）SessionService 实现：维护 MongoDB 会话元数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final ChatSessionRepository sessionRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public List<SessionVO> listSessions() {
        return sessionRepository.findByUserIdOrderByLastMessageTimeDesc(AgentConstants.DEFAULT_USER_ID)
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public SessionVO createSession(String title) {
        ChatSession s = new ChatSession();
        s.setUserId(AgentConstants.DEFAULT_USER_ID);
        s.setTitle(title != null && !title.isBlank() ? title : "新会话");
        LocalDateTime now = LocalDateTime.now();
        s.setStatus("ACTIVE");
        s.setCreateTime(now);
        s.setLastMessageTime(now);
        s.setExpireAt(now.plusDays(30));
        ChatSession saved = sessionRepository.save(s);
        log.info("创建会话: id={}, title={}", saved.getConversationId(), saved.getTitle());
        return toVO(saved);
    }

    @Override
    public SessionVO getSession(String conversationId) {
        ChatSession s = findByCid(conversationId);
        return toVO(s);
    }

    @Override
    public SessionVO renameSession(String conversationId, String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("会话标题不能为空");
        }
        ChatSession s = findByCid(conversationId);
        s.setTitle(title);
        ChatSession saved = sessionRepository.save(s);
        log.info("重命名会话: id={}, title={}", saved.getConversationId(), saved.getTitle());
        return toVO(saved);
    }

    @Override
    public void closeSession(String conversationId) {
        findByCid(conversationId);
        Query q = new Query(Criteria.where("conversationId").is(conversationId));
        Update u = new Update().set("status", "CLOSED");
        mongoTemplate.updateFirst(q, u, ChatSession.class);
        log.info("关闭会话: id={}", conversationId);
    }

    @Override
    public void deleteSession(String conversationId) {
        Query q = new Query(Criteria.where("conversationId").is(conversationId));
        mongoTemplate.remove(q, ChatSession.class);
        log.info("删除会话元数据: id={}", conversationId);
    }

    @Override
    public int cleanExpiredSessions() {
        Query q = new Query(Criteria.where("status").is("CLOSED")
                .and("expireAt").lt(LocalDateTime.now()));
        long n = mongoTemplate.count(q, ChatSession.class);
        if (n > 0) {
            mongoTemplate.remove(q, ChatSession.class);
        }
        log.info("清理过期会话: count={}", n);
        return (int) n;
    }

    private ChatSession findByCid(String conversationId) {
        return sessionRepository.findByConversationId(conversationId)
                .orElseThrow(() -> new BusinessException(AgentConstants.CODE_SESSION_NOT_FOUND,
                        "会话不存在: " + conversationId));
    }

    private SessionVO toVO(ChatSession s) {
        SessionVO vo = new SessionVO();
        vo.setConversationId(s.getConversationId());
        vo.setTitle(s.getTitle());
        vo.setStatus(s.getStatus());
        vo.setCreateTime(s.getCreateTime());
        vo.setLastMessageTime(s.getLastMessageTime());
        vo.setExpireAt(s.getExpireAt());
        return vo;
    }
}
