package com.habit.agent.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 自定义目标打卡记录视图对象
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class HabitGoalRecordVO {

    @Schema(description = "打卡记录ID", example = "1")
    private Long id;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @NotNull(message = "目标ID不能为空")
    @Schema(description = "关联目标ID", example = "1")
    private Long goalId;

    @Schema(description = "目标类型", example = "CUSTOM")
    private String goalType;

    @NotNull(message = "打卡日期不能为空")
    @Schema(description = "打卡日期", example = "2026-08-02")
    private LocalDate recordDate;

    @Schema(description = "打卡数值", example = "10000")
    private BigDecimal value;

    @Schema(description = "备注", example = "完成")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
