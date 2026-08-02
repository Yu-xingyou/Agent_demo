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

    private Long id;
    private Long userId;
    private Long goalId;
    private String goalType;
    private LocalDate recordDate;
    private BigDecimal value;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
