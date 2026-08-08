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

    /**
     * 创建提醒（新增一条打卡提醒）
     *
     * @param userId 用户 id，可选（空时使用默认用户）
     * @param vo     提醒创建视图（时间/重复规则/类型等）
     * @return 创建成功的提醒实体
     */
    @PostMapping
    @Operation(summary = "创建提醒", description = "新增一条打卡提醒")
    public Result<Reminder> create(
            @RequestParam(required = false) Long userId,
            @Valid @RequestBody ReminderCreateVO vo) {
        return Result.success(reminderService.create(userId, vo));
    }

    /**
     * 更新提醒（按 ID 全字段覆盖）
     *
     * @param userId 用户 id，可选（空时使用默认用户）
     * @param id     提醒 id
     * @param vo     更新后的提醒视图
     * @return 更新后的提醒实体
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新提醒", description = "按 ID 更新提醒的全部字段")
    public Result<Reminder> update(
            @RequestParam(required = false) Long userId,
            @PathVariable Long id,
            @Valid @RequestBody ReminderCreateVO vo) {
        return Result.success(reminderService.update(userId, id, vo));
    }

    /**
     * 删除提醒
     *
     * @param userId 用户 id，可选（空时使用默认用户）
     * @param id     提醒 id
     * @return 统一成功响应
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除提醒", description = "按 ID 删除提醒")
    public Result<Void> delete(
            @RequestParam(required = false) Long userId,
            @PathVariable Long id) {
        reminderService.delete(userId, id);
        return Result.success();
    }

    /**
     * 提醒列表
     *
     * @param userId 用户 id，可选（空时使用默认用户）
     * @return 该用户全部提醒实体列表（倒序）
     */
    @GetMapping
    @Operation(summary = "提醒列表", description = "返回当前用户全部提醒（倒序）")
    public Result<List<Reminder>> list(
            @RequestParam(required = false) Long userId) {
        Long uid = userId == null ? AgentConstants.DEFAULT_USER_ID : userId;
        return Result.success(reminderService.list(uid));
    }

    /**
     * 启用/停用（切换提醒的启用状态）
     *
     * @param userId 用户 id，可选（空时使用默认用户）
     * @param id     提醒 id
     * @param active 目标启用状态（true=启用，false=停用）
     * @return 切换后的提醒实体
     */
    @PutMapping("/{id}/toggle")
    @Operation(summary = "启用/停用", description = "切换提醒的启用状态")
    public Result<Reminder> toggle(
            @RequestParam(required = false) Long userId,
            @PathVariable Long id,
            @RequestParam Boolean active) {
        return Result.success(reminderService.toggle(userId, id, active));
    }
}
