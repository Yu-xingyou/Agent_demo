package com.habit.agent.agent;

import com.habit.agent.aigc.vo.ChatEventVO;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 智能体统一契约（子 Agent 实现）
 * <p>
 * 公共流式对话能力由 AbstractAgent 提供，子 Agent 只需声明身份、提示词与参数
 */
public interface Agent {

    /**
     * 获取智能体类型
     */
    AgentTypeEnum getAgentType();

    /**
     * 获取系统提示词文本
     */
    String systemMessage();

    /**
     * 系统提示词参数（如 {now} 时间占位）
     */
    Map<String, Object> systemMessageParams();

    /**
     * 执行一次流式对话，返回 SSE 数据事件流
     *
     * @param question  用户问题
     * @param sessionId 会话 id
     */
    Flux<ChatEventVO> processStream(String question, String sessionId);
}
