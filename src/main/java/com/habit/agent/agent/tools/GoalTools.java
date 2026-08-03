package com.habit.agent.agent.tools;

import java.math.BigDecimal;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.common.vo.HabitGoalVO;
import com.habit.agent.service.GoalService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 习惯目标管理工具（阶段六：Tool Calling 与业务整合）。
 *
 * <p>把目标查询、创建、更新等能力暴露给 AI，让助手可帮用户管理习惯目标。
 * 当前 demo 为单用户，固定使用 {@link AgentConstants#DEFAULT_USER_ID}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoalTools {

    private final GoalService goalService;

    @Tool(description = "查询用户当前所有已启用的习惯目标（含内置类型与自定义目标，含目标数值、单位、周期与本周完成度）")
    public String listActiveGoals() {
        try {
            var goals = goalService.getActiveGoalsWithCustom(AgentConstants.DEFAULT_USER_ID);
            if (goals.isEmpty()) {
                return "用户当前没有启用的习惯目标。";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("共 ").append(goals.size()).append(" 个启用目标：\n");
            for (HabitGoalVO g : goals) {
                sb.append("- ").append(g.getDisplayName())
                        .append("(类型=").append(g.getGoalType()).append(")");
                if (g.getTargetValue() != null) {
                    sb.append(" 目标=").append(g.getTargetValue())
                            .append(g.getUnit() == null ? "" : g.getUnit());
                }
                if (g.getWeeklyAchievement() != null) {
                    sb.append(" 本周完成度=").append(g.getWeeklyAchievement()).append("%");
                }
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("[GoalTools] 查询目标失败: {}", e.getMessage());
            return "查询目标时发生错误：" + e.getMessage();
        }
    }

    @Tool(description = "为用户创建一个自定义习惯目标（如每日阅读、每日冥想），需提供名称、目标数值、单位与周期")
    public String createCustomGoal(
            @ToolParam(description = "自定义目标名称，例如 每日阅读") String name,
            @ToolParam(description = "目标数值，例如 30") BigDecimal targetValue,
            @ToolParam(description = "单位，例如 分钟 / 页 / 次") String unit,
            @ToolParam(description = "周期：DAY(每天) / WEEK(每周) / MONTH(每月)") String period) {
        try {
            HabitGoalVO vo = HabitGoalVO.builder()
                    .userId(AgentConstants.DEFAULT_USER_ID)
                    .goalType("CUSTOM")
                    .customName(name)
                    .targetValue(targetValue)
                    .unit(unit)
                    .period(period)
                    .isActive(true)
                    .build();
            HabitGoalVO saved = goalService.saveGoal(vo);
            return "已创建自定义目标：「" + saved.getDisplayName() + "」(ID=" + saved.getId()
                    + ")，目标 " + saved.getTargetValue() + (saved.getUnit() == null ? "" : saved.getUnit())
                    + " / " + saved.getPeriod() + "。";
        } catch (Exception e) {
            log.warn("[GoalTools] 创建目标失败: {}", e.getMessage());
            return "创建目标时发生错误：" + e.getMessage();
        }
    }

    @Tool(description = "更新指定目标的启用状态（开启或暂停某个习惯目标的追踪）")
    public String toggleGoalActive(
            @ToolParam(description = "目标ID") Long goalId,
            @ToolParam(description = "是否启用：true 启用，false 暂停") Boolean active) {
        try {
            HabitGoalVO existing = goalService.getGoalById(goalId);
            HabitGoalVO vo = HabitGoalVO.builder()
                    .id(goalId)
                    .userId(AgentConstants.DEFAULT_USER_ID)
                    .goalType(existing.getGoalType())
                    .customName(existing.getCustomName())
                    .targetValue(existing.getTargetValue())
                    .unit(existing.getUnit())
                    .period(existing.getPeriod())
                    .isActive(active)
                    .build();
            goalService.updateGoal(goalId, vo);
            return "已将目标「" + existing.getDisplayName() + "」" + (active ? "启用" : "暂停") + "。";
        } catch (Exception e) {
            log.warn("[GoalTools] 更新目标状态失败: {}", e.getMessage());
            return "更新目标状态时发生错误：" + e.getMessage();
        }
    }
}
