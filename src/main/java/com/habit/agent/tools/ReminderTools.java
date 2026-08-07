package com.habit.agent.tools;

import java.time.LocalTime;
import java.util.List;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.common.vo.ReminderCreateVO;
import com.habit.agent.entity.jpa.Reminder;
import com.habit.agent.entity.jpa.Reminder.ReminderType;
import com.habit.agent.service.ReminderService;
import com.habit.agent.tools.constant.ToolConstants;
import com.habit.agent.tools.result.ReminderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 打卡提醒相关工具（对应 PRD 第 10 节 T07 create_reminder / T08 manage_reminder）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderTools {

    private final ReminderService reminderService;

    /**
     * 创建提醒。
     */
    @Tool(description = ToolConstants.Tools.CREATE_REMINDER)
    public ReminderResult createReminder(
            @ToolParam(description = ToolConstants.ToolParams.REMINDER_TITLE) String title,
            @ToolParam(description = ToolConstants.ToolParams.REMINDER_TIME) LocalTime reminderTime,
            @ToolParam(description = ToolConstants.ToolParams.REMINDER_TYPE) String reminderType,
            @ToolParam(description = ToolConstants.ToolParams.WEEKDAYS, required = false) String weekdays) {

        ReminderCreateVO vo = new ReminderCreateVO();
        vo.setTitle(title);
        vo.setReminderTime(reminderTime);
        vo.setReminderType(ReminderType.valueOf(reminderType.trim().toUpperCase()));
        vo.setWeekdays(weekdays);
        log.info("Agent 创建提醒: title={}, time={}, type={}", title, reminderTime, reminderType);
        return ReminderResult.of(reminderService.create(AgentConstants.DEFAULT_USER_ID, vo));
    }

    /**
     * 管理提醒：列出/更新/删除/启停。
     *
     * @param action 操作：LIST/UPDATE/DELETE/TOGGLE
     */
    @Tool(description = ToolConstants.Tools.MANAGE_REMINDER)
    public Object manageReminder(
            @ToolParam(description = "操作类型：LIST(列出全部)/UPDATE(更新)/DELETE(删除)/TOGGLE(启停)") String action,
            @ToolParam(description = ToolConstants.ToolParams.REMINDER_ID, required = false) Long reminderId,
            @ToolParam(description = ToolConstants.ToolParams.REMINDER_TITLE, required = false) String title,
            @ToolParam(description = ToolConstants.ToolParams.REMINDER_TIME, required = false) LocalTime reminderTime,
            @ToolParam(description = ToolConstants.ToolParams.REMINDER_TYPE, required = false) String reminderType,
            @ToolParam(description = ToolConstants.ToolParams.WEEKDAYS, required = false) String weekdays,
            @ToolParam(description = ToolConstants.ToolParams.REMINDER_ACTIVE, required = false) Boolean active) {

        Long userId = AgentConstants.DEFAULT_USER_ID;
        switch (action.trim().toUpperCase()) {
            case "LIST":
                log.info("Agent 列出提醒: userId={}", userId);
                return reminderService.list(userId).stream().map(ReminderResult::of).toList();
            case "UPDATE":
                ReminderCreateVO vo = new ReminderCreateVO();
                vo.setTitle(title);
                vo.setReminderTime(reminderTime);
                vo.setReminderType(reminderType != null ? ReminderType.valueOf(reminderType.trim().toUpperCase()) : null);
                vo.setWeekdays(weekdays);
                log.info("Agent 更新提醒: id={}", reminderId);
                return ReminderResult.of(reminderService.update(userId, reminderId, vo));
            case "DELETE":
                log.info("Agent 删除提醒: id={}", reminderId);
                reminderService.delete(userId, reminderId);
                return "提醒已删除: id=" + reminderId;
            case "TOGGLE":
                log.info("Agent 启停提醒: id={}, active={}", reminderId, active);
                return ReminderResult.of(reminderService.toggle(userId, reminderId, active));
            default:
                return "不支持的操作: " + action;
        }
    }
}
