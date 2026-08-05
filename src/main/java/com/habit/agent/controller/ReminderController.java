package com.habit.agent.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.common.result.Result;
import com.habit.agent.common.vo.ReminderCreateVO;
import com.habit.agent.entity.jpa.Reminder;
import com.habit.agent.service.ReminderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 阶段十（打卡提醒）接口。
 *
 * <p>提供提醒的增删改查与启用开关，配合前端「提醒设置」页与定时推送（拓展计划）。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reminder")
@Tag(name = "打卡提醒", description = "提醒的创建/更新/删除/查询与开关")
public class ReminderController {

    private final ReminderService reminderService;

    @PostMapping
    @Operation(summary = "创建提醒", description = "新增一条打卡提醒")
    public Result<Reminder> create(
            @RequestParam(required = false) Long userId,
            @Valid @RequestBody ReminderCreateVO vo) {
        return Result.success(reminderService.create(userId, vo));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新提醒", description = "按 ID 更新提醒的全部字段")
    public Result<Reminder> update(
            @RequestParam(required = false) Long userId,
            @PathVariable Long id,
            @Valid @RequestBody ReminderCreateVO vo) {
        return Result.success(reminderService.update(userId, id, vo));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除提醒", description = "按 ID 删除提醒")
    public Result<Void> delete(
            @RequestParam(required = false) Long userId,
            @PathVariable Long id) {
        reminderService.delete(userId, id);
        return Result.success();
    }

    @GetMapping
    @Operation(summary = "提醒列表", description = "返回当前用户全部提醒（倒序）")
    public Result<List<Reminder>> list(
            @RequestParam(required = false) Long userId) {
        Long uid = userId == null ? AgentConstants.DEFAULT_USER_ID : userId;
        return Result.success(reminderService.list(uid));
    }

    @PutMapping("/{id}/toggle")
    @Operation(summary = "启用/停用", description = "切换提醒的启用状态")
    public Result<Reminder> toggle(
            @RequestParam(required = false) Long userId,
            @PathVariable Long id,
            @RequestParam Boolean active) {
        return Result.success(reminderService.toggle(userId, id, active));
    }
}
