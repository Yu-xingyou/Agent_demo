package com.habit.agent.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.common.vo.AchievementRateVO;
import com.habit.agent.common.vo.AnalysisOverviewVO;
import com.habit.agent.common.vo.HabitGoalVO;
import com.habit.agent.common.vo.RadarDataVO;
import com.habit.agent.common.vo.TrendDataVO;
import com.habit.agent.entity.jpa.HabitGoal.GoalType;
import com.habit.agent.entity.jpa.HabitRecord;
import com.habit.agent.repository.jpa.HabitRecordRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 习惯分析服务（阶段九）
 *
 * 基于 HabitRecord + HabitGoal 聚合计算：趋势、达成率、概览、雷达维度。
 * 复用既有 Repository 与 GoalService，纯内存聚合，不引入新查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final HabitRecordRepository habitRecordRepository;
    private final GoalService goalService;

    /** 默认分析窗口天数 */
    private static final int DEFAULT_DAYS = 7;
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private Long resolveUserId(Long userId) {
        return userId == null ? AgentConstants.DEFAULT_USER_ID : userId;
    }

    /** 取最近 days 天的记录（升序） */
    private List<HabitRecord> recentRecords(Long userId, int days) {
        userId = resolveUserId(userId);
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1L);
        List<HabitRecord> list = habitRecordRepository
                .findByUserIdAndRecordDateBetweenOrderByRecordDateDesc(userId, start, end);
        list.sort((a, b) -> a.getRecordDate().compareTo(b.getRecordDate()));
        return list;
    }

    /**
     * 趋势数据：睡眠/运动/饮水/心情 四条序列
     */
    public TrendDataVO getTrend(Long userId, int days) {
        if (days <= 0) days = DEFAULT_DAYS;
        List<HabitRecord> records = recentRecords(userId, days);
        List<String> dates = new ArrayList<>();
        List<BigDecimal> sleep = new ArrayList<>();
        List<BigDecimal> exercise = new ArrayList<>();
        List<BigDecimal> water = new ArrayList<>();
        List<Integer> mood = new ArrayList<>();
        for (HabitRecord r : records) {
            dates.add(r.getRecordDate().toString());
            sleep.add(r.getSleepDuration() == null ? ZERO : r.getSleepDuration());
            exercise.add(BigDecimal.valueOf(r.getExerciseDuration() == null ? 0 : r.getExerciseDuration()));
            water.add(BigDecimal.valueOf(r.getWaterIntake() == null ? 0 : r.getWaterIntake()));
            mood.add(r.getMood() == null ? 0 : r.getMood());
        }
        return TrendDataVO.builder()
                .dates(dates)
                .sleep(sleep)
                .exercise(exercise)
                .water(water)
                .mood(mood)
                .build();
    }

    /**
     * 概览：各维度平均值与打卡天数
     */
    public AnalysisOverviewVO getOverview(Long userId, int days) {
        if (days <= 0) days = DEFAULT_DAYS;
        List<HabitRecord> records = recentRecords(userId, days);
        AnalysisOverviewVO.AnalysisOverviewVOBuilder builder = AnalysisOverviewVO.builder()
                .days(days)
                .checkedDays(records.size());
        if (records.isEmpty()) {
            return builder.avgSleep(ZERO).avgExercise(ZERO).avgWater(ZERO)
                    .avgMood(ZERO).avgDiet(ZERO).build();
        }
        builder.avgSleep(avgBD(records.stream().map(HabitRecord::getSleepDuration).collect(java.util.stream.Collectors.toList())))
               .avgExercise(avgInt(records.stream().map(HabitRecord::getExerciseDuration).collect(java.util.stream.Collectors.toList())))
               .avgWater(avgInt(records.stream().map(HabitRecord::getWaterIntake).collect(java.util.stream.Collectors.toList())))
               .avgMood(avgInt(records.stream().map(HabitRecord::getMood).collect(java.util.stream.Collectors.toList())))
               .avgDiet(avgInt(records.stream().map(HabitRecord::getDietScore).collect(java.util.stream.Collectors.toList())));
        return builder.build();
    }

    /**
     * 达成率：按激活目标逐一计算（实际均值 / 目标值）
     */
    public AchievementRateVO getAchievementRate(Long userId, int days) {
        if (days <= 0) days = DEFAULT_DAYS;
        List<HabitRecord> records = recentRecords(userId, days);
        List<HabitGoalVO> goals = goalService.getActiveGoals(userId);

        Map<GoalType, BigDecimal> actualAvg = new LinkedHashMap<>();
        if (!records.isEmpty()) {
            actualAvg.put(GoalType.SLEEP, avgBD(records.stream().map(HabitRecord::getSleepDuration).collect(java.util.stream.Collectors.toList())));
            actualAvg.put(GoalType.EXERCISE, avgInt(records.stream().map(HabitRecord::getExerciseDuration).collect(java.util.stream.Collectors.toList())));
            actualAvg.put(GoalType.WATER, avgInt(records.stream().map(HabitRecord::getWaterIntake).collect(java.util.stream.Collectors.toList())));
            actualAvg.put(GoalType.DIET, avgInt(records.stream().map(HabitRecord::getDietScore).collect(java.util.stream.Collectors.toList())));
        } else {
            for (GoalType t : GoalType.values()) actualAvg.put(t, ZERO);
        }

        List<AchievementRateVO.DimensionRate> dimensions = new ArrayList<>();
        BigDecimal sumRate = ZERO;
        int counted = 0;
        for (HabitGoalVO g : goals) {
            GoalType type;
            try {
                type = GoalType.valueOf(g.getGoalType());
            } catch (Exception e) {
                continue;
            }
            if (!actualAvg.containsKey(type) || g.getTargetValue() == null
                    || g.getTargetValue().compareTo(ZERO) <= 0) {
                continue;
            }
            BigDecimal actual = actualAvg.get(type);
            BigDecimal rate = actual.divide(g.getTargetValue(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, RoundingMode.HALF_UP);
            dimensions.add(AchievementRateVO.DimensionRate.builder()
                    .dimension(g.getGoalType())
                    .label(g.getDisplayName())
                    .target(g.getTargetValue())
                    .actual(actual.setScale(2, RoundingMode.HALF_UP))
                    .rate(rate)
                    .build());
            sumRate = sumRate.add(rate);
            counted++;
        }
        BigDecimal overall = counted == 0 ? ZERO
                : sumRate.divide(BigDecimal.valueOf(counted), 1, RoundingMode.HALF_UP);
        return AchievementRateVO.builder()
                .dimensions(dimensions)
                .overallRate(overall)
                .build();
    }

    /**
     * 雷达图：睡眠/运动/饮水/饮食/心情 五维，各维度按目标归一化到 0-100
     */
    public RadarDataVO getRadar(Long userId, int days) {
        if (days <= 0) days = DEFAULT_DAYS;
        List<HabitRecord> records = recentRecords(userId, days);
        List<HabitGoalVO> goals = goalService.getActiveGoals(userId);

        Map<GoalType, BigDecimal> actualAvg = new LinkedHashMap<>();
        if (!records.isEmpty()) {
            actualAvg.put(GoalType.SLEEP, avgBD(records.stream().map(HabitRecord::getSleepDuration).collect(java.util.stream.Collectors.toList())));
            actualAvg.put(GoalType.EXERCISE, avgInt(records.stream().map(HabitRecord::getExerciseDuration).collect(java.util.stream.Collectors.toList())));
            actualAvg.put(GoalType.WATER, avgInt(records.stream().map(HabitRecord::getWaterIntake).collect(java.util.stream.Collectors.toList())));
            actualAvg.put(GoalType.DIET, avgInt(records.stream().map(HabitRecord::getDietScore).collect(java.util.stream.Collectors.toList())));
            actualAvg.put(GoalType.CUSTOM, avgInt(records.stream().map(HabitRecord::getMood).collect(java.util.stream.Collectors.toList())));
        }

        Map<GoalType, BigDecimal> targetMap = new LinkedHashMap<>();
        List<GoalType> dims = List.of(GoalType.SLEEP, GoalType.EXERCISE, GoalType.WATER, GoalType.DIET, GoalType.CUSTOM);
        for (HabitGoalVO g : goals) {
            try {
                GoalType t = GoalType.valueOf(g.getGoalType());
                if (g.getTargetValue() != null) targetMap.put(t, g.getTargetValue());
            } catch (Exception ignored) {
            }
        }
        targetMap.putIfAbsent(GoalType.SLEEP, BigDecimal.valueOf(8));
        targetMap.putIfAbsent(GoalType.EXERCISE, BigDecimal.valueOf(60));
        targetMap.putIfAbsent(GoalType.WATER, BigDecimal.valueOf(2000));
        targetMap.putIfAbsent(GoalType.DIET, BigDecimal.valueOf(5));
        targetMap.putIfAbsent(GoalType.CUSTOM, BigDecimal.valueOf(5));

        List<String> indicators = dims.stream().map(this::dimLabel).collect(java.util.stream.Collectors.toList());
        List<Integer> values = new ArrayList<>();
        List<Integer> targets = new ArrayList<>();
        for (GoalType t : dims) {
            BigDecimal actual = Optional.ofNullable(actualAvg.get(t)).orElse(ZERO);
            BigDecimal target = targetMap.getOrDefault(t, BigDecimal.ONE);
            if (target.compareTo(ZERO) <= 0) target = BigDecimal.ONE;
            int score = actual.divide(target, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValue();
            values.add(clamp(score, 0, 100));
            targets.add(100);
        }
        return RadarDataVO.builder()
                .indicators(indicators)
                .values(values)
                .targets(targets)
                .build();
    }

    private String dimLabel(GoalType t) {
        return switch (t) {
            case SLEEP -> "睡眠";
            case EXERCISE -> "运动";
            case WATER -> "饮水";
            case DIET -> "饮食";
            case CUSTOM -> "心情";
        };
    }

    private BigDecimal avgBD(List<BigDecimal> list) {
        if (list == null || list.isEmpty()) return ZERO;
        BigDecimal sum = ZERO;
        int n = 0;
        for (BigDecimal v : list) {
            if (v != null) {
                sum = sum.add(v);
                n++;
            }
        }
        if (n == 0) return ZERO;
        return sum.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal avgInt(List<Integer> list) {
        if (list == null || list.isEmpty()) return ZERO;
        int sum = 0;
        int n = 0;
        for (Integer v : list) {
            if (v != null) {
                sum += v;
                n++;
            }
        }
        if (n == 0) return ZERO;
        return BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
    }

    private int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
