package com.habit.agent.aigc.service.impl;

import com.habit.agent.aigc.config.SessionProperties;
import com.habit.agent.aigc.entity.mongo.ChatMessageDoc;
import com.habit.agent.aigc.entity.mongo.ChatSessionDoc;
import com.habit.agent.aigc.enums.MessageTypeEnum;
import com.habit.agent.aigc.repository.ChatMessageRepository;
import com.habit.agent.aigc.repository.ChatSessionRepository;
import com.habit.agent.aigc.service.ChatService;
import com.habit.agent.aigc.service.ChatSessionService;
import com.habit.agent.aigc.vo.MessageVO;
import com.habit.agent.aigc.vo.SessionVO;
import com.habit.agent.common.constant.AgentConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 会话服务实现（参照天机学堂 tj-aigc ChatSessionServiceImpl，MyBatis-Plus 改为 MongoDB）
 * <p>
 * 数据链路：创建会话（随机示例 + UUID + 落库 chatSession）→ 历史回显（读 chatMessage）→
 * 会话列表（按最后消息时间倒序）→ 删除（级联清理 chatSession + chatMessage + chatMemory）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl implements ChatSessionService {

    private final SessionProperties sessionProperties;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMemoryRepository chatMemoryRepository;

    @Override
    public SessionVO createSession(Integer num) {
        int n = (num == null || num <= 0) ? 3 : num;
        // 随机生成 sessionId
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();

        // 构建返回 VO
        SessionVO sessionVO = SessionVO.builder()
                .sessionId(sessionId)
                .title(sessionProperties.getTitle())
                .describe(sessionProperties.getDescribe())
                .examples(randomEleList(sessionProperties.getExamples(), n))
                .lastMessageTime(now)
                .build();

        // 落库 chatSession（TTL 7 天）
        ChatSessionDoc doc = ChatSessionDoc.builder()
                .sessionId(sessionId)
                .userId(AgentConstants.DEFAULT_USER_ID)
                .title("")
                .describe(sessionProperties.getDescribe())
                .lastMessageTime(now)
                .createTime(now)
                .expireAt(now.plusDays(7))
                .build();
        chatSessionRepository.save(doc);

        return sessionVO;
    }

    @Override
    public List<SessionVO.Example> hotExamples(Integer num) {
        int n = (num == null || num <= 0) ? 3 : num;
        return randomEleList(sessionProperties.getExamples(), n);
    }

    @Override
    public List<MessageVO> queryBySessionId(String sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreateTimeAsc(sessionId).stream()
                .filter(msg -> "USER".equals(msg.getRole()) || "ASSISTANT".equals(msg.getRole()))
                .map(msg -> MessageVO.builder()
                        .type(MessageTypeEnum.valueOf(msg.getRole()))
                        .content(msg.getContent())
                        .build())
                .toList();
    }

    @Override
    public List<SessionVO> listSessions() {
        return chatSessionRepository.findByUserIdOrderByLastMessageTimeDesc(AgentConstants.DEFAULT_USER_ID).stream()
                .map(doc -> SessionVO.builder()
                        .sessionId(doc.getSessionId())
                        .title(doc.getTitle())
                        .describe(doc.getDescribe())
                        .lastMessageTime(doc.getLastMessageTime())
                        .build())
                .toList();
    }

    @Override
    public void deleteSession(String sessionId) {
        // 级联清理三处
        chatSessionRepository.deleteBySessionId(sessionId);
        chatMessageRepository.deleteBySessionId(sessionId);
        chatMemoryRepository.deleteByConversationId(ChatService.getConversationId(sessionId));
        log.info("删除会话：sessionId={}", sessionId);
    }

    /**
     * 随机取 n 个示例（hutool RandomUtil.randomEleList 本地实现）
     */
    private List<SessionVO.Example> randomEleList(List<SessionVO.Example> source, int n) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        List<SessionVO.Example> list = new ArrayList<>(source);
        Collections.shuffle(list);
        return list.subList(0, Math.min(n, list.size()));
    }
}
