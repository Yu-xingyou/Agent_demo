package com.habit.agent.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 子智能体路由分发骨架（Agent 池）。
 *
 * <p>构建 {@code AgentTypeEnum → Agent} 的映射，供后续按 {@link RouteAgent} 的分类结果
 * 选择具体子 Agent 处理用户请求。</p>
 *
 * <p><b>本期为骨架</b>：仅注入所有业务 Agent 并构建映射，提供 {@link #get(AgentTypeEnum)} 查询。
 * 实际的分发接入（ChatController 依据路由结果选择子 Agent）作为后续项，
 * 本期默认主链路仍走 {@code ChatServiceImpl(HEALTH)}，本类暂不被控制器调用，避免改动爆炸半径。</p>
 *
 * <p>注意：本类<b>不实现</b> {@link Agent} 接口，且 <b>排除</b> {@link RouteAgent} 自身，
 * 以免被 {@code List<Agent>} 注入时自引用或把路由智能体当作可分发业务 Agent。</p>
 */
@Slf4j
@Component
public class AgentRouter {

    /** 类型 → 子智能体 映射（不含 ROUTE 路由智能体本身） */
    private final Map<AgentTypeEnum, Agent> agentMap;

    public AgentRouter(List<Agent> agents) {
        this.agentMap = agents.stream()
                .filter(a -> a.getAgentType() != AgentTypeEnum.ROUTE)
                .collect(Collectors.toMap(
                        Agent::getAgentType,
                        Function.identity(),
                        (a, b) -> {
                            log.warn("存在重复类型的 Agent 实现：{}，保留 {}", a.getAgentType(), a.getClass().getSimpleName());
                            return a;
                        }
                ));
        log.info("子智能体路由池初始化完成，已注册 {} 个业务 Agent：{}",
                agentMap.size(), agentMap.keySet());
    }

    /**
     * 按类型获取子智能体。
     *
     * @param type 智能体类型
     * @return 对应的 Agent 实例，未注册时返回 {@link Optional#empty()}
     */
    public Optional<Agent> get(AgentTypeEnum type) {
        return Optional.ofNullable(agentMap.get(type));
    }
}
