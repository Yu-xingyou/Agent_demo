package com.habit.agent.common.vo;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 趋势数据（近 N 天睡眠/运动/饮水）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendDataVO {

    /** 日期列表（YYYY-MM-DD，升序） */
    private List<String> dates;

    /** 睡眠时长（小时），与 dates 一一对应 */
    private List<BigDecimal> sleep;

    /** 运动时长（分钟） */
    private List<BigDecimal> exercise;

    /** 饮水量（ml） */
    private List<BigDecimal> water;

    /** 心情评分（1-5） */
    private List<Integer> mood;

    /** 饮食评分（1-5），与 dates 一一对应 */
    private List<Integer> diet;

    /** 自定义目标趋势序列（动态维度，按用户实际自定义目标生成） */
    private List<CustomGoalSeriesVO> customSeries;
}
