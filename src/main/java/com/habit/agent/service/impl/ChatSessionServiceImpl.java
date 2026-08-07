package com.habit.agent.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.config.SessionProperties;
import com.habit.agent.entity.mongo.ChatSession;
import com.habit.agent.enums.MessageTypeEnum;
import com.habit.agent.repository.ChatSessionRepository;
import com.habit.agent.service.ChatService;
import com.habit.agent.service.ChatSessionService;
import com.habit.agent.memory.MyAssistantMessage;
import com.habit.agent.vo.MessageVO;
import com.habit.agent.vo.SessionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    /** 会话记忆（MongoDB 实现），历史消息查询的数据来源 */
    private final ChatMemory chatMemory;

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
                // 持久化标题先置空：首屏展示标题由 SessionVO 提供（来自配置），
                // 真正标题在首条消息到达时由 update() 异步生成，避免新建会话无标题
                .title(null)
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

    @Override
    public List<MessageVO> queryBySessionId(String sessionId) {
        // 1. 根据会话 ID 推导对话 ID（与流式写入侧使用同一规则）
        String conversationId = ChatService.getConversationId(sessionId);

        // 2. 从会话记忆（MongoDB）中获取历史消息
        List<Message> messageList = chatMemory.get(conversationId);
        if (messageList == null || messageList.isEmpty()) {
            return List.of();
        }

        // 3. 过滤并转换消息列表
        return messageList.stream()
                // 只保留用户提问与 AI 回答，过滤掉 SYSTEM / TOOL 等非展示类消息
                .filter(message -> message.getMessageType() == MessageType.USER
                        || message.getMessageType() == MessageType.ASSISTANT)
                // 转换为 MessageVO 对象
                .map(message -> {
                    MessageVO.MessageVOBuilder builder = MessageVO.builder()
                            .content(message.getText())
                            .type(MessageTypeEnum.valueOf(message.getMessageType().name()));
                    // 回显工具调用结果：从助手消息 metadata 中提取 params
                    if (message.getMessageType() == MessageType.ASSISTANT) {
                        Map<String, Object> params = MyAssistantMessage.extractParams(message);
                        if (params != null && !params.isEmpty()) {
                            builder.params(params);
                        }
                    }
                    return builder.build();
                })
                .toList();
    }

    /**
     * 异步更新会话标题（首条消息到达时调用）。
     *
     * <p>仅当该会话当前标题为空、且传入 title 非空时才写入，避免覆盖已有标题；
     * 标题统一截断至 100 字，并刷新 updateTime。采用异步执行，不阻塞对话主流程。</p>
     */
    @Async
    @Override
    public void update(String sessionId, String title, Long userId) {
        if (sessionId == null || userId == null || StrUtil.isBlank(title)) {
            return;
        }
        // 定位用户的会话（单用户演示场景 userId 即 DEFAULT_USER_ID）
        ChatSession chatSession = chatSessionRepository.findBySessionIdAndUserId(sessionId, userId);
        if (chatSession == null) {
            log.warn("异步更新标题失败：会话不存在 sessionId={}, userId={}", sessionId, userId);
            return;
        }
        // 仅当标题为空时才用首条消息覆盖，避免覆盖自定义/历史标题
        if (StrUtil.isBlank(chatSession.getTitle())) {
            chatSession.setTitle(StrUtil.sub(title, 0, 100));
        }
        chatSession.setUpdateTime(LocalDateTime.now());
        chatSession.setUpdater(userId);
        chatSessionRepository.save(chatSession);
        log.info("异步更新会话标题：sessionId={}, title={}", sessionId, chatSession.getTitle());
    }
}
