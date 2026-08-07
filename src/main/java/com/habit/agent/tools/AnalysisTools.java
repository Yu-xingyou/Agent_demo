package com.habit.agent.tools;

import java.util.Map;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.common.vo.AchievementRateVO;
import com.habit.agent.common.vo.RadarDataVO;
import com.habit.agent.common.vo.TrendDataVO;
import com.habit.agent.service.AnalysisService;
import com.habit.agent.tools.constant.ToolConstants;
import com.habit.agent.tools.result.AnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 习惯分析工具（对应 PRD 第 10 节 T09 analyze_habit_trend）。
 *
 * <p>结构化分析（TREND/OVERVIEW/ACHIEVEMENT/RADAR）直接返回 {@code AnalysisService} 数据；
 * AI_SUMMARY 基于结构化指标生成纯文本周报（非 LLM 生成，待接入 LLM 摘要能力后升级）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisTools {

    private final AnalysisService analysisService;

    @Tool(description = ToolConstants.Tools.ANALYZE_HABIT_TREND)
    public AnalysisResult analyzeHabitTrend(
            @ToolParam(description = ToolConstants.ToolParams.ANALYSIS_TYPE, required = false) String type,
            @ToolParam(description = ToolConstants.ToolParams.DAYS, required = false) Integer days) {

        int d = (days != null && days > 0) ? days : 7;
        String t = (type == null || type.isBlank()) ? "AI_SUMMARY" : type.trim().toUpperCase();
        Long userId = AgentConstants.DEFAULT_USER_ID;
        log.info("Agent 分析习惯: type={}, days={}", t, d);

        return switch (t) {
            case "TREND" -> AnalysisResult.builder()
                    .type(t).data(toMap(analysisService.getTrend(userId, d))).build();
            case "OVERVIEW" -> AnalysisResult.builder()
                    .type(t).data(analysisService.getOverview(userId, d)).build();
            case "ACHIEVEMENT" -> AnalysisResult.builder()
                    .type(t).data(toMap(analysisService.getAchievementRate(userId, d))).build();
            case "RADAR" -> AnalysisResult.builder()
                    .type(t).data(toMap(analysisService.getRadar(userId, d))).build();
            default -> AnalysisResult.builder()
                    .type("AI_SUMMARY")
                    .summary(buildSummary(userId, d))
                    .build();
        };
    }

    private String buildSummary(Long userId, int days) {
        // TODO: 接入 LLM 摘要能力（如 AiAnalysisService）后将改为模型生成的自然语言周报
        TrendDataVO trend = analysisService.getTrend(userId, days);
        AchievementRateVO achievement = analysisService.getAchievementRate(userId, days);
        RadarDataVO radar = analysisService.getRadar(userId, days);
        StringBuilder sb = new StringBuilder();
        sb.append("近").append(days).append("天习惯分析：");
        if (achievement != null && achievement.getOverallRate() != null) {
            sb.append("总体达成率 ").append(achievement.getOverallRate()).append("%；");
        }
        if (radar != null && radar.getIndicators() != null) {
            sb.append("雷达维度 ").append(radar.getIndicators().size()).append(" 项；");
        }
        if (trend != null) {
            sb.append("趋势数据点 ").append(trend.getDates() != null ? trend.getDates().size() : 0).append(" 个。");
        }
        sb.append("（详请查看趋势/达成率/雷达数据）");
        return sb.toString();
    }

    private Map<String, Object> toMap(Object obj) {
        // 借助 Spring 的 Bean 映射；这里用最简 JSON 序列化避免额外依赖
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            return mapper.convertValue(obj, Map.class);
        } catch (Exception e) {
            return Map.of("raw", obj.toString());
        }
    }
}
