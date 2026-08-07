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
 * 饮食建议子智能体（{@code DIET}）。
 *
 * <p>聚焦饮食与饮水维度的健康建议：复用基类 RAG 检索增强获取科普支撑，
 * 挂载打卡/目标/日期工具以查询与记录饮食、饮水相关数据，由系统提示词约束
 * 模型仅处理饮食维度。其余能力继承自 {@link AbstractAgent}。</p>
 */
@Component
public class DietAgent extends AbstractAgent {

    private final SystemPromptConfig systemPromptConfig;
    private final HabitTools habitTools;
    private final GoalTools goalTools;
    private final CommonTools commonTools;

    public DietAgent(ChatSessionService chatSessionService,
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
        return AgentTypeEnum.DIET;
    }

    @Override
    public String systemMessage() {
        return systemPromptConfig.getDietAgentSystemMessage().get();
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
