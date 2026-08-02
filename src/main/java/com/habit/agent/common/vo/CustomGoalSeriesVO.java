package com.habit.agent.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

/**
 * 自定义目标趋势序列（动态维度）
 *
 * 每个 CUSTOM 目标对应一条趋势序列，由前端按 id 动态渲染为图表。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CustomGoalSeriesVO {

    @Schema(description = "目标ID", example = "5")
    private Long goalId;

    @Schema(description = "显示名", example = "每日阅读")
    private String name;

    @Schema(description = "渐变起始色（莫兰迪）", example = "#8a7fa0")
    private String colorFrom;

    @Schema(description = "渐变结束色（莫兰迪）", example = "#9a8fb0")
    private String colorTo;

    @Schema(description = "单位", example = "页")
    private String unit;

    @Schema(description = "目标值", example = "20")
    private BigDecimal targetValue;

    @Schema(description = "按日期顺序的数值序列（与 TrendDataVO.dates 对齐，无记录为 null）")
    private List<BigDecimal> data;
}
