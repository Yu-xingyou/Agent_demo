package com.habit.agent.common.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 习惯目标视图对象（子模块 2-2）
 *
 * 用于 API 响应，goalType 和 period 以字符串返回便于前端直接展示。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class HabitGoalVO {

    private Long id;
    private Long userId;
    private String goalType;
    private BigDecimal targetValue;
    private String unit;
    private String period;
    private Boolean isActive;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
