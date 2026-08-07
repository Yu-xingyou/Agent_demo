package com.habit.agent.tools;

import java.math.BigDecimal;
import java.util.List;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.common.vo.HabitGoalVO;
import com.habit.agent.service.GoalService;
import com.habit.agent.tools.constant.ToolConstants;
import com.habit.agent.tools.result.HabitGoalResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 生活习惯目标相关工具（对应 PRD 第 10 节 T04 list_active_goals / T05 set_goal / T06 delete_goal）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoalTools {

    private final GoalService goalService;

    /**
     * 查询用户当前已启用的目标（含本周完成度）。
     */
    @Tool(description = ToolConstants.Tools.LIST_ACTIVE_GOALS)
    public List<HabitGoalResult> listActiveGoals() {
        Long userId = AgentConstants.DEFAULT_USER_ID;
        log.info("Agent 查询启用目标: userId={}", userId);
        return goalService.getActiveGoalsWithCustom(userId)
                .stream()
                .map(HabitGoalResult::of)
                .toList();
    }

    /**
     * 创建或更新目标。
     *
     * <p>内置类型（SLEEP/EXERCISE/WATER/DIET）同类型唯一，已存在时按 goalId 更新；
     * 未传 goalId 但已存在同类型则抛错，需先查询获得 id。CUSTOM 允许多个。</p>
     */
    @Tool(description = ToolConstants.Tools.SET_GOAL)
    public HabitGoalResult setGoal(
            @ToolParam(description = ToolConstants.ToolParams.GOAL_TYPE) String goalType,
            @ToolParam(description = ToolConstants.ToolParams.TARGET_VALUE, required = false) BigDecimal targetValue,
            @ToolParam(description = ToolConstants.ToolParams.UNIT, required = false) String unit,
            @ToolParam(description = ToolConstants.ToolParams.CUSTOM_NAME, required = false) String customName,
            @ToolParam(description = ToolConstants.ToolParams.GOAL_ID, required = false) Long goalId) {

        HabitGoalVO vo = HabitGoalVO.builder()
                .userId(AgentConstants.DEFAULT_USER_ID)
                .goalType(goalType)
                .targetValue(targetValue)
                .unit(unit)
                .customName(customName)
                .build();

        HabitGoalResult result;
        if (goalId != null) {
            log.info("Agent 更新目标: id={}, type={}", goalId, goalType);
            result = HabitGoalResult.of(goalService.updateGoal(goalId, vo));
        } else {
            log.info("Agent 创建目标: type={}, customName={}", goalType, customName);
            result = HabitGoalResult.of(goalService.saveGoal(vo));
        }
        return result;
    }

    /**
     * 删除目标（级联删除关联打卡）。
     */
    @Tool(description = ToolConstants.Tools.DELETE_GOAL)
    public String deleteGoal(
            @ToolParam(description = ToolConstants.ToolParams.GOAL_ID) Long goalId) {
        log.info("Agent 删除目标: id={}", goalId);
        goalService.deleteGoal(goalId);
        return "目标已删除: id=" + goalId;
    }
}
