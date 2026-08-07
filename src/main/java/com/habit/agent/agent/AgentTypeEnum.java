package com.habit.agent.agent;

import cn.hutool.core.util.EnumUtil;
import lombok.Getter;

/**
 * 智能体类型枚举。
 *
 * <p>标识不同职能的 Agent。当前项目为「生活习惯助手」，以 {@code HEALTH} 为默认类型，
 * 围绕睡眠、饮食、运动等健康习惯场景拆解出更贴合本项目的子智能体类型，
 * 其余类型作为多 Agent 扩展框架预留（后续模块可按需实现具体 Agent）。</p>
 *
 * <p>参照示例 {@code AgentTypeEnum} 设计，保留按 {@code agentName} 反查的能力。</p>
 */
@Getter
public enum AgentTypeEnum {

    HEALTH("HEALTH", "健康习惯助手"),
    ROUTE("ROUTE", "路由智能体"),
    SLEEP("SLEEP", "睡眠建议智能体"),
    DIET("DIET", "饮食建议智能体"),
    EXERCISE("EXERCISE", "运动建议智能体"),
    CHECKIN("CHECKIN", "习惯打卡智能体"),
    KNOWLEDGE("KNOWLEDGE", "健康知识讲解智能体");

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
