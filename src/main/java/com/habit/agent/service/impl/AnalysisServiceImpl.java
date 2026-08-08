package com.habit.agent.service.impl;

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
import com.habit.agent.service.AnalysisService;
import com.habit.agent.service.GoalService;
import com.habit.agent.service.HabitGoalRecordService;
import com.habit.agent.repository.jpa.HabitRecordRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 习惯分析业务逻辑实现
 *
 * 聚合内置目标（睡眠/运动/饮水/饮食/心情）与用户自定义目标（CUSTOM）的真实打卡数据。
 * 自定义目标维度完全动态：按用户实际启用的 CUSTOM 目标生成趋势序列、雷达维度与达成率维度。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private final HabitRecordRepository habitRecordRepository;
    private final GoalService goalService;
    private final HabitGoalRecordService goalRecordService;

    /** 自定义目标莫兰迪配色池（按顺序分配给各 CUSTOM 目标） */
    private static final String[][] CUSTOM_PALETTE = {
        {"#8a7fa0", "#9a8fb0"},
        {"#6f9a8a", "#7faa9a"},
        {"#b08a6f", "#c39b7e"},
        {"#5f8a92", "#6f97a0"},
        {"#a87f8e", "#b88f9e"},
        {"#7e88a3", "#8f96ad"},
    };

    /**
     * 获取趋势数据（内置四维度 + 自定义目标动态序列）
     * <p>聚合指定天数内的真实打卡记录，按日期生成睡眠/运动/饮水/饮食/心情序列，
     * 并附加用户自定义目标的趋势序列。</p>
     *
     * @param userId 用户 ID
     * @param days   统计天数
     * @return 趋势数据视图对象
     */
    @Override
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
        List<Integer> diet = new ArrayList<>();
        for (String date : dateList) {
            HabitRecord r = byDate.get(date);
            sleep.add(r == null ? null : r.getSleepDuration());
            exercise.add(r == null ? null : nullToZero(r.getExerciseDuration()));
            water.add(r == null ? null : nullToZero(r.getWaterIntake()));
            mood.add(r == null ? null : (r.getMood() == null ? null : r.getMood()));
            diet.add(r == null ? null : (r.getDietScore() == null ? null : r.getDietScore()));
        }

        TrendDataVO vo = TrendDataVO.builder()
                .dates(dateList)
                .sleep(sleep)
                .exercise(exercise)
                .water(water)
                .mood(mood)
                .diet(diet)
                .build();

        vo.setCustomSeries(buildCustomSeries(startDate, endDate, dateList));
        return vo;
    }

    /**
     * 获取综合概览（统计卡 + 达成率摘要 + 雷达）
     * <p>组合趋势统计、达成率与各维度雷达数据，供首页仪表盘使用。</p>
     *
     * @param userId 用户 ID
     * @param days   统计天数
     * @return 包含记录天数、各维度均值、达成率与雷达数据的概览 Map
     */
    @Override
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
     * 获取达成率（内置 + 自定义目标动态维度）
     * <p>基于各维度实际均值与目标值计算达成率，内置维度与 CUSTOM 维度均纳入统计。</p>
     *
     * @param userId 用户 ID
     * @param days   统计天数
     * @return 各维度的目标值、实际均值与达成率
     */
    @Override
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
     * 获取雷达数据（内置五维 + 自定义目标动态维度）
     * <p>仅保留用户启用目标的维度，避免空雷达；自定义目标按其均值与上限生成维度。</p>
     *
     * @param userId 用户 ID
     * @param days   统计天数
     * @return 雷达图数据（指标定义、实际值与目标值）
     */
    @Override
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

    /**
     * 构建自定义目标的趋势序列
     * <p>遍历所有启用的 CUSTOM 类型目标，按日期对齐其打卡记录，生成前端图表所需的系列数据。</p>
     *
     * @param startDate 统计起始日期
     * @param endDate   统计截止日期
     * @param dateList  日期列表（用于对齐数据）
     * @return 自定义目标系列视图对象列表
     */
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

    /**
     * 将趋势数据中的饮食评分列表转换为 BigDecimal 列表
     * <p>饮食以 1-5 分近似，读取每日打卡的 dietScore 字段，null 值原样保留。</p>
     *
     * @param trend 趋势数据视图对象
     * @return BigDecimal 类型的饮食评分列表（包含 null 占位）
     */
    private List<BigDecimal> dietScores(TrendDataVO trend) {
        // 饮食以 1-5 评分近似，读取每日打卡 dietScore
        List<Integer> diet = trend.getDiet();
        if (diet == null) return new ArrayList<>();
        return diet.stream()
                .map(d -> d == null ? null : BigDecimal.valueOf(d))
                .collect(Collectors.toList());
    }

    /**
     * 将 Integer 安全转为 BigDecimal，null 时返回零值
     *
     * @param v 原始 Integer 值
     * @return 非 null 的 BigDecimal，null 输入返回 {@link BigDecimal#ZERO}
     */
    private BigDecimal nullToZero(Integer v) {
        return v == null ? BigDecimal.ZERO : BigDecimal.valueOf(v);
    }

    /**
     * 计算数值列表的平均值，自动排除 null 元素，空列表返回 0
     *
     * @param list 数值列表（元素允许为 null）
     * @return 平均值，无有效数据时返回 0
     */
    private double avgOrZero(List<? extends Number> list) {
        if (list == null || list.isEmpty()) return 0;
        double sum = 0;
        int count = 0;
        for (Number n : list) {
            if (n != null) { sum += n.doubleValue(); count++; }
        }
        return count == 0 ? 0 : sum / count;
    }

    /**
     * 计算 BigDecimal 列表的平均值（返回 double），自动排除 null 元素，空列表返回 0
     *
     * @param list BigDecimal 列表（元素允许为 null）
     * @return 平均值，无有效数据时返回 0
     */
    private double avgOrNull(List<BigDecimal> list) {
        if (list == null || list.isEmpty()) return 0;
        double sum = 0;
        int count = 0;
        for (BigDecimal n : list) {
            if (n != null) { sum += n.doubleValue(); count++; }
        }
        return count == 0 ? 0 : sum / count;
    }

    /**
     * 计算 BigDecimal 列表的平均值（返回 BigDecimal），自动排除 null，结果保留两位小数
     *
     * @param list BigDecimal 列表（元素允许为 null）
     * @return 平均值（保留两位），无有效数据时返回 null
     */
    private BigDecimal avgOrNullBig(List<BigDecimal> list) {
        if (list == null || list.isEmpty()) return null;
        double sum = 0;
        int count = 0;
        for (BigDecimal n : list) {
            if (n != null) { sum += n.doubleValue(); count++; }
        }
        return count == 0 ? null : BigDecimal.valueOf(sum / count).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 将 double 值四舍五入保留两位小数
     *
     * @param v 原始值
     * @return 保留两位小数的结果
     */
    private double round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 将达成率限制在 [0, 100] 区间内并保留两位小数
     *
     * @param rate 原始达成率
     * @return 裁剪后的达成率
     */
    private double clampRate(double rate) {
        return Math.max(0, Math.min(100, round2(rate)));
    }
}
