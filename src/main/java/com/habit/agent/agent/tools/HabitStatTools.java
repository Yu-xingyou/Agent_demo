package com.habit.agent.agent.tools;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.service.AnalysisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 习惯统计分析工具（阶段六：Tool Calling 与业务整合）。
 *
 * <p>把趋势概览、达成率等"只读"分析能力暴露给 AI，便于助手在对话中
 * 解读用户近况。当前 demo 为单用户，固定使用 {@link AgentConstants#DEFAULT_USER_ID}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HabitStatTools {

    private final AnalysisService analysisService;

    @Tool(description = "获取用户近 N 天的分析概览：打卡天数、平均睡眠(小时)、平均运动(分钟)、平均饮水(ml)、平均心情(1-5)、平均饮食评分(1-5)")
    public String getOverview(
            @ToolParam(description = "统计天数，例如 7 表示最近一周") int days) {
        try {
            Map<String, Object> overview = analysisService.getOverview(AgentConstants.DEFAULT_USER_ID, days);
            StringBuilder sb = new StringBuilder();
            sb.append("近 ").append(days).append(" 天概览：\n");
            sb.append("- 打卡天数：").append(overview.get("checkedDays")).append("/").append(days).append("\n");
            appendAvg(sb, "平均睡眠时长", overview.get("avgSleep"), "小时");
            appendAvg(sb, "平均运动时长", overview.get("avgExercise"), "分钟");
            appendAvg(sb, "平均饮水量", overview.get("avgWater"), "ml");
            appendAvg(sb, "平均心情", overview.get("avgMood"), "分(1-5)");
            appendAvg(sb, "平均饮食评分", overview.get("avgDiet"), "分(1-5)");
            return sb.toString();
        } catch (Exception e) {
            log.warn("[HabitStatTools] 查询概览失败: {}", e.getMessage());
            return "查询分析概览时发生错误：" + e.getMessage();
        }
    }

    @Tool(description = "获取用户各习惯维度的近期达成率（百分比），用于判断哪些习惯坚持得好、哪些需要加强")
    public String getAchievementRate(
            @ToolParam(description = "统计天数，例如 30 表示最近一月") int days) {
        try {
            var vo = analysisService.getAchievementRate(AgentConstants.DEFAULT_USER_ID, days);
            StringBuilder sb = new StringBuilder();
            sb.append("近 ").append(days).append(" 天各维度达成率：\n");
            if (vo.getDimensions() != null) {
                for (var d : vo.getDimensions()) {
                    sb.append("- ").append(d.getDimension()).append("：")
                            .append(d.getRate() == null ? "无数据" : d.getRate()).append("%\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("[HabitStatTools] 查询达成率失败: {}", e.getMessage());
            return "查询达成率时发生错误：" + e.getMessage();
        }
    }

    private void appendAvg(StringBuilder sb, String label, Object value, String unit) {
        if (value == null) {
            sb.append("- ").append(label).append("：无数据\n");
            return;
        }
        if (value instanceof BigDecimal bd) {
            sb.append("- ").append(label).append("：").append(bd.stripTrailingZeros().toPlainString())
                    .append(unit).append("\n");
        } else {
            sb.append("- ").append(label).append("：").append(value).append(unit).append("\n");
        }
    }
}
