package com.habit.agent.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.habit.agent.common.vo.HabitGoalRecordVO;
import com.habit.agent.common.vo.HabitGoalVO;
import com.habit.agent.common.vo.HabitRecordVO;
import com.habit.agent.entity.jpa.HabitGoal;
import com.habit.agent.entity.jpa.HabitGoal.GoalType;
import com.habit.agent.service.GoalService;
import com.habit.agent.service.HabitGoalRecordService;
import com.habit.agent.service.HabitService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 页面路由控制器
 *
 * 负责 Thymeleaf 页面渲染。所有页面数据均从后端 Service 获取后注入 Model。
 * 支持内置目标（SLEEP/EXERCISE/WATER/DIET）和自定义目标（CUSTOM）的动态数据注入。
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PageController {

    private final HabitService habitService;
    private final GoalService goalService;
    private final HabitGoalRecordService goalRecordService;

    /** 内置目标默认值 */
    private static final Map<String, BigDecimal> DEFAULT_TARGETS = Map.of(
            GoalType.SLEEP.name(), BigDecimal.valueOf(8.0),
            GoalType.EXERCISE.name(), BigDecimal.valueOf(30),
            GoalType.WATER.name(), BigDecimal.valueOf(2000),
            GoalType.DIET.name(), BigDecimal.valueOf(4)
    );

    /** 内置目标类型的中文映射 */
    private static final Map<String, String> BUILTIN_TYPE_NAMES = Map.of(
            "SLEEP", "睡眠目标",
            "EXERCISE", "运动目标",
            "WATER", "饮水目标",
            "DIET", "饮食目标"
    );

    /**
     * GET / — 首页
     */
    @GetMapping("/")
    public String index(Model model) {
        HabitRecordVO todayRecord = habitService.getTodayRecord(null);
        List<HabitRecordVO> recentRecords = habitService.getRecentRecords(null, 7);
        List<HabitGoalVO> goals = getAllGoalsWithDefaults();

        Map<String, BigDecimal> weekAvg = calcWeeklyAverage(recentRecords);
        Map<String, Integer> goalRates = calcGoalRates(goals, weekAvg);

        model.addAttribute("todayRecord", todayRecord);
        model.addAttribute("recentRecords", recentRecords);
        model.addAttribute("goals", goals);
        model.addAttribute("weekAvg", weekAvg);
        model.addAttribute("goalRates", goalRates);
        model.addAttribute("builtinTypeNames", BUILTIN_TYPE_NAMES);
        log.info("首页加载: todayRecord={}, recentRecords={}, goals={}",
                todayRecord != null ? todayRecord.getRecordDate() : "null",
                recentRecords.size(), goals.size());
        return "index";
    }

    /**
     * GET /checkin — 每日打卡页
     */
    @GetMapping("/checkin")
    public String checkin(Model model) {
        HabitRecordVO todayRecord = habitService.getTodayRecord(null);
        List<HabitGoalVO> goals = goalService.getActiveGoals(null);

        // 今日已有的自定义目标打卡记录
        List<HabitGoalRecordVO> customRecords = goalRecordService.getByDate(null,
                todayRecord != null ? todayRecord.getRecordDate() : java.time.LocalDate.now());

        // 分离内置类型和自定义类型
        List<HabitGoalVO> customGoals = goals.stream()
                .filter(g -> "CUSTOM".equals(g.getGoalType()))
                .toList();

        // 将自定义打卡记录按 goalId 建立映射
        Map<Long, HabitGoalRecordVO> customRecordMap = customRecords.stream()
                .collect(Collectors.toMap(HabitGoalRecordVO::getGoalId, Function.identity(), (a, b) -> a));

        model.addAttribute("todayRecord", todayRecord);
        model.addAttribute("allGoals", goals);
        model.addAttribute("customGoals", customGoals);
        model.addAttribute("customRecordMap", customRecordMap);
        model.addAttribute("builtinTypeNames", BUILTIN_TYPE_NAMES);
        log.info("打卡页加载: todayRecord={}, customGoals={}",
                todayRecord != null ? todayRecord.getRecordDate() : "null",
                customGoals.size());
        return "checkin";
    }

    /**
     * GET /history — 历史记录页
     */
    @GetMapping("/history")
    public String history(Model model) {
        List<HabitRecordVO> records = habitService.getRecentRecords(null, 30);
        List<HabitGoalVO> goals = goalService.getActiveGoals(null);

        // 最近 30 天的自定义目标打卡记录（按日期分组）
        List<HabitGoalRecordVO> customRecords = goalRecordService.getRecent(null, 30);

        // 将自定义记录按日期+goalId 组织
        Map<String, Map<Long, HabitGoalRecordVO>> customRecordByDate = new HashMap<>();
        for (HabitGoalRecordVO cr : customRecords) {
            String dateKey = cr.getRecordDate() != null ? cr.getRecordDate().toString() : "";
            customRecordByDate.computeIfAbsent(dateKey, k -> new HashMap<>()).put(cr.getGoalId(), cr);
        }

        List<HabitGoalVO> customGoals = goals.stream()
                .filter(g -> "CUSTOM".equals(g.getGoalType()))
                .toList();

        model.addAttribute("records", records);
        model.addAttribute("goals", goals);
        model.addAttribute("customGoals", customGoals);
        model.addAttribute("customRecordByDate", customRecordByDate);
        model.addAttribute("builtinTypeNames", BUILTIN_TYPE_NAMES);
        log.info("历史页加载: records={}, customGoals={}", records.size(), customGoals.size());
        return "history";
    }

    /**
     * GET /trend — 趋势分析页
     */
    @GetMapping("/trend")
    public String trend(Model model) {
        List<HabitRecordVO> records = habitService.getRecentRecords(null, 30);
        List<HabitGoalVO> goals = getAllGoalsWithDefaults();
        Map<String, HabitGoalVO> goalMap = goals.stream()
                .collect(Collectors.toMap(HabitGoalVO::getGoalType, Function.identity(), (a, b) -> a));

        BigDecimal avgSleep = avg(records, HabitRecordVO::getSleepDuration);
        BigDecimal avgExercise = avg(records, HabitRecordVO::getExerciseDuration);
        BigDecimal avgWater = avg(records, HabitRecordVO::getWaterIntake);

        BigDecimal targetSleep = getTarget(goalMap, GoalType.SLEEP, DEFAULT_TARGETS.get(GoalType.SLEEP.name()));
        BigDecimal targetExercise = getTarget(goalMap, GoalType.EXERCISE, DEFAULT_TARGETS.get(GoalType.EXERCISE.name()));
        BigDecimal targetWater = getTarget(goalMap, GoalType.WATER, DEFAULT_TARGETS.get(GoalType.WATER.name()));

        int sleepRate = pct(avgSleep, targetSleep);
        int exerciseRate = pct(avgExercise, targetExercise);
        int waterRate = pct(avgWater, targetWater);

        BigDecimal avgDietScore = avg(records, HabitRecordVO::getDietScore);
        BigDecimal avgMood = avg(records, HabitRecordVO::getMood);
        BigDecimal targetDiet = getTarget(goalMap, GoalType.DIET, DEFAULT_TARGETS.get(GoalType.DIET.name()));

        int dietRate = pct(avgDietScore, targetDiet);
        int moodRate = avgMood == null ? 0 : pct(avgMood, BigDecimal.valueOf(5));

        // 自定义目标数据
        List<HabitGoalVO> customGoals = goals.stream()
                .filter(g -> "CUSTOM".equals(g.getGoalType()))
                .toList();

        // 获取每个自定义目标的历史数据和平均达成率
        Map<Long, BigDecimal> customGoalAvgs = new HashMap<>();
        Map<Long, Integer> customGoalRates = new HashMap<>();
        Map<Long, List<HabitGoalRecordVO>> customGoalHistory = new HashMap<>();

        for (HabitGoalVO cg : customGoals) {
            if (cg.getId() != null) {
                List<HabitGoalRecordVO> history = goalRecordService.getByGoalIdRecent(cg.getId(), 30);
                customGoalHistory.put(cg.getId(), history);

                BigDecimal avgVal = goalRecordService.calcAverage(cg.getId(), 30);
                customGoalAvgs.put(cg.getId(), avgVal);

                int rate = 0;
                if (avgVal != null && cg.getTargetValue() != null && cg.getTargetValue().doubleValue() > 0) {
                    rate = (int) Math.min(100, Math.round(avgVal.doubleValue() / cg.getTargetValue().doubleValue() * 100.0));
                }
                customGoalRates.put(cg.getId(), rate);
            }
        }

        model.addAttribute("records", records);
        model.addAttribute("goals", goals);
        model.addAttribute("customGoals", customGoals);
        model.addAttribute("customGoalAvgs", customGoalAvgs);
        model.addAttribute("customGoalRates", customGoalRates);
        model.addAttribute("customGoalHistory", customGoalHistory);
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
        model.addAttribute("builtinTypeNames", BUILTIN_TYPE_NAMES);
        log.info("趋势页加载: records={}, customGoals={}, avgSleep={}, avgExercise={}, avgWater={}",
                records.size(), customGoals.size(), avgSleep, avgExercise, avgWater);
        return "trend";
    }

    /**
     * GET /ai-chat — AI 建议页
     */
    @GetMapping("/ai-chat")
    public String aiChat() {
        log.info("AI建议页加载");
        return "ai-chat";
    }

    // ===== 辅助方法 =====

    /** 获取所有启用目标（含内置默认补全 + 用户自定义） */
    private List<HabitGoalVO> getAllGoalsWithDefaults() {
        List<HabitGoalVO> activeGoals = goalService.getActiveGoals(null);

        // 分离内置和自定义
        Map<String, HabitGoalVO> existingBuiltin = new HashMap<>();
        List<HabitGoalVO> customGoals = new ArrayList<>();

        for (HabitGoalVO g : activeGoals) {
            if ("CUSTOM".equals(g.getGoalType())) {
                customGoals.add(g);
            } else if (g.getGoalType() != null) {
                existingBuiltin.put(g.getGoalType(), g);
            }
        }

        // 补全内置类型（缺哪类补哪类）
        List<HabitGoalVO> result = new ArrayList<>();
        for (GoalType t : new GoalType[]{GoalType.SLEEP, GoalType.EXERCISE, GoalType.WATER, GoalType.DIET}) {
            if (existingBuiltin.containsKey(t.name())) {
                result.add(existingBuiltin.get(t.name()));
            } else {
                String unit = switch (t) {
                    case SLEEP -> "h";
                    case EXERCISE -> "min";
                    case WATER -> "ml";
                    case DIET -> "/5";
                    default -> "";
                };
                result.add(HabitGoalVO.builder()
                        .goalType(t.name())
                        .displayName(BUILTIN_TYPE_NAMES.getOrDefault(t.name(), t.name()))
                        .targetValue(DEFAULT_TARGETS.get(t.name()))
                        .unit(unit)
                        .period(HabitGoal.Period.DAILY.name())
                        .isActive(Boolean.TRUE)
                        .build());
            }
        }
        // 追加自定义目标
        result.addAll(customGoals);
        return result;
    }

    private <T> BigDecimal avg(List<HabitRecordVO> records, Function<HabitRecordVO, T> getter) {
        var values = records.stream().map(getter).filter(java.util.Objects::nonNull).toList();
        if (values.isEmpty()) return null;
        double sum = values.stream().mapToDouble(v -> Double.parseDouble(v.toString())).sum();
        return BigDecimal.valueOf(sum / values.size()).setScale(1, RoundingMode.HALF_UP);
    }

    private int pct(BigDecimal avg, double target) {
        if (avg == null) return 0;
        return Math.min(100, (int) (avg.doubleValue() / target * 100));
    }

    private int pct(BigDecimal avg, BigDecimal target) {
        if (avg == null || target == null || target.doubleValue() <= 0) return 0;
        return Math.min(100, (int) (avg.doubleValue() / target.doubleValue() * 100));
    }

    private BigDecimal getTarget(Map<String, HabitGoalVO> goalMap, GoalType type, BigDecimal defaultValue) {
        HabitGoalVO g = goalMap.get(type.name());
        if (g != null && g.getTargetValue() != null) return g.getTargetValue();
        return defaultValue;
    }

    private Map<String, BigDecimal> calcWeeklyAverage(List<HabitRecordVO> records) {
        BigDecimal sleep = avg(records, HabitRecordVO::getSleepDuration);
        BigDecimal exercise = avg(records, HabitRecordVO::getExerciseDuration);
        BigDecimal water = avg(records, HabitRecordVO::getWaterIntake);
        BigDecimal diet = avg(records, HabitRecordVO::getDietScore);
        Map<String, BigDecimal> result = new HashMap<>();
        result.put(GoalType.SLEEP.name(), sleep != null ? sleep : BigDecimal.ZERO);
        result.put(GoalType.EXERCISE.name(), exercise != null ? exercise : BigDecimal.ZERO);
        result.put(GoalType.WATER.name(), water != null ? water : BigDecimal.ZERO);
        result.put(GoalType.DIET.name(), diet != null ? diet : BigDecimal.ZERO);
        return result;
    }

    private Map<String, Integer> calcGoalRates(List<HabitGoalVO> goals, Map<String, BigDecimal> weekAvg) {
        Map<String, Integer> rates = new HashMap<>();
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
