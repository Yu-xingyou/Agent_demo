package com.habit.agent.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.habit.agent.common.result.Result;
import com.habit.agent.common.vo.AchievementRateVO;
import com.habit.agent.common.vo.RadarDataVO;
import com.habit.agent.common.vo.TrendDataVO;
import com.habit.agent.service.AnalysisService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 习惯分析接口（阶段九）
 *
 * 提供趋势、概览、达成率、雷达四类分析数据，供前端图表消费。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analysis")
@Tag(name = "习惯分析", description = "趋势/概览/达成率/雷达分析接口")
public class AnalysisController {

    private final AnalysisService analysisService;

    /**
     * 趋势数据
     *
     * @param userId 用户 id，可空（空时使用默认用户）
     * @param days   统计天数窗口，默认 7
     * @return 近 days 天的睡眠时长/运动/饮水/心情等序列
     */
    @GetMapping("/trends")
    @Operation(summary = "趋势数据", description = "返回近 days 天睡眠/运动/饮水/心情序列")
    public Result<TrendDataVO> trends(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "7") int days) {
        return Result.success(analysisService.getTrend(userId, days));
    }

    /**
     * 分析概览
     *
     * @param userId 用户 id，可空（空时使用默认用户）
     * @param days   统计天数窗口，默认 7
     * @return 各维度平均值与打卡天数等概览数据
     */
    @GetMapping("/overview")
    @Operation(summary = "分析概览", description = "返回近 days 天各维度平均值与打卡天数")
    public Result<Map<String, Object>> overview(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "7") int days) {
        return Result.success(analysisService.getOverview(userId, days));
    }

    /**
     * 目标达成率
     *
     * @param userId 用户 id，可空（空时使用默认用户）
     * @param days   统计天数窗口，默认 7
     * @return 各激活目标的达成率与总体达成率
     */
    @GetMapping("/achievement")
    @Operation(summary = "目标达成率", description = "返回各激活目标的达成率与总体达成率")
    public Result<AchievementRateVO> achievement(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "7") int days) {
        return Result.success(analysisService.getAchievementRate(userId, days));
    }

    /**
     * 五维雷达
     *
     * @param userId 用户 id，可空（空时使用默认用户）
     * @param days   统计天数窗口，默认 7
     * @return 睡眠/运动/饮水/饮食/心情五维 0-100 分值
     */
    @GetMapping("/radar")
    @Operation(summary = "五维雷达", description = "返回睡眠/运动/饮水/饮食/心情五维 0-100 分值")
    public Result<RadarDataVO> radar(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "7") int days) {
        return Result.success(analysisService.getRadar(userId, days));
    }
}
