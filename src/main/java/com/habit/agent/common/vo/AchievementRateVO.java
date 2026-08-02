package com.habit.agent.common.vo;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 目标达成率
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementRateVO {

    /** 各维度达成率 */
    @lombok.Builder.Default
    private List<DimensionRate> dimensions = java.util.Collections.emptyList();

    /** 总体达成率（0-100） */
    private BigDecimal overallRate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionRate {
        /** 维度名称：SLEEP / EXERCISE / WATER / DIET */
        private String dimension;
        /** 维度中文名 */
        private String label;
        /** 目标值 */
        private BigDecimal target;
        /** 实际均值 */
        private BigDecimal actual;
        /** 达成率（0-100，可超过 100） */
        private BigDecimal rate;
    }
}
