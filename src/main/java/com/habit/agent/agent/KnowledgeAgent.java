package com.habit.agent.agent;

import cn.hutool.core.date.DateUtil;
import com.habit.agent.config.SystemPromptConfig;
import com.habit.agent.service.ChatSessionService;
import com.habit.agent.tools.CommonTools;
import com.habit.agent.tools.KnowledgeTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 健康知识讲解子智能体（{@code KNOWLEDGE}）。
 *
 * <p>专注健康科普问答：复用基类 RAG 检索增强获取科普支撑，
 * 挂载知识检索工具与日期工具，由系统提示词约束模型仅做知识讲解、
 * 不执行任何写操作（不打卡、不设定目标）。其余能力继承自 {@link AbstractAgent}。</p>
 */
@Component
public class KnowledgeAgent extends AbstractAgent {

    private final SystemPromptConfig systemPromptConfig;
    private final KnowledgeTools knowledgeTools;
    private final CommonTools commonTools;

    public KnowledgeAgent(ChatSessionService chatSessionService,
                          ChatClient chatClient,
                          ChatMemory chatMemory,
                          SystemPromptConfig systemPromptConfig,
                          VectorStore vectorStore,
                          KnowledgeTools knowledgeTools,
                          CommonTools commonTools) {
        super(chatSessionService, chatClient, chatMemory, systemPromptConfig, vectorStore);
        this.systemPromptConfig = systemPromptConfig;
        this.knowledgeTools = knowledgeTools;
        this.commonTools = commonTools;
    }

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.KNOWLEDGE;
    }

    @Override
    public String systemMessage() {
        return systemPromptConfig.getKnowledgeAgentSystemMessage().get();
    }

    @Override
    public Object[] tools() {
        return new Object[]{knowledgeTools, commonTools};
    }

    @Override
    public Map<String, Object> systemMessageParams() {
        return Map.of("now", DateUtil.now());
    }
}
