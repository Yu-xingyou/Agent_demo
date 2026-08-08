package com.habit.agent.agent;

import com.habit.agent.aigc.Constant.Constant;
import com.habit.agent.aigc.config.SystemPromptConfig;
import com.habit.agent.aigc.enums.ChatEventTypeEnum;
import com.habit.agent.aigc.vo.ChatEventVO;
import com.habit.agent.common.constant.AgentConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 智能体抽象基类
 * <p>
 * 提供公共流式对话能力（基于 ChatClient stream().chatResponse() 模式）：
 * system 提示词 + MessageChatMemoryAdvisor 会话记忆 + toolContext 请求 id + 逐字 DATA 事件输出。
 * 单用户场景：conversationId = "1_" + sessionId（DEFAULT_USER_ID=1）。
 * 工具统一由 ChatClient 的 defaultTools(HabitTools) 提供，子 Agent 通过提示词约束维度。
 */
@Slf4j
public abstract class AbstractAgent implements Agent {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    protected final ChatClient chatClient;
    protected final ChatMemory chatMemory;
    protected final SystemPromptConfig systemPromptConfig;

    protected AbstractAgent(ChatClient chatClient, ChatMemory chatMemory, SystemPromptConfig systemPromptConfig) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.systemPromptConfig = systemPromptConfig;
    }

    /**
     * 系统提示词参数默认注入当前时间 {now}
     */
    @Override
    public Map<String, Object> systemMessageParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("now", LocalDateTime.now().format(TIME_FORMATTER));
        return params;
    }

    /**
     * 执行一次流式对话（SSE 数据事件流，参照 example ChatServiceImpl）
     */
    @Override
    public Flux<ChatEventVO> processStream(String question, String sessionId) {
        var requestId = UUID.randomUUID().toString().replace("-", "");
        String conversationId = getConversationId(sessionId);
        return chatClient.prompt()
                .system(promptSpec -> promptSpec.text(systemMessage()).params(systemMessageParams()))
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .toolContext(Map.of(Constant.REQUEST_ID, requestId))
                .user(question)
                .stream()
                .chatResponse()
                .map(chatResponse -> {
                    String text = chatResponse.getResult().getOutput().getText();
                    return ChatEventVO.builder()
                            .eventData(text)
                            .eventType(ChatEventTypeEnum.DATA.getValue())
                            .build();
                });
    }

    /**
     * 会话对话 id 规则（单用户）：用户id_会话id
     */
    protected String getConversationId(String sessionId) {
        return AgentConstants.DEFAULT_USER_ID + "_" + sessionId;
    }
}
