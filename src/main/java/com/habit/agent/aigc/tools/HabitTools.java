package com.habit.agent.aigc.tools;

import com.habit.agent.aigc.Constant.Constant;
import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.common.vo.AchievementRateVO;
import com.habit.agent.common.vo.HabitGoalVO;
import com.habit.agent.common.vo.HabitRecordVO;
import com.habit.agent.common.vo.TrendDataVO;
import com.habit.agent.entity.jpa.Reminder;
import com.habit.agent.service.AnalysisService;
import com.habit.agent.service.GoalService;
import com.habit.agent.service.HabitService;
import com.habit.agent.service.ReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 生活习惯业务工具
 * 注入现有业务 Service，供大模型查询真实习惯数据
 */
@Component
@RequiredArgsConstructor
public class HabitTools {

    private final HabitService habitService;
    private final GoalService goalService;
    private final ReminderService reminderService;
    private final AnalysisService analysisService;

    /**
     * 查询用户最近 N 天的习惯打卡记录（睡眠/饮食/运动/饮水/心情等）
     *
     * @param days 查询天数（最近 N 天），为 null 或非法值时默认 7
     * @return 打卡记录视图列表
     */
    @Tool(description = Constant.Tools.QUERY_RECENT_RECORDS)
    public List<HabitRecordVO> queryRecentRecords(
            @ToolParam(description = Constant.ToolParams.DAYS) Integer days) {
        int d = (days == null || days <= 0) ? 7 : days;
        return habitService.getRecentRecords(AgentConstants.DEFAULT_USER_ID, d);
    }

    /**
     * 查询用户当前启用的习惯目标（内置默认目标 + 用户自定义目标）
     *
     * @return 启用中的习惯目标视图列表
     */
    @Tool(description = Constant.Tools.QUERY_ACTIVE_GOALS)
    public List<HabitGoalVO> queryActiveGoals() {
        return goalService.getActiveGoalsWithCustom(AgentConstants.DEFAULT_USER_ID);
    }

    /**
     * 查询用户最近 N 天各类目标的达成率统计
     *
     * @param days 统计天数（最近 N 天），为 null 或非法值时默认 7
     * @return 各类型目标达成率统计视图
     */
    @Tool(description = Constant.Tools.QUERY_ACHIEVEMENT_RATE)
    public AchievementRateVO queryAchievementRate(
            @ToolParam(description = Constant.ToolParams.DAYS) Integer days) {
        int d = (days == null || days <= 0) ? 7 : days;
        return analysisService.getAchievementRate(AgentConstants.DEFAULT_USER_ID, d);
    }

    /**
     * 查询用户当前的打卡提醒列表
     *
     * @return 打卡提醒实体列表
     */
    @Tool(description = Constant.Tools.QUERY_REMINDERS)
    public List<Reminder> queryReminders() {
        return reminderService.list(AgentConstants.DEFAULT_USER_ID);
    }

    /**
     * 查询用户最近 N 天的习惯数据趋势（睡眠时长/运动/饮水/心情等）
     *
     * @param days 查询天数（最近 N 天），为 null 或非法值时默认 7
     * @return 习惯数据趋势视图
     */
    @Tool(description = Constant.Tools.QUERY_TRENDS)
    public TrendDataVO queryTrends(
            @ToolParam(description = Constant.ToolParams.DAYS) Integer days) {
        int d = (days == null || days <= 0) ? 7 : days;
        return analysisService.getTrend(AgentConstants.DEFAULT_USER_ID, d);
    }

    /**
     * 查询用户最近 N 天的习惯数据综合概览统计
     *
     * @param days 查询天数（最近 N 天），为 null 或非法值时默认 7
     * @return 综合概览统计键值对
     */
    @Tool(description = Constant.Tools.QUERY_OVERVIEW)
    public Map<String, Object> queryOverview(
            @ToolParam(description = Constant.ToolParams.DAYS) Integer days) {
        int d = (days == null || days <= 0) ? 7 : days;
        return analysisService.getOverview(AgentConstants.DEFAULT_USER_ID, d);
    }
}
