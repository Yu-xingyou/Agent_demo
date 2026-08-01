package com.habit.agent.common.vo;

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

    private Long id;
    private Long userId;
    private LocalDate recordDate;
    private LocalTime sleepTime;
    private LocalTime wakeTime;
    private BigDecimal sleepDuration;
    private Integer sleepQuality;
    private String dietDesc;
    private Integer dietScore;
    private String exerciseType;
    private Integer exerciseDuration;
    private Integer waterIntake;
    private Integer mood;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
