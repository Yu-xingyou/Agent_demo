package com.habit.agent.tools;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.common.vo.HabitRecordVO;
import com.habit.agent.entity.jpa.HabitRecord;
import com.habit.agent.service.HabitService;
import com.habit.agent.tools.constant.ToolConstants;
import com.habit.agent.tools.result.HabitRecordListResult;
import com.habit.agent.tools.result.HabitRecordResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 生活习惯打卡相关工具（对应 PRD 第 10 节 T01 record_habit / T02 query_habit_records）。
 *
 * <p>封装对 {@link HabitService} 的调用，使 LLM 可通过自然语言"记一下今天 23 点睡、
 * 喝了 2 升水"来完成打卡，或"查一下最近一周的睡眠"来读取记录，实现"对话即操作/查询"的闭环。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HabitTools {

    private final HabitService habitService;

    /**
     * 录入或更新用户某一天的生活习惯打卡记录。
     *
     * <p>同一天重复调用为覆盖更新（依赖 uk_user_date 唯一约束）。
     * 未提供的字段不会被覆盖为 null（仅更新调用方显式传入的字段）。</p>
     *
     * @param recordDate       打卡日期 yyyy-MM-dd，不传默认今天
     * @param sleepTime        入睡时间 HH:mm
     * @param wakeTime         起床时间 HH:mm
     * @param sleepQuality     睡眠质量评分 1-5
     * @param exerciseType     运动类型描述
     * @param exerciseDuration 运动时长（分钟）
     * @param waterIntake      饮水量（毫升）
     * @param dietDesc         饮食备注
     * @param dietScore        饮食评分 1-5
     * @param mood             心情评分 1-5
     * @param remark           其他备注
     * @return 保存后的打卡记录；若解析失败或无有效字段返回 null
     */
    @Tool(description = ToolConstants.Tools.RECORD_HABIT)
    public HabitRecordResult recordHabit(
            @ToolParam(description = ToolConstants.ToolParams.RECORD_DATE, required = false) LocalDate recordDate,
            @ToolParam(description = ToolConstants.ToolParams.SLEEP_TIME, required = false) LocalTime sleepTime,
            @ToolParam(description = ToolConstants.ToolParams.WAKE_TIME, required = false) LocalTime wakeTime,
            @ToolParam(description = ToolConstants.ToolParams.SLEEP_QUALITY, required = false) Integer sleepQuality,
            @ToolParam(description = ToolConstants.ToolParams.EXERCISE_TYPE, required = false) String exerciseType,
            @ToolParam(description = ToolConstants.ToolParams.EXERCISE_DURATION, required = false) Integer exerciseDuration,
            @ToolParam(description = ToolConstants.ToolParams.WATER_INTAKE, required = false) Integer waterIntake,
            @ToolParam(description = ToolConstants.ToolParams.DIET_DESC, required = false) String dietDesc,
            @ToolParam(description = ToolConstants.ToolParams.DIET_SCORE, required = false) Integer dietScore,
            @ToolParam(description = ToolConstants.ToolParams.MOOD, required = false) Integer mood,
            @ToolParam(description = ToolConstants.ToolParams.REMARK, required = false) String remark) {

        HabitRecord record = HabitRecord.builder()
                .userId(AgentConstants.DEFAULT_USER_ID)
                .recordDate(recordDate != null ? recordDate : LocalDate.now())
                .sleepTime(sleepTime)
                .wakeTime(wakeTime)
                .sleepQuality(sleepQuality)
                .exerciseType(exerciseType)
                .exerciseDuration(exerciseDuration)
                .waterIntake(waterIntake)
                .dietDesc(dietDesc)
                .dietScore(dietScore)
                .mood(mood)
                .remark(remark)
                .build();

        return HabitRecordResult.of(habitService.saveOrUpdate(record));
    }

    /**
     * 查询用户的生活习惯打卡记录。
     *
     * <p>查询优先级：指定单日（queryDate） &gt; 日期范围（startDate~endDate） &gt; 最近 N 天（days）。
     * 三者均未提供时默认查最近 7 天。</p>
     *
     * @param days       最近 N 天（含今天）
     * @param startDate  日期范围起点 yyyy-MM-dd
     * @param endDate    日期范围终点 yyyy-MM-dd
     * @param queryDate  指定单日 yyyy-MM-dd
     * @return 命中的打卡记录列表与统计；无记录时返回 total=0 的空结果
     */
    @Tool(description = ToolConstants.Tools.QUERY_HABIT_RECORDS)
    public HabitRecordListResult queryHabitRecords(
            @ToolParam(description = ToolConstants.ToolParams.DAYS, required = false) Integer days,
            @ToolParam(description = ToolConstants.ToolParams.START_DATE, required = false) LocalDate startDate,
            @ToolParam(description = ToolConstants.ToolParams.END_DATE, required = false) LocalDate endDate,
            @ToolParam(description = ToolConstants.ToolParams.QUERY_DATE, required = false) LocalDate queryDate) {

        Long userId = AgentConstants.DEFAULT_USER_ID;
        List<HabitRecordVO> records;

        if (queryDate != null) {
            log.info("Agent 查询单日打卡: userId={}, date={}", userId, queryDate);
            records = habitService.getRecordByDate(userId, queryDate);
        } else if (startDate != null && endDate != null) {
            log.info("Agent 查询范围打卡: userId={}, {}~{}", userId, startDate, endDate);
            records = habitService.getRecordsByDateRange(userId, startDate, endDate);
        } else {
            int effectiveDays = (days != null && days > 0) ? days : 7;
            log.info("Agent 查询最近{}天打卡: userId={}", effectiveDays, userId);
            records = habitService.getRecentRecords(userId, effectiveDays);
        }

        return HabitRecordListResult.of(records);
    }

    /**
     * 删除用户指定的一条例行打卡记录。
     *
     * <p>优先按 recordId 删除；未提供 id 时按 recordDate 定位（需先查 id 再删）。</p>
     *
     * @param recordId   打卡记录 id
     * @param recordDate 打卡日期 yyyy-MM-dd（无 id 时使用）
     * @return 操作结果描述
     */
    @Tool(description = ToolConstants.Tools.DELETE_HABIT_RECORD)
    public String deleteHabitRecord(
            @ToolParam(description = ToolConstants.ToolParams.RECORD_ID, required = false) Long recordId,
            @ToolParam(description = ToolConstants.ToolParams.RECORD_DATE, required = false) LocalDate recordDate) {

        Long userId = AgentConstants.DEFAULT_USER_ID;
        if (recordId != null) {
            log.info("Agent 删除打卡: id={}", recordId);
            habitService.deleteRecord(recordId);
            return "打卡记录已删除: id=" + recordId;
        }
        if (recordDate != null) {
            HabitRecordVO vo = habitService.getRecordByDate(userId, recordDate);
            if (vo == null || vo.getId() == null) {
                return "未找到该日期的打卡记录: " + recordDate;
            }
            log.info("Agent 删除打卡: date={}, id={}", recordDate, vo.getId());
            habitService.deleteRecord(vo.getId());
            return "打卡记录已删除: date=" + recordDate;
        }
        return "删除失败：请提供 recordId 或 recordDate";
    }
}
