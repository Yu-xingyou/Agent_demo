package com.habit.agent.tools.result;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.habit.agent.entity.jpa.Reminder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * 提醒工具返回结果（对应 PRD T07/T08）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderResult {

    @JsonPropertyDescription("提醒id")
    private Long id;
    @JsonPropertyDescription("提醒标题")
    private String title;
    @JsonPropertyDescription("提醒时间，格式 HH:mm")
    private LocalTime reminderTime;
    @JsonPropertyDescription("提醒类型：SLEEP/DIET/EXERCISE/WATER/CUSTOM")
    private String reminderType;
    @JsonPropertyDescription("重复星期，1-7 逗号分隔")
    private String weekdays;
    @JsonPropertyDescription("是否启用")
    private Boolean isActive;

    public static ReminderResult of(Reminder reminder) {
        if (reminder == null) {
            return null;
        }
        return ReminderResult.builder()
                .id(reminder.getId())
                .title(reminder.getTitle())
                .reminderTime(reminder.getReminderTime())
                .reminderType(reminder.getReminderType() != null ? reminder.getReminderType().name() : null)
                .weekdays(reminder.getWeekdays())
                .isActive(reminder.getIsActive())
                .build();
    }
}
