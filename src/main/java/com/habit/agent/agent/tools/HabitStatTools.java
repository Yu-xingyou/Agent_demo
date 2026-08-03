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
 * <p>把趋势概览、达成率、趋势序列、雷达等多维「只读」分析能力暴露给 AI，
 * 便于助手在对话中全面解读用户近况。当前 demo 为单用户，固定使用
 * {@link AgentConstants#DEFAULT_USER_ID}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HabitStatTools {

    private final AnalysisService analysisService;

    @Tool(description = "获取用户近 N 天的综合概览（打卡天数、平均睡眠小时、平均运动分钟、平均饮水ml、平均心情1-5、平均饮食评分1-5）。"
            + "当用户问「我最近状态怎样」「整体怎么样」「平均睡多久」时调用。")
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

    @Tool(description = "获取用户各习惯维度近期达成率（百分比），判断哪些习惯坚持得好、哪些需加强。"
            + "当用户问「达成率」「我哪方面做得好」「坚持得如何」时调用。")
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

    @Tool(description = "获取用户近 N 天各习惯维度的逐日趋势序列（用于观察走向、波动）。"
            + "当用户问「最近趋势」「有没有进步」「变化大不大」「走势」时调用。")
    public String getTrend(
            @ToolParam(description = "统计天数，例如 14 表示最近两周") int days) {
        try {
            var vo = analysisService.getTrend(AgentConstants.DEFAULT_USER_ID, days);
            StringBuilder sb = new StringBuilder();
            sb.append("近 ").append(days).append(" 天趋势：\n");
            sb.append("- 日期：").append(vo.getDates()).append("\n");
            sb.append("- 睡眠(小时)：").append(vo.getSleep()).append("\n");
            sb.append("- 运动(分钟)：").append(vo.getExercise()).append("\n");
            sb.append("- 饮水(ml)：").append(vo.getWater()).append("\n");
            sb.append("- 心情(1-5)：").append(vo.getMood()).append("\n");
            sb.append("- 饮食(1-5)：").append(vo.getDiet()).append("\n");
            if (vo.getCustomSeries() != null) {
                for (var cs : vo.getCustomSeries()) {
                    sb.append("- 自定义[").append(cs.getName()).append("]：").append(cs.getData()).append("\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("[HabitStatTools] 查询趋势失败: {}", e.getMessage());
            return "查询趋势时发生错误：" + e.getMessage();
        }
    }

    @Tool(description = "获取用户近 N 天的多维雷达数据（内置五维+自定义目标），用于综合对比强弱项。"
            + "当用户问「我的强弱项」「各方面对比」「给我画个雷达」时调用。")
    public String getRadar(
            @ToolParam(description = "统计天数，例如 30 表示最近一月") int days) {
        try {
            var vo = analysisService.getRadar(AgentConstants.DEFAULT_USER_ID, days);
            StringBuilder sb = new StringBuilder();
            sb.append("近 ").append(days).append(" 天雷达维度（实际值/目标值）：\n");
            if (vo.getIndicators() != null && vo.getValues() != null) {
                var indicators = vo.getIndicators();
                var values = vo.getValues();
                var targets = vo.getTargets();
                for (int i = 0; i < indicators.size(); i++) {
                    String name = indicators.get(i).getName();
                    Double value = i < values.size() ? values.get(i) : null;
                    Double target = (targets != null && i < targets.size()) ? targets.get(i) : null;
                    sb.append("- ").append(name).append("：实际=")
                            .append(value == null ? "无数据" : value)
                            .append(" 目标=").append(target == null ? "—" : target).append("\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("[HabitStatTools] 查询雷达失败: {}", e.getMessage());
            return "查询雷达数据时发生错误：" + e.getMessage();
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
