package com.habit.agent.config;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.entity.jpa.HabitGoal;
import com.habit.agent.entity.jpa.HabitRecord;
import com.habit.agent.entity.jpa.User;
import com.habit.agent.repository.jpa.HabitGoalRepository;
import com.habit.agent.repository.jpa.HabitRecordRepository;
import com.habit.agent.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Random;

/**
 * 测试数据初始化器（子模块 2-2）
 *
 * 启动时：
 * 1. 确保默认用户存在（username=demo）
 * 2. 如果 habit_record 表为空，生成 14 天模拟打卡数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final HabitRecordRepository habitRecordRepository;
    private final HabitGoalRepository habitGoalRepository;

    private final Random random = new Random();

    @Override
    public void run(String... args) {
        ensureDefaultUser();
        ensureDefaultGoals();
        generateMockRecords();
    }

    /**
     * 确保默认用户存在
     */
    private void ensureDefaultUser() {
        Optional<User> existing = userRepository.findByUsername(AgentConstants.DEFAULT_USERNAME);
        if (existing.isEmpty()) {
            User user = User.builder()
                    .username(AgentConstants.DEFAULT_USERNAME)
                    .nickname(AgentConstants.DEFAULT_NICKNAME)
                    .build();
            userRepository.save(user);
            log.info("创建默认用户: {}", AgentConstants.DEFAULT_USERNAME);
        }
    }

    /**
     * 确保默认目标存在（与 schema.sql 种子数据互补）
     */
    private void ensureDefaultGoals() {
        long count = habitGoalRepository.count();
        if (count == 0) {
            createGoal(HabitGoal.GoalType.SLEEP, new BigDecimal("8.0"), "小时", HabitGoal.Period.DAILY);
            createGoal(HabitGoal.GoalType.EXERCISE, new BigDecimal("30"), "分钟", HabitGoal.Period.DAILY);
            createGoal(HabitGoal.GoalType.WATER, new BigDecimal("2000"), "毫升", HabitGoal.Period.DAILY);
            createGoal(HabitGoal.GoalType.DIET, new BigDecimal("3"), "分", HabitGoal.Period.DAILY);
            log.info("创建默认目标: 4 条");
        }
    }

    /**
     * 创建一条默认目标并落库
     *
     * @param type   目标类型（SLEEP/EXERCISE/WATER/DIET）
     * @param target 目标值
     * @param unit   计量单位
     * @param period 周期（DAILY 等）
     */
    private void createGoal(HabitGoal.GoalType type, BigDecimal target, String unit, HabitGoal.Period period) {
        HabitGoal goal = HabitGoal.builder()
                .userId(AgentConstants.DEFAULT_USER_ID)
                .goalType(type)
                .targetValue(target)
                .unit(unit)
                .period(period)
                .isActive(true)
                .build();
        habitGoalRepository.save(goal);
    }

    /**
     * 如果 habit_record 表为空，生成 14 天模拟数据
     */
    private void generateMockRecords() {
        if (habitRecordRepository.count() > 0) {
            log.info("打卡记录已存在({}条), 跳过模拟数据生成", habitRecordRepository.count());
            return;
        }

        LocalDate today = LocalDate.now();
        String[] exerciseTypes = {"跑步", "游泳", "瑜伽", "骑行", "散步", null};
        String[] dietDescs = {
                "早餐:燕麦+鸡蛋, 午餐:鸡胸肉+西兰花, 晚餐:蔬菜沙拉",
                "早餐:面包+牛奶, 午餐:牛肉面, 晚餐:清蒸鱼",
                "早餐:包子+豆浆, 午餐:米饭+回锅肉, 晚餐:番茄炒蛋",
                "早餐:三明治, 午餐:沙拉+鸡胸肉, 晚餐:小米粥",
                "早餐:油条+豆浆, 午餐:盖饭, 晚餐:火锅"
        };

        for (int i = 13; i >= 0; i--) {
            LocalDate date = today.minusDays(i);

            // 随机睡眠时间 22:00-01:30
            int sleepHour = 22 + random.nextInt(4); // 22-25 (25→01:00)
            int sleepMinute = random.nextInt(60);
            LocalTime sleepTime = LocalTime.of(sleepHour % 24, sleepMinute);

            // 随机起床时间 06:00-09:00
            int wakeHour = 6 + random.nextInt(4); // 6-9
            int wakeMinute = random.nextInt(60);
            LocalTime wakeTime = LocalTime.of(wakeHour, wakeMinute);

            HabitRecord record = HabitRecord.builder()
                    .userId(AgentConstants.DEFAULT_USER_ID)
                    .recordDate(date)
                    .sleepTime(sleepTime)
                    .wakeTime(wakeTime)
                    .sleepQuality(3 + random.nextInt(3))       // 3-5
                    .dietDesc(dietDescs[random.nextInt(dietDescs.length)])
                    .dietScore(3 + random.nextInt(3))           // 3-5
                    .exerciseType(exerciseTypes[random.nextInt(exerciseTypes.length)])
                    .exerciseDuration(20 + random.nextInt(61))  // 20-80 min
                    .waterIntake(1200 + random.nextInt(1801))   // 1200-3000 ml
                    .mood(3 + random.nextInt(3))                // 3-5
                    .remark(i == 0 ? "今日打卡" : "")
                    .build();

            habitRecordRepository.save(record);
        }
        log.info("生成 14 天模拟打卡数据完成");
    }
}
