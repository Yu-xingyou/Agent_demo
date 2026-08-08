package com.habit.agent.agent;

import lombok.Getter;

import java.util.Arrays;

/**
 * 智能体类型枚举（7 类）
 * agentName 供 RouteAgent 输出与前端 constants/agent.js 过滤（大写，保持一一对应）
 */
@Getter
public enum AgentTypeEnum {

    /** 主对话智能体（综合健康咨询，默认兜底） */
    HEALTH("HEALTH", "综合健康助手"),
    /** 意图路由智能体（内部使用，不直接对外输出） */
    ROUTE("ROUTE", "意图路由智能体"),
    /** 睡眠维度子智能体 */
    SLEEP("SLEEP", "睡眠顾问"),
    /** 饮食/饮水维度子智能体 */
    DIET("DIET", "饮食顾问"),
    /** 运动维度子智能体 */
    EXERCISE("EXERCISE", "运动顾问"),
    /** 打卡维度子智能体 */
    CHECKIN("CHECKIN", "打卡助手"),
    /** 健康知识科普子智能体 */
    KNOWLEDGE("KNOWLEDGE", "健康知识顾问");

    private final String agentName;
    private final String desc;

    AgentTypeEnum(String agentName, String desc) {
        this.agentName = agentName;
        this.desc = desc;
    }

    /**
     * 根据 agentName 解析枚举，未知返回 HEALTH（默认兜底）
     *
     * @param agentName 智能体名称（大小写不敏感，允许前后空白）
     * @return 匹配到的智能体类型；为 null 或无法匹配时返回 {@link #HEALTH}
     */
    public static AgentTypeEnum of(String agentName) {
        if (agentName == null) {
            return HEALTH;
        }
        return Arrays.stream(values())
                .filter(type -> type.agentName.equalsIgnoreCase(agentName.trim()))
                .findFirst()
                .orElse(HEALTH);
    }
}
