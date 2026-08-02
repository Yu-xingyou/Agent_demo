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
import java.time.LocalTime;

/**
 * 习惯记录视图对象（子模块 2-2）
 *
 * 用于 API 响应，屏蔽实体层的 @PrePersist/@PreUpdate 等持久化逻辑。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class HabitRecordVO {

    @Schema(description = "记录ID", example = "1")
    private Long id;

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "记录日期", example = "2026-08-02")
    private LocalDate recordDate;

    @Schema(description = "入睡时间", example = "23:00")
    private LocalTime sleepTime;

    @Schema(description = "起床时间", example = "07:00")
    private LocalTime wakeTime;

    @Schema(description = "睡眠时长(小时)", example = "8.0")
    private BigDecimal sleepDuration;

    @Schema(description = "睡眠质量(1-5)", example = "4")
    private Integer sleepQuality;

    @Schema(description = "饮食描述", example = "清淡")
    private String dietDesc;

    @Schema(description = "饮食评分(1-5)", example = "4")
    private Integer dietScore;

    @Schema(description = "运动类型", example = "RUN")
    private String exerciseType;

    @Schema(description = "运动时长(分钟)", example = "30")
    private Integer exerciseDuration;

    @Schema(description = "饮水摄入量(毫升)", example = "1500")
    private Integer waterIntake;

    @Schema(description = "心情(1-5)", example = "4")
    private Integer mood;

    @Schema(description = "备注", example = "今日状态良好")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
