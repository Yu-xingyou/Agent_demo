package com.habit.agent.agent.tools;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.common.vo.HabitRecordVO;
import com.habit.agent.entity.jpa.HabitGoalRecord;
import com.habit.agent.entity.jpa.HabitRecord;
import com.habit.agent.service.HabitGoalRecordService;
import com.habit.agent.service.HabitService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 习惯打卡执行工具（阶段六增强：打通「建议→执行」闭环）。
 *
 * <p>让 AI 不只是给建议，还能在用户口述后直接帮其完成打卡：
 * 内置五维（睡眠/运动/饮水/饮食/心情）与自定义目标打卡。
 * 当前 demo 为单用户，固定使用 {@link AgentConstants#DEFAULT_USER_ID}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HabitActionTools {

    private final HabitService habitService;

    private final HabitGoalRecordService habitGoalRecordService;

    /**
     * 内置维度枚举，与 HabitRecord 字段对应。
     */
    private enum Dimension {
        SLEEP, EXERCISE, WATER, DIET, MOOD
    }

    @Tool(description = "为用户记录今日某个内置习惯的打卡数据（睡眠小时/运动分钟/饮水ml/饮食评分1-5/心情1-5）。"
            + "当用户说「我今天运动了30分钟」「帮我记一下喝了1500ml水」「今天心情不错」时调用。"
            + "dimension 取值：SLEEP(小时) EXERCISE(分钟) WATER(ml) DIET(评分1-5) MOOD(评分1-5)。")
    public String checkInHabit(
            @ToolParam(description = "习惯维度：SLEEP / EXERCISE / WATER / DIET / MOOD") String dimension,
            @ToolParam(description = "数值，如运动30、饮水1500、评分4") BigDecimal value,
            @ToolParam(description = "可选备注") String remark) {
        try {
            Dimension dim = Dimension.valueOf(dimension.trim().toUpperCase());
            HabitRecordVO today = habitService.getTodayRecord(AgentConstants.DEFAULT_USER_ID);
            HabitRecord record = HabitRecord.builder()
                    .userId(AgentConstants.DEFAULT_USER_ID)
                    .recordDate(LocalDate.now())
                    .build();
            // 复用今日已有打卡的其他维度数值，避免覆盖
            if (today != null) {
                record.setSleepDuration(today.getSleepDuration());
                record.setExerciseDuration(today.getExerciseDuration());
                record.setWaterIntake(today.getWaterIntake());
                record.setDietScore(today.getDietScore());
                record.setMood(today.getMood());
                record.setRemark(today.getRemark());
            }
            switch (dim) {
                case SLEEP -> record.setSleepDuration(value);
                case EXERCISE -> record.setExerciseDuration(value.intValue());
                case WATER -> record.setWaterIntake(value.intValue());
                case DIET -> record.setDietScore(value.intValue());
                case MOOD -> record.setMood(value.intValue());
                default -> { /* 已通过枚举校验 */ }
            }
            if (remark != null && !remark.isBlank()) {
                record.setRemark(remark);
            }
            habitService.saveOrUpdate(record);
            return "已为你记录今日「" + dim + "」=" + value + "。继续保持！";
        } catch (IllegalArgumentException e) {
            return "dimension 仅支持 SLEEP/EXERCISE/WATER/DIET/MOOD，请检查输入：" + dimension;
        } catch (Exception e) {
            log.warn("[HabitActionTools] 打卡失败: {}", e.getMessage());
            return "打卡时发生错误：" + e.getMessage();
        }
    }

    @Tool(description = "为用户记录今日某个自定义目标的打卡（需先 listActiveGoals 取得 goalId）。"
            + "当用户说「标记今天阅读完成」「自定义目标打卡」时调用。")
    public String checkInCustomGoal(
            @ToolParam(description = "目标 ID（先调用 listActiveGoals 查询）") Long goalId,
            @ToolParam(description = "本次打卡数值，如阅读30、冥想10") BigDecimal value,
            @ToolParam(description = "可选备注") String remark) {
        try {
            HabitGoalRecord record = HabitGoalRecord.builder()
                    .userId(AgentConstants.DEFAULT_USER_ID)
                    .goalId(goalId)
                    .recordDate(LocalDate.now())
                    .value(value)
                    .remark(remark)
                    .build();
            habitGoalRecordService.saveOrUpdate(record);
            return "已为自定义目标(ID=" + goalId + ")记录今日打卡：数值=" + value + "。棒！";
        } catch (Exception e) {
            log.warn("[HabitActionTools] 自定义目标打卡失败: {}", e.getMessage());
            return "自定义目标打卡时发生错误：" + e.getMessage();
        }
    }
}
