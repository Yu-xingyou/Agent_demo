package com.habit.agent.agent;

import cn.hutool.core.date.DateUtil;
import com.habit.agent.config.SystemPromptConfig;
import com.habit.agent.service.ChatSessionService;
import com.habit.agent.tools.CommonTools;
import com.habit.agent.tools.GoalTools;
import com.habit.agent.tools.HabitTools;
import com.habit.agent.tools.ReminderTools;
import com.habit.agent.tools.SessionTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 习惯打卡子智能体（{@code CHECKIN}）。
 *
 * <p>综合处理用户的习惯打卡操作请求：复用基类 RAG 检索增强作为科普辅助，
 * 挂载最全的工具集（打卡/目标/提醒/会话历史/日期），由系统提示词约束模型
 * 聚焦打卡相关写操作并准确调用工具。其余能力继承自 {@link AbstractAgent}。</p>
 */
@Component
public class CheckinAgent extends AbstractAgent {

    private final SystemPromptConfig systemPromptConfig;
    private final HabitTools habitTools;
    private final GoalTools goalTools;
    private final ReminderTools reminderTools;
    private final SessionTools sessionTools;
    private final CommonTools commonTools;

    public CheckinAgent(ChatSessionService chatSessionService,
                        ChatClient chatClient,
                        ChatMemory chatMemory,
                        SystemPromptConfig systemPromptConfig,
                        VectorStore vectorStore,
                        HabitTools habitTools,
                        GoalTools goalTools,
                        ReminderTools reminderTools,
                        SessionTools sessionTools,
                        CommonTools commonTools) {
        super(chatSessionService, chatClient, chatMemory, systemPromptConfig, vectorStore);
        this.systemPromptConfig = systemPromptConfig;
        this.habitTools = habitTools;
        this.goalTools = goalTools;
        this.reminderTools = reminderTools;
        this.sessionTools = sessionTools;
        this.commonTools = commonTools;
    }

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.CHECKIN;
    }

    @Override
    public String systemMessage() {
        return systemPromptConfig.getCheckinAgentSystemMessage().get();
    }

    @Override
    public Object[] tools() {
        return new Object[]{habitTools, goalTools, reminderTools, sessionTools, commonTools};
    }

    @Override
    public Map<String, Object> systemMessageParams() {
        return Map.of("now", DateUtil.now());
    }
}
