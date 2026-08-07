package com.habit.agent.tools.result;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.habit.agent.common.vo.HabitRecordVO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 打卡记录工具返回结果。
 *
 * <p>仅暴露对对话有意义的字段（含 {@code @JsonPropertyDescription} 供 LLM 理解），
 * 由 {@link HabitRecordVO} 转换而来。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HabitRecordResult {

    @JsonPropertyDescription("打卡记录id")
    private Long id;

    @JsonPropertyDescription("打卡日期，格式 yyyy-MM-dd")
    private LocalDate recordDate;

    @JsonPropertyDescription("入睡时间，格式 HH:mm")
    private LocalTime sleepTime;

    @JsonPropertyDescription("起床时间，格式 HH:mm")
    private LocalTime wakeTime;

    @JsonPropertyDescription("睡眠时长（小时），由入睡/起床时间自动计算")
    private Double sleepDuration;

    @JsonPropertyDescription("睡眠质量评分，1-5，5 最好")
    private Integer sleepQuality;

    @JsonPropertyDescription("运动类型描述")
    private String exerciseType;

    @JsonPropertyDescription("运动时长（分钟）")
    private Integer exerciseDuration;

    @JsonPropertyDescription("饮水量（毫升）")
    private Integer waterIntake;

    @JsonPropertyDescription("饮食备注")
    private String dietDesc;

    @JsonPropertyDescription("饮食评分，1-5，5 最健康")
    private Integer dietScore;

    @JsonPropertyDescription("心情评分，1-5，5 最好")
    private Integer mood;

    @JsonPropertyDescription("其他备注")
    private String remark;

    /**
     * 将 HabitRecordVO 转换为工具返回结果。
     *
     * @param vo 打卡记录 VO（可为 null）
     * @return 转换后的结果对象，vo 为 null 时返回 null
     */
    public static HabitRecordResult of(HabitRecordVO vo) {
        if (vo == null) {
            return null;
        }
        return HabitRecordResult.builder()
                .id(vo.getId())
                .recordDate(vo.getRecordDate())
                .sleepTime(vo.getSleepTime())
                .wakeTime(vo.getWakeTime())
                .sleepDuration(vo.getSleepDuration() != null ? vo.getSleepDuration().doubleValue() : null)
                .sleepQuality(vo.getSleepQuality())
                .exerciseType(vo.getExerciseType())
                .exerciseDuration(vo.getExerciseDuration())
                .waterIntake(vo.getWaterIntake())
                .dietDesc(vo.getDietDesc())
                .dietScore(vo.getDietScore())
                .mood(vo.getMood())
                .remark(vo.getRemark())
                .build();
    }
}
