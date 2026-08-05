package com.habit.agent.common.vo;

import com.habit.agent.entity.jpa.Reminder.ReminderType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

import java.time.LocalTime;

/**
 * 创建/更新提醒请求体（阶段十）。
 */
@Data
public class ReminderCreateVO {

    /** 提醒标题（与 userId 唯一约束） */
    @NotBlank(message = "标题不能为空")
    private String title;

    /** 提醒时间（HH:mm） */
    @NotNull(message = "提醒时间不能为空")
    private LocalTime reminderTime;

    /** 提醒类型 */
    @NotNull(message = "提醒类型不能为空")
    private ReminderType reminderType;

    /** 重复星期（1-7 逗号分隔，默认每日） */
    private String weekdays;

    /** 是否启用（默认 true） */
    private Boolean isActive;
}
