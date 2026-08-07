package com.habit.agent.agent;

import cn.hutool.core.util.EnumUtil;
import lombok.Getter;

/**
 * 智能体类型枚举。
 *
 * <p>标识不同职能的 Agent（如：路由 / 知识讲解 / 咨询等）。
 * 当前项目以「健康习惯助手」为主，{@code HEALTH} 为默认类型，
 * 其余类型作为多 Agent 扩展框架预留（后续模块可按需实现具体 Agent）。</p>
 *
 * <p>参照示例 {@code AgentTypeEnum} 设计，保留按 {@code agentName} 反查的能力。</p>
 */
@Getter
public enum AgentTypeEnum {

    HEALTH("HEALTH", "健康习惯助手"),
    ROUTE("ROUTE", "路由智能体"),
    RECOMMEND("RECOMMEND", "课程推荐智能体"),
    CONSULT("CONSULT", "课程咨询智能体"),
    BUY("BUY", "课程购买智能体"),
    KNOWLEDGE("KNOWLEDGE", "知识讲解智能体");

    private final String agentName;
    private final String desc;

    AgentTypeEnum(String agentName, String desc) {
        this.agentName = agentName;
        this.desc = desc;
    }

    @Override
    public String toString() {
        return this.name();
    }

    /**
     * 通过智能体的名称查找枚举。
     *
     * @param agentName 名称（如 "HEALTH"）
     * @return 对应的枚举值，未匹配时返回 null
     */
    public static AgentTypeEnum agentNameOf(String agentName) {
        return EnumUtil.getBy(AgentTypeEnum::getAgentName, agentName);
    }
}
