package com.habit.agent.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 习惯目标视图对象
 *
 * 支持内置类型(SLEEP/EXERCISE/WATER/DIET)和自定义类型(CUSTOM + customName)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class HabitGoalVO {

    @Schema(description = "目标ID", example = "1")
    private Long id;

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @NotBlank(message = "目标类型不能为空")
    @Schema(description = "目标类型(SLEEP/EXERCISE/WATER/DIET/CUSTOM)", example = "EXERCISE")
    private String goalType;

    @Schema(description = "自定义目标名称(CUSTOM 类型使用)", example = "每日阅读")
    private String customName;

    @Schema(description = "显示名(CUSTOM→customName, 其他→goalType)", example = "运动")
    private String displayName;

    @Schema(description = "目标数值", example = "10000")
    private BigDecimal targetValue;

    @Schema(description = "单位", example = "步")
    private String unit;

    @Schema(description = "周期(DAY/WEEK/MONTH)", example = "DAY")
    private String period;

    @Schema(description = "是否启用", example = "true")
    private Boolean isActive;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
