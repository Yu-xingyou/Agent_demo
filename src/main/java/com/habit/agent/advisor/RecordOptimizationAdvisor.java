package com.habit.agent.advisor;

import com.habit.agent.agent.AgentTypeEnum;
import com.habit.agent.memory.MyChatMemoryRepository;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;

/**
 * 记录优化 Advisor。
 *
 * <p>参照天机 aigc 示例 {@code com.tianji.aigc.advisor.RecordOptimizationAdvisor} 设计，
 * 适配本项目（MongoDB 记忆仓库而非 Redis）。</p>
 *
 * <p><b>职责</b>：监控大模型生成的响应内容。如果检测到输出内容恰好是某个智能体的名称
 * （例如 {@code SLEEP}、{@code DIET}、{@code ROUTE} 等，即路由智能体内部转发的分类结果），
 * 说明这条 ASSISTANT 消息属于<b>内部实现</b>而非面向用户的回答，不应被用户看到，
 * 因此从记忆仓库中移除最近写入的两条记录（用户提问 + 该内部转发消息），保持历史记录干净。</p>
 *
 * <p><b>执行顺序</b>：本 Advisor 的 {@code order} 必须比 {@code MessageChatMemoryAdvisor} 更小，
 * 即排在它<b>之前</b>执行。这样可保证「大模型生成响应 → 记忆 Advisor 落库」之后，
 * 再由本 Advisor 进行清理，否则清理会发生在落库之前而失效。</p>
 */
public class RecordOptimizationAdvisor implements BaseAdvisor {

    private final MyChatMemoryRepository myChatMemoryRepository;

    public RecordOptimizationAdvisor(MyChatMemoryRepository myChatMemoryRepository) {
        this.myChatMemoryRepository = myChatMemoryRepository;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        return chatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        // 获取大模型的响应内容
        var chatResponse = chatClientResponse.chatResponse();
        // 获取大模型的响应内容，判断内容是否是智能体的名称，如果是，优化记录，否则无需优化
        assert chatResponse != null;
        var text = chatResponse.getResult().getOutput().getText();
        var agentType = AgentTypeEnum.agentNameOf(text);
        if (null != agentType) {
            // 需要优化记录：该消息是路由智能体内部转发的分类结果，不属于用户可见回答
            var conversationId = (String) chatClientResponse.context().get(ChatMemory.CONVERSATION_ID);
            this.myChatMemoryRepository.optimization(conversationId);
        }

        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER - 100;
    }
}
