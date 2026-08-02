package com.habit.agent.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.habit.agent.common.vo.HabitGoalVO;
import com.habit.agent.common.vo.HabitRecordVO;
import com.habit.agent.entity.jpa.HabitGoal;
import com.habit.agent.service.GoalService;
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
    private final GoalService goalService;

    /** 默认目标常量（当用户未设定时使用） */
    private static final Map<String, BigDecimal> DEFAULT_TARGETS = Map.of(
            HabitGoal.GoalType.SLEEP.name(), BigDecimal.valueOf(8.0),
            HabitGoal.GoalType.EXERCISE.name(), BigDecimal.valueOf(30),
            HabitGoal.GoalType.WATER.name(), BigDecimal.valueOf(2000),
            HabitGoal.GoalType.DIET.name(), BigDecimal.valueOf(4)
    );

    /**
     * GET / — 首页
     * 注入今日记录、最近 7 天记录、以及启用的习惯目标（含默认目标补充）
     */
    @GetMapping("/")
    public String index(Model model) {
        HabitRecordVO todayRecord = habitService.getTodayRecord(null);
        List<HabitRecordVO> recentRecords = habitService.getRecentRecords(null, 7);
        List<HabitGoalVO> goals = goalsWithDefaults(goalService.getActiveGoals(null));

        // 计算 7 天平均（用于目标进度对比）
        Map<String, BigDecimal> weekAvg = calcWeeklyAverage(recentRecords);
        // 预计算每个目标的达成率（避免模板里复杂的静态方法调用）
        Map<String, Integer> goalRates = calcGoalRates(goals, weekAvg);

        model.addAttribute("todayRecord", todayRecord);
        model.addAttribute("recentRecords", recentRecords);
        model.addAttribute("goals", goals);
        model.addAttribute("weekAvg", weekAvg);
        model.addAttribute("goalRates", goalRates);
        log.info("首页加载: todayRecord={}, recentRecords={}, goals={}",
                todayRecord != null ? todayRecord.getRecordDate() : "null",
                recentRecords.size(),
                goals.size());
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
     * 注入最近 30 天记录、统计概览（平均睡眠/运动/饮水 + 达成率）、目标数据
     */
    @GetMapping("/trend")
    public String trend(Model model) {
        List<HabitRecordVO> records = habitService.getRecentRecords(null, 30);
        List<HabitGoalVO> goals = goalsWithDefaults(goalService.getActiveGoals(null));
        Map<String, HabitGoalVO> goalMap = goals.stream()
                .collect(Collectors.toMap(HabitGoalVO::getGoalType, Function.identity(), (a, b) -> a));

        // 计算平均值（从后端真实数据计算，前端不硬编码）
        BigDecimal avgSleep = avg(records, HabitRecordVO::getSleepDuration);
        BigDecimal avgExercise = avg(records, HabitRecordVO::getExerciseDuration);
        BigDecimal avgWater = avg(records, HabitRecordVO::getWaterIntake);

        // 达成率：优先使用数据库目标值，否则回退默认目标
        BigDecimal targetSleep = getTarget(goalMap, HabitGoal.GoalType.SLEEP, DEFAULT_TARGETS.get(HabitGoal.GoalType.SLEEP.name()));
        BigDecimal targetExercise = getTarget(goalMap, HabitGoal.GoalType.EXERCISE, DEFAULT_TARGETS.get(HabitGoal.GoalType.EXERCISE.name()));
        BigDecimal targetWater = getTarget(goalMap, HabitGoal.GoalType.WATER, DEFAULT_TARGETS.get(HabitGoal.GoalType.WATER.name()));

        int sleepRate = pct(avgSleep, targetSleep);
        int exerciseRate = pct(avgExercise, targetExercise);
        int waterRate = pct(avgWater, targetWater);

        // 30天平均用于雷达图计算
        BigDecimal avgDietScore = avg(records, HabitRecordVO::getDietScore);
        BigDecimal avgMood = avg(records, HabitRecordVO::getMood);
        BigDecimal targetDiet = getTarget(goalMap, HabitGoal.GoalType.DIET, DEFAULT_TARGETS.get(HabitGoal.GoalType.DIET.name()));

        int dietRate = pct(avgDietScore, targetDiet);
        int moodRate = avgMood == null ? 0 : pct(avgMood, BigDecimal.valueOf(5));

        model.addAttribute("records", records);
        model.addAttribute("goals", goals);
        model.addAttribute("avgSleep", avgSleep);
        model.addAttribute("avgExercise", avgExercise);
        model.addAttribute("avgWater", avgWater);
        model.addAttribute("sleepRate", sleepRate);
        model.addAttribute("exerciseRate", exerciseRate);
        model.addAttribute("waterRate", waterRate);
        model.addAttribute("targetSleep", targetSleep);
        model.addAttribute("targetExercise", targetExercise);
        model.addAttribute("targetWater", targetWater);
        model.addAttribute("targetDiet", targetDiet);
        model.addAttribute("dietRate", dietRate);
        model.addAttribute("moodRate", moodRate);
        log.info("趋势页加载: records={}, avgSleep={}, avgExercise={}, avgWater={}, dietRate={}, moodRate={}",
                records.size(), avgSleep, avgExercise, avgWater, dietRate, moodRate);
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

    /** 计算达成率（BigDecimal 版，target 传 BigDecimal） */
    private int pct(BigDecimal avg, BigDecimal target) {
        if (avg == null || target == null || target.doubleValue() <= 0) return 0;
        return Math.min(100, (int) (avg.doubleValue() / target.doubleValue() * 100));
    }

    /** 从目标映射里取值，回退默认值 */
    private BigDecimal getTarget(Map<String, HabitGoalVO> goalMap, HabitGoal.GoalType type, BigDecimal defaultValue) {
        HabitGoalVO g = goalMap.get(type.name());
        if (g != null && g.getTargetValue() != null) return g.getTargetValue();
        return defaultValue;
    }

    /** 对用户启用的目标补全默认值（缺哪类补哪类），确保 4 类都有 */
    private List<HabitGoalVO> goalsWithDefaults(List<HabitGoalVO> activeGoals) {
        Map<String, HabitGoalVO> existing = activeGoals.stream()
                .filter(g -> g.getGoalType() != null)
                .collect(Collectors.toMap(HabitGoalVO::getGoalType, Function.identity(), (a, b) -> a));

        java.util.ArrayList<HabitGoalVO> result = new java.util.ArrayList<>();
        for (HabitGoal.GoalType t : HabitGoal.GoalType.values()) {
            if (existing.containsKey(t.name())) {
                result.add(existing.get(t.name()));
            } else {
                // 默认目标（id=null 表示在数据库不存在，前端仍可编辑创建）
                String unit = switch (t) {
                    case SLEEP -> "h";
                    case EXERCISE -> "min";
                    case WATER -> "ml";
                    case DIET -> "/5";
                };
                result.add(HabitGoalVO.builder()
                        .goalType(t.name())
                        .targetValue(DEFAULT_TARGETS.get(t.name()))
                        .unit(unit)
                        .period(HabitGoal.Period.DAILY.name())
                        .isActive(Boolean.TRUE)
                        .build());
            }
        }
        return result;
    }

    /** 计算最近7天平均（用于首页目标进度对比） */
    private Map<String, BigDecimal> calcWeeklyAverage(List<HabitRecordVO> records) {
        BigDecimal sleep = avg(records, HabitRecordVO::getSleepDuration);
        BigDecimal exercise = avg(records, HabitRecordVO::getExerciseDuration);
        BigDecimal water = avg(records, HabitRecordVO::getWaterIntake);
        BigDecimal diet = avg(records, HabitRecordVO::getDietScore);
        return Map.of(
                HabitGoal.GoalType.SLEEP.name(), sleep != null ? sleep : BigDecimal.ZERO,
                HabitGoal.GoalType.EXERCISE.name(), exercise != null ? exercise : BigDecimal.ZERO,
                HabitGoal.GoalType.WATER.name(), water != null ? water : BigDecimal.ZERO,
                HabitGoal.GoalType.DIET.name(), diet != null ? diet : BigDecimal.ZERO
        );
    }

    /** 计算每个目标类型的达成率百分比（上限100） */
    private Map<String, Integer> calcGoalRates(List<HabitGoalVO> goals, Map<String, BigDecimal> weekAvg) {
        java.util.HashMap<String, Integer> rates = new java.util.HashMap<>();
        for (HabitGoalVO g : goals) {
            if (g.getGoalType() == null) continue;
            BigDecimal target = g.getTargetValue();
            BigDecimal actual = weekAvg.getOrDefault(g.getGoalType(), BigDecimal.ZERO);
            int rate = 0;
            if (target != null && target.doubleValue() > 0) {
                rate = (int) Math.min(100, Math.round(actual.doubleValue() / target.doubleValue() * 100.0));
            }
            rates.put(g.getGoalType(), rate);
        }
        return rates;
    }
}
