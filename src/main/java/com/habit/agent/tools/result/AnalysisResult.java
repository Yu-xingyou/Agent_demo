package com.habit.agent.tools.result;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 习惯分析工具返回结果（对应 PRD T09）。
 *
 * <p>不同类型分析返回不同结构：结构化分析（TREND/OVERVIEW/ACHIEVEMENT/RADAR）以
 * {@code data} 承载原始 VO，AI_SUMMARY 以 {@code summary} 承载自然语言周报。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResult {

    @JsonPropertyDescription("分析类型：TREND/OVERVIEW/ACHIEVEMENT/RADAR/AI_SUMMARY")
    private String type;
    @JsonPropertyDescription("自然语言周报（仅 AI_SUMMARY 类型有值）")
    private String summary;
    @JsonPropertyDescription("结构化分析数据（非 AI_SUMMARY 类型有值）")
    private Map<String, Object> data;
}
