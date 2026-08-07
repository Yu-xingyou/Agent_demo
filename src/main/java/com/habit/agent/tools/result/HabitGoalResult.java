package com.habit.agent.tools.result;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.habit.agent.common.vo.HabitGoalVO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 目标工具返回结果（对应 PRD T04/T05/T06）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HabitGoalResult {

    @JsonPropertyDescription("目标id")
    private Long id;
    @JsonPropertyDescription("目标类型：SLEEP/EXERCISE/WATER/DIET/CUSTOM")
    private String goalType;
    @JsonPropertyDescription("自定义目标名称（CUSTOM 类型）")
    private String customName;
    @JsonPropertyDescription("显示名，例如 运动目标")
    private String displayName;
    @JsonPropertyDescription("目标数值")
    private BigDecimal targetValue;
    @JsonPropertyDescription("单位，例如 h/min/ml")
    private String unit;
    @JsonPropertyDescription("周期：DAY/WEEK/MONTH")
    private String period;
    @JsonPropertyDescription("是否启用")
    private Boolean isActive;
    @JsonPropertyDescription("本周当前累计值")
    private BigDecimal currentValue;
    @JsonPropertyDescription("本周完成度百分比（0-100+）")
    private BigDecimal weeklyAchievement;

    public static HabitGoalResult of(HabitGoalVO vo) {
        if (vo == null) {
            return null;
        }
        return HabitGoalResult.builder()
                .id(vo.getId())
                .goalType(vo.getGoalType())
                .customName(vo.getCustomName())
                .displayName(vo.getDisplayName())
                .targetValue(vo.getTargetValue())
                .unit(vo.getUnit())
                .period(vo.getPeriod())
                .isActive(vo.getIsActive())
                .currentValue(vo.getCurrentValue())
                .weeklyAchievement(vo.getWeeklyAchievement())
                .build();
    }
}
