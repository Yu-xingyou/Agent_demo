package com.habit.agent.common.vo;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分析概览（近 N 天平均值与打卡天数）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisOverviewVO {

    /** 统计天数 */
    private Integer days;

    /** 打卡天数 */
    private Integer checkedDays;

    /** 平均睡眠时长（小时） */
    private BigDecimal avgSleep;

    /** 平均运动时长（分钟） */
    private BigDecimal avgExercise;

    /** 平均饮水量（ml） */
    private BigDecimal avgWater;

    /** 平均心情（1-5） */
    private BigDecimal avgMood;

    /** 平均饮食评分（1-5） */
    private BigDecimal avgDiet;
}
