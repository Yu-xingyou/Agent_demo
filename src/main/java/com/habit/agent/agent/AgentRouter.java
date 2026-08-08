package com.habit.agent.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 智能体路由器：按 AgentTypeEnum 分发到对应子智能体
 * <p>
 * 注入容器中所有 Agent Bean（HEALTH/ROUTE/SLEEP/DIET/EXERCISE/CHECKIN/KNOWLEDGE），
 * 构建 AgentTypeEnum→Agent 映射；未知类型回退 HEALTH。
 */
@Slf4j
@Component
public class AgentRouter {

    /** 智能体类型到智能体实例的映射表 */
    private final Map<AgentTypeEnum, Agent> agentMap;

    /**
     * 构造路由器，收集容器中全部 Agent Bean 并构建类型映射
     *
     * @param agents 容器中所有实现了 Agent 接口的 Bean（Spring 自动注入）
     */
    public AgentRouter(List<Agent> agents) {
        this.agentMap = agents.stream()
                .collect(Collectors.toMap(Agent::getAgentType, Function.identity()));
        log.info("智能体路由注册完成：{}", agentMap.keySet());
    }

    /**
     * 按类型获取智能体，未知类型回退 HEALTH
     *
     * @param type 目标智能体类型
     * @return 对应的智能体实例；类型未知时返回 HEALTH 兜底智能体
     */
    public Agent get(AgentTypeEnum type) {
        return agentMap.getOrDefault(type, agentMap.get(AgentTypeEnum.HEALTH));
    }
}
