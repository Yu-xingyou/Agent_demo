package com.habit.agent.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.common.vo.AchievementRateVO;
import com.habit.agent.common.vo.AchievementRateVO.DimensionRate;
import com.habit.agent.common.vo.CustomGoalSeriesVO;
import com.habit.agent.common.vo.HabitGoalRecordVO;
import com.habit.agent.common.vo.HabitGoalVO;
import com.habit.agent.common.vo.RadarDataVO;
import com.habit.agent.common.vo.RadarDataVO.Indicator;
import com.habit.agent.common.vo.TrendDataVO;
import com.habit.agent.entity.jpa.HabitRecord;
import com.habit.agent.repository.jpa.HabitRecordRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 习惯分析业务逻辑
 *
 * 聚合内置目标（睡眠/运动/饮水/饮食/心情）与用户自定义目标（CUSTOM）的真实打卡数据。
 * 自定义目标维度完全动态：按用户实际启用的 CUSTOM 目标生成趋势序列、雷达维度与达成率维度。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final HabitRecordRepository habitRecordRepository;
    private final GoalService goalService;
    private final HabitGoalRecordService goalRecordService;

    // 自定义目标莫兰迪配色池（按顺序分配）
    private static final String[][] CUSTOM_PALETTE = {
        {"#8a7fa0", "#9a8fb0"},
        {"#6f9a8a", "#7faa9a"},
        {"#b08a6f", "#c39b7e"},
        {"#5f8a92", "#6f97a0"},
        {"#a87f8e", "#b88f9e"},
        {"#7e88a3", "#8f96ad"},
    };

    /**
     * 趋势数据（内置四维度 + 自定义目标动态序列）
     */
    public TrendDataVO getTrend(Long userId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1L);

        List<HabitRecord> records = habitRecordRepository
                .findByUserIdAndRecordDateBetweenOrderByRecordDateDesc(
                        AgentConstants.DEFAULT_USER_ID, startDate, endDate);

        List<String> dateList = new ArrayList<>();
        Map<String, HabitRecord> byDate = new LinkedHashMap<>();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            dateList.add(d.toString());
        }
        for (HabitRecord r : records) {
            byDate.put(r.getRecordDate().toString(), r);
        }

        List<BigDecimal> sleep = new ArrayList<>();
        List<BigDecimal> exercise = new ArrayList<>();
        List<BigDecimal> water = new ArrayList<>();
        List<Integer> mood = new ArrayList<>();
        for (String date : dateList) {
            HabitRecord r = byDate.get(date);
            sleep.add(r == null ? null : r.getSleepDuration());
            exercise.add(r == null ? null : nullToZero(r.getExerciseDuration()));
            water.add(r == null ? null : nullToZero(r.getWaterIntake()));
            mood.add(r == null ? null : (r.getMood() == null ? null : r.getMood()));
        }

        TrendDataVO vo = TrendDataVO.builder()
                .dates(dateList)
                .sleep(sleep)
                .exercise(exercise)
                .water(water)
                .mood(mood)
                .build();

        vo.setCustomSeries(buildCustomSeries(startDate, endDate, dateList));
        return vo;
    }

    /**
     * 综合概览（统计卡 + 达成率摘要 + 雷达）
     */
    public Map<String, Object> getOverview(Long userId, int days) {
        TrendDataVO trend = getTrend(userId, days);
        AchievementRateVO achievement = getAchievementRate(userId, days);
        RadarDataVO radar = getRadar(userId, days);

        int recordDays = (int) trend.getDates().stream()
                .filter(d -> {
                    int idx = trend.getDates().indexOf(d);
                    return trend.getSleep() != null && trend.getSleep().get(idx) != null;
                })
                .count();

        double avgSleep = avgOrNull(trend.getSleep());
        double avgExercise = avgOrZero(trend.getExercise());
        double avgWater = avgOrZero(trend.getWater());
        double avgMood = avgOrZero(trend.getMood());

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("recordDays", recordDays);
        overview.put("avgSleep", round2(avgSleep));
        overview.put("avgExercise", round2(avgExercise));
        overview.put("avgWater", round2(avgWater));
        overview.put("avgMood", round2(avgMood));
        overview.put("achievement", achievement);
        overview.put("radar", radar);
        return overview;
    }

    /**
     * 达成率（内置 + 自定义目标动态维度）
     */
    public AchievementRateVO getAchievementRate(Long userId, int days) {
        List<HabitGoalVO> goals = goalService.getActiveGoalsWithCustom(null);
        TrendDataVO trend = getTrend(userId, days);
        Map<String, BigDecimal> builtinAvg = new LinkedHashMap<>();
        builtinAvg.put("SLEEP", avgOrNullBig(trend.getSleep()));
        builtinAvg.put("EXERCISE", avgOrNullBig(trend.getExercise()));
        builtinAvg.put("WATER", avgOrNullBig(trend.getWater()));
        builtinAvg.put("DIET", avgOrNullBig(dietScores(trend)));

        List<DimensionRate> dims = new ArrayList<>();
        for (HabitGoalVO g : goals) {
            if (g.getGoalType() == null || g.getTargetValue() == null) continue;
            if (g.getGoalType().equals("CUSTOM")) {
                // 自定义目标：用其打卡记录均值
                BigDecimal avg = goalRecordService.calcAverage(g.getId(), days);
                if (avg == null) avg = BigDecimal.ZERO;
                dims.add(DimensionRate.builder()
                        .label(g.getDisplayName())
                        .target(BigDecimal.valueOf(round2(g.getTargetValue().doubleValue())))
                        .actual(BigDecimal.valueOf(round2(avg.doubleValue())))
                        .rate(BigDecimal.valueOf(clampRate(avg.doubleValue() / g.getTargetValue().doubleValue() * 100)))
                        .build());
            } else if (builtinAvg.containsKey(g.getGoalType())) {
                BigDecimal avg = builtinAvg.get(g.getGoalType());
                if (avg == null) avg = BigDecimal.ZERO;
                dims.add(DimensionRate.builder()
                        .label(g.getDisplayName())
                        .target(BigDecimal.valueOf(round2(g.getTargetValue().doubleValue())))
                        .actual(BigDecimal.valueOf(round2(avg.doubleValue())))
                        .rate(BigDecimal.valueOf(clampRate(avg.doubleValue() / g.getTargetValue().doubleValue() * 100)))
                        .build());
            }
        }
        return AchievementRateVO.builder().dimensions(dims).build();
    }

    /**
     * 雷达数据（内置五维 + 自定义目标动态维度）
     */
    public RadarDataVO getRadar(Long userId, int days) {
        List<HabitGoalVO> goals = goalService.getActiveGoalsWithCustom(null);
        TrendDataVO trend = getTrend(userId, days);

        Map<String, Integer> builtinMax = new LinkedHashMap<>();
        builtinMax.put("SLEEP", 8);
        builtinMax.put("EXERCISE", 120);
        builtinMax.put("WATER", 3000);
        builtinMax.put("DIET", 5);
        builtinMax.put("MOOD", 5);

        Map<String, Double> builtinVal = new LinkedHashMap<>();
        builtinVal.put("SLEEP", avgOrNull(trend.getSleep()));
        builtinVal.put("EXERCISE", avgOrZero(trend.getExercise()));
        builtinVal.put("WATER", avgOrZero(trend.getWater()));
        builtinVal.put("DIET", avgOrZero(dietScores(trend)));
        builtinVal.put("MOOD", avgOrZero(trend.getMood()));

        // 固定五维（仅保留用户启用了对应目标的维度，避免空雷达）
        List<String> fixedOrder = List.of("SLEEP", "EXERCISE", "WATER", "DIET", "MOOD");
        Map<String, String> fixedLabel = Map.of(
                "SLEEP", "睡眠", "EXERCISE", "运动", "WATER", "饮水",
                "DIET", "饮食", "MOOD", "心情");
        List<Indicator> indicators = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        List<Double> targets = new ArrayList<>();
        for (String key : fixedOrder) {
            boolean has = goals.stream().anyMatch(g -> key.equals(g.getGoalType()));
            if (!has) continue;
            indicators.add(Indicator.builder().name(fixedLabel.get(key)).max(builtinMax.get(key).doubleValue()).build());
            values.add(round2(builtinVal.get(key)));
            targets.add(round2(builtinMax.get(key) * 0.8));
        }

        // 自定义目标维度
        for (HabitGoalVO g : goals) {
            if (!"CUSTOM".equals(g.getGoalType())) continue;
            BigDecimal avg = goalRecordService.calcAverage(g.getId(), days);
            double val = avg == null ? 0 : avg.doubleValue();
            double max = g.getTargetValue() != null ? g.getTargetValue().doubleValue() * 1.5 : 10;
            if (max <= 0) max = 10;
            indicators.add(Indicator.builder().name(g.getDisplayName()).max(round2(max)).build());
            values.add(round2(val));
            targets.add(round2(max * 0.8));
        }

        return RadarDataVO.builder()
                .indicators(indicators)
                .values(values)
                .targets(targets)
                .build();
    }

    // ===== 内部工具 =====

    private List<CustomGoalSeriesVO> buildCustomSeries(LocalDate startDate, LocalDate endDate, List<String> dateList) {
        List<HabitGoalVO> goals = goalService.getActiveGoalsWithCustom(null)
                .stream().filter(g -> "CUSTOM".equals(g.getGoalType())).collect(Collectors.toList());
        List<CustomGoalSeriesVO> series = new ArrayList<>();
        int idx = 0;
        for (HabitGoalVO g : goals) {
            String[] colors = CUSTOM_PALETTE[idx % CUSTOM_PALETTE.length];
            idx++;

            // 该目标在范围内的所有记录
            List<HabitGoalRecordVO> recs = goalRecordService.getByDateRange(null, startDate, endDate)
                    .stream().filter(r -> g.getId().equals(r.getGoalId())).collect(Collectors.toList());
            Map<String, BigDecimal> byDate = recs.stream()
                    .collect(Collectors.toMap(r -> r.getRecordDate().toString(), HabitGoalRecordVO::getValue, (a, b) -> a));

            List<BigDecimal> data = dateList.stream()
                    .map(d -> byDate.getOrDefault(d, null))
                    .collect(Collectors.toList());

            series.add(CustomGoalSeriesVO.builder()
                    .goalId(g.getId())
                    .name(g.getDisplayName())
                    .colorFrom(colors[0])
                    .colorTo(colors[1])
                    .unit(g.getUnit())
                    .targetValue(g.getTargetValue())
                    .data(data)
                    .build());
        }
        return series;
    }

    private List<BigDecimal> dietScores(TrendDataVO trend) {
        // 饮食以 1-5 评分近似，这里复用趋势中未单独存储，返回空序列（由雷达/达成按目标均值处理）
        return trend.getDates().stream().map(d -> (BigDecimal) null).collect(Collectors.toList());
    }

    private BigDecimal nullToZero(Integer v) {
        return v == null ? BigDecimal.ZERO : BigDecimal.valueOf(v);
    }

    private double avgOrZero(List<? extends Number> list) {
        if (list == null || list.isEmpty()) return 0;
        double sum = 0;
        int count = 0;
        for (Number n : list) {
            if (n != null) { sum += n.doubleValue(); count++; }
        }
        return count == 0 ? 0 : sum / count;
    }

    private double avgOrNull(List<BigDecimal> list) {
        if (list == null || list.isEmpty()) return 0;
        double sum = 0;
        int count = 0;
        for (BigDecimal n : list) {
            if (n != null) { sum += n.doubleValue(); count++; }
        }
        return count == 0 ? 0 : sum / count;
    }

    private BigDecimal avgOrNullBig(List<BigDecimal> list) {
        if (list == null || list.isEmpty()) return null;
        double sum = 0;
        int count = 0;
        for (BigDecimal n : list) {
            if (n != null) { sum += n.doubleValue(); count++; }
        }
        return count == 0 ? null : BigDecimal.valueOf(sum / count).setScale(2, RoundingMode.HALF_UP);
    }

    private double round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private double clampRate(double rate) {
        return Math.max(0, Math.min(100, round2(rate)));
    }
}
