package com.habit.agent.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.config.SessionProperties;
import com.habit.agent.entity.mongo.ChatSession;
import com.habit.agent.repository.ChatSessionRepository;
import com.habit.agent.service.ChatSessionService;
import com.habit.agent.vo.SessionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话服务实现。
 *
 * <p>领域适配说明（相对天机课程模板的改造）：
 * <ul>
 *     <li>持久化：MongoDB {@link ChatSessionRepository}，而非 MyBatis-Plus 的 Mapper/ServiceImpl</li>
 *     <li>用户：单用户演示场景，固定 {@link AgentConstants#DEFAULT_USER_ID}，不依赖 UserContext</li>
 *     <li>实体字段：按规范补全 createTime / updateTime / creater / updater</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl implements ChatSessionService {

    private final SessionProperties sessionProperties;
    private final ChatSessionRepository chatSessionRepository;

    @Override
    public SessionVO createSession(Integer num) {
        // 1. 配置 -> 首屏 VO（title/describe/examples 来自 application.yml 的 habit.ai.session）
        SessionVO sessionVO = BeanUtil.toBean(sessionProperties, SessionVO.class);

        // 2. 随机抽取 num 条快捷示例（num 超出总数时取全部）
        List<SessionVO.Example> examples = sessionProperties.getExamples();
        int take = (num == null || num <= 0) ? 3 : num;
        sessionVO.setExamples(RandomUtil.randomEleList(examples, Math.min(take, examples.size())));

        // 3. 生成唯一 sessionId
        String sessionId = IdUtil.fastSimpleUUID();
        sessionVO.setSessionId(sessionId);

        // 4. 持久化会话（本地场景持久化、不自动删除，见开发文档 8.4）
        Long userId = AgentConstants.DEFAULT_USER_ID;
        LocalDateTime now = LocalDateTime.now();
        ChatSession chatSession = ChatSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .title(sessionProperties.getTitle())
                .createTime(now)
                .updateTime(now)
                .creater(userId)
                .updater(userId)
                .build();
        chatSessionRepository.save(chatSession);
        log.info("创建会话: sessionId={}, userId={}", sessionId, userId);

        return sessionVO;
    }

    @Override
    public List<SessionVO.HotQuestion> hotQuestions(Integer num) {
        List<SessionVO.HotQuestion> hotQuestions = sessionProperties.getHotQuestions();
        int take = (num == null || num <= 0) ? 3 : num;
        if (hotQuestions == null || hotQuestions.isEmpty()) {
            return List.of();
        }
        return RandomUtil.randomEleList(hotQuestions, Math.min(take, hotQuestions.size()));
    }
}
