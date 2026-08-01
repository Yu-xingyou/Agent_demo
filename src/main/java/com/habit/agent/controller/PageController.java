package com.habit.agent.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.habit.agent.common.vo.HabitRecordVO;
import com.habit.agent.service.HabitService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 页面路由控制器（子模块 3-1 / 3-2 / 3-3）
 *
 * 负责 Thymeleaf 页面渲染，非 REST API。
 * 所有页面数据均从后端 Service 获取后注入 Model，前端不硬编码任何数据。
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PageController {

    private final HabitService habitService;

    /**
     * GET / — 首页
     * 注入今日记录和最近 7 天记录
     */
    @GetMapping("/")
    public String index(Model model) {
        HabitRecordVO todayRecord = habitService.getTodayRecord(null);
        List<HabitRecordVO> recentRecords = habitService.getRecentRecords(null, 7);

        model.addAttribute("todayRecord", todayRecord);
        model.addAttribute("recentRecords", recentRecords);
        log.info("首页加载: todayRecord={}, recentRecords={}",
                todayRecord != null ? todayRecord.getRecordDate() : "null",
                recentRecords.size());
        return "index";
    }

    /**
     * GET /checkin — 每日打卡页
     * 注入今日记录（已打卡则预填表单）
     */
    @GetMapping("/checkin")
    public String checkin(Model model) {
        HabitRecordVO todayRecord = habitService.getTodayRecord(null);
        model.addAttribute("todayRecord", todayRecord);
        log.info("打卡页加载: todayRecord={}", todayRecord != null ? todayRecord.getRecordDate() : "null");
        return "checkin";
    }

    /**
     * GET /history — 历史记录页
     * 注入最近 30 天记录（用于表格和图表）
     */
    @GetMapping("/history")
    public String history(Model model) {
        List<HabitRecordVO> records = habitService.getRecentRecords(null, 30);
        model.addAttribute("records", records);
        log.info("历史页加载: records={}", records.size());
        return "history";
    }

    /**
     * GET /trend — 趋势分析页
     * 注入最近 30 天记录的统计概览（平均睡眠/运动/饮水 + 达成率）
     */
    @GetMapping("/trend")
    public String trend(Model model) {
        List<HabitRecordVO> records = habitService.getRecentRecords(null, 30);

        // 计算平均值（从后端真实数据计算，前端不硬编码）
        BigDecimal avgSleep = avg(records, HabitRecordVO::getSleepDuration);
        BigDecimal avgExercise = avg(records, HabitRecordVO::getExerciseDuration);
        BigDecimal avgWater = avg(records, HabitRecordVO::getWaterIntake);

        // 达成率（目标: 睡眠8h / 运动30min / 饮水2000ml）
        int sleepRate = pct(avgSleep, 8);
        int exerciseRate = pct(avgExercise, 30);
        int waterRate = pct(avgWater, 2000);

        model.addAttribute("avgSleep", avgSleep);
        model.addAttribute("avgExercise", avgExercise);
        model.addAttribute("avgWater", avgWater);
        model.addAttribute("sleepRate", sleepRate);
        model.addAttribute("exerciseRate", exerciseRate);
        model.addAttribute("waterRate", waterRate);
        log.info("趋势页加载: avgSleep={}, avgExercise={}, avgWater={}", avgSleep, avgExercise, avgWater);
        return "trend";
    }

    /**
     * GET /ai-chat — AI 建议页
     * 本阶段页面可访问但对话功能未实现（阶段四对接）
     */
    @GetMapping("/ai-chat")
    public String aiChat() {
        log.info("AI建议页加载");
        return "ai-chat";
    }

    // ===== 统计辅助方法 =====

    /** 计算平均值（忽略 null，保留 1 位小数） */
    private <T> BigDecimal avg(List<HabitRecordVO> records,
                                java.util.function.Function<HabitRecordVO, T> getter) {
        var values = records.stream()
                .map(getter)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (values.isEmpty()) return null;
        double sum = values.stream()
                .mapToDouble(v -> Double.parseDouble(v.toString()))
                .sum();
        return BigDecimal.valueOf(sum / values.size()).setScale(1, RoundingMode.HALF_UP);
    }

    /** 计算达成率（百分比，上限 100） */
    private int pct(BigDecimal avg, double target) {
        if (avg == null) return 0;
        return Math.min(100, (int) (avg.doubleValue() / target * 100));
    }
}
