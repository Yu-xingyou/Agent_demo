package com.habit.agent.common.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 雷达图数据（内置五维 + 自定义目标动态维度）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadarDataVO {

    /** 维度描述列表（名称 + 最大值） */
    private List<Indicator> indicators;

    /** 各维度实际分值（与 indicators 一一对应） */
    private List<Double> values;

    /** 各维度目标分值（与 indicators 一一对应，用于参考线） */
    private List<Double> targets;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Indicator {
        /** 维度名称 */
        private String name;
        /** 维度最大值 */
        private Double max;
    }
}
