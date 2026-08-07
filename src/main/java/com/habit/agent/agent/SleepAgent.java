package com.habit.agent.agent;

import cn.hutool.core.date.DateUtil;
import com.habit.agent.config.SystemPromptConfig;
import com.habit.agent.service.ChatSessionService;
import com.habit.agent.tools.CommonTools;
import com.habit.agent.tools.GoalTools;
import com.habit.agent.tools.HabitTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 睡眠建议子智能体（{@code SLEEP}）。
 *
 * <p>聚焦睡眠与作息维度的健康建议：复用基类 RAG 检索增强获取科普支撑，
 * 挂载打卡/目标/日期工具以查询与记录睡眠相关数据，由系统提示词约束
 * 模型仅处理睡眠维度。其余流式对话、会话标题等能力均继承自 {@link AbstractAgent}。</p>
 */
@Component
public class SleepAgent extends AbstractAgent {

    private final SystemPromptConfig systemPromptConfig;
    private final HabitTools habitTools;
    private final GoalTools goalTools;
    private final CommonTools commonTools;

    public SleepAgent(ChatSessionService chatSessionService,
                      ChatClient chatClient,
                      ChatMemory chatMemory,
                      SystemPromptConfig systemPromptConfig,
                      VectorStore vectorStore,
                      HabitTools habitTools,
                      GoalTools goalTools,
                      CommonTools commonTools) {
        super(chatSessionService, chatClient, chatMemory, systemPromptConfig, vectorStore);
        this.systemPromptConfig = systemPromptConfig;
        this.habitTools = habitTools;
        this.goalTools = goalTools;
        this.commonTools = commonTools;
    }

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.SLEEP;
    }

    @Override
    public String systemMessage() {
        return systemPromptConfig.getSleepAgentSystemMessage().get();
    }

    @Override
    public Object[] tools() {
        return new Object[]{habitTools, goalTools, commonTools};
    }

    @Override
    public Map<String, Object> systemMessageParams() {
        return Map.of("now", DateUtil.now());
    }
}
