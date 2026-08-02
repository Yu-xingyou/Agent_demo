package com.habit.agent.common.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 五维雷达图数据（睡眠/运动/饮水/饮食/心情，0-100 分值）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadarDataVO {

    /** 维度名称列表 */
    private List<String> indicators;

    /** 各维度分值（0-100，与 indicators 一一对应） */
    private List<Integer> values;

    /** 各维度目标分值（满分为 100，用于参考线） */
    private List<Integer> targets;
}
