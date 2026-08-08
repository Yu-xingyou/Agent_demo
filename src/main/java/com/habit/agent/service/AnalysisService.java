package com.habit.agent.service;

import java.util.Map;

import com.habit.agent.common.vo.AchievementRateVO;
import com.habit.agent.common.vo.RadarDataVO;
import com.habit.agent.common.vo.TrendDataVO;

/**
 * 习惯分析业务逻辑接口。
 *
 * 聚合内置目标（睡眠/运动/饮水/饮食/心情）与用户自定义目标（CUSTOM）的真实打卡数据。
 */
public interface AnalysisService {

    /**
     * 趋势数据（内置四维度 + 自定义目标动态序列）。
     *
     * @param userId 用户 id，可空（空时使用默认用户）
     * @param days   统计天数窗口
     * @return 趋势数据视图（睡眠/运动/饮水/心情序列 + 自定义维度）
     */
    TrendDataVO getTrend(Long userId, int days);

    /**
     * 综合概览（统计卡 + 达成率摘要 + 雷达）。
     *
     * @param userId 用户 id，可空（空时使用默认用户）
     * @param days   统计天数窗口
     * @return 概览统计键值对
     */
    Map<String, Object> getOverview(Long userId, int days);

    /**
     * 达成率（内置 + 自定义目标动态维度）。
     *
     * @param userId 用户 id，可空（空时使用默认用户）
     * @param days   统计天数窗口
     * @return 各目标达成率与总体达成率视图
     */
    AchievementRateVO getAchievementRate(Long userId, int days);

    /**
     * 雷达数据（内置五维 + 自定义目标动态维度）。
     *
     * @param userId 用户 id，可空（空时使用默认用户）
     * @param days   统计天数窗口
     * @return 五维雷达分值视图
     */
    RadarDataVO getRadar(Long userId, int days);
}
