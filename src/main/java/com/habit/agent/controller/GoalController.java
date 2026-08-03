package com.habit.agent.controller;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.habit.agent.common.result.Result;
import com.habit.agent.common.vo.HabitGoalRecordVO;
import com.habit.agent.common.vo.HabitGoalVO;
import com.habit.agent.entity.jpa.HabitGoal;
import com.habit.agent.entity.jpa.HabitGoalRecord;
import com.habit.agent.service.GoalService;
import com.habit.agent.service.HabitGoalRecordService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 习惯目标 REST API
 *
 * 目标管理：CRUD + 自定义目标支持
 * 自定义目标打卡记录：录入/查询
 */
@Slf4j
@Validated
@Tag(name = "习惯目标", description = "习惯目标管理(CRUD + 自定义目标) 与 自定义目标打卡记录")
@RestController
@RequestMapping({"/api/goals", "/api/goal-records"})
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;
    private final HabitGoalRecordService goalRecordService;

    // ===== 目标 CRUD =====

    @Operation(summary = "创建目标", description = "支持内置类型与自定义类型")
    @PostMapping
    public Result<HabitGoalVO> create(@Valid @RequestBody HabitGoalVO goal) {
        log.info("创建目标: type={}, customName={}", goal.getGoalType(), goal.getCustomName());
        return Result.success(goalService.saveGoal(goal));
    }

    @Operation(summary = "查询所有目标")
    @GetMapping
    public Result<List<HabitGoalVO>> getAll() {
        return Result.success(goalService.getAllGoals(null));
    }

    @Operation(summary = "查询启用中的目标")
    @GetMapping("/active")
    public Result<List<HabitGoalVO>> getActive() {
        return Result.success(goalService.getActiveGoals(null));
    }

    @Operation(summary = "查询启用中的目标(含自定义)")
    @GetMapping("/active-with-custom")
    public Result<List<HabitGoalVO>> getActiveWithCustom() {
        return Result.success(goalService.getActiveGoalsWithCustom(null));
    }

    @Operation(summary = "按 ID 查询目标")
    @GetMapping("/{id}")
    public Result<HabitGoalVO> getById(
            @Parameter(description = "目标ID", example = "1") @NotNull @PathVariable Long id) {
        return Result.success(goalService.getGoalById(id));
    }

    @Operation(summary = "按类型查询目标")
    @GetMapping("/type/{type}")
    public Result<HabitGoalVO> getByType(
            @Parameter(description = "目标类型(SLEEP/EXERCISE/WATER/DIET/CUSTOM)", example = "EXERCISE") @PathVariable String type) {
        HabitGoal.GoalType goalType = HabitGoal.GoalType.valueOf(type.toUpperCase());
        return Result.success(goalService.getGoalByType(null, goalType));
    }

    @Operation(summary = "更新目标")
    @PutMapping("/{id}")
    public Result<HabitGoalVO> update(
            @Parameter(description = "目标ID", example = "1") @NotNull @PathVariable Long id,
            @Valid @RequestBody HabitGoalVO goalUpdate) {
        log.info("更新目标: id={}", id);
        return Result.success(goalService.updateGoal(id, goalUpdate));
    }

    @Operation(summary = "删除目标")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "目标ID", example = "1") @NotNull @PathVariable Long id) {
        goalService.deleteGoal(id);
        return Result.success();
    }

    // ===== 自定义目标打卡记录 =====

    /**
     * POST /api/goal-records — 录入/更新自定义目标打卡
     */
    @Operation(summary = "录入/更新自定义目标打卡")
    @PostMapping("/records")
    public Result<HabitGoalRecordVO> saveRecord(@Valid @RequestBody HabitGoalRecord record) {
        log.info("录入自定义目标打卡: goalId={}, date={}", record.getGoalId(), record.getRecordDate());
        return Result.success(goalRecordService.saveOrUpdate(record));
    }

    /**
     * GET /api/goal-records/today — 查询今日所有自定义目标打卡
     */
    @Operation(summary = "查询今日自定义目标打卡")
    @GetMapping("/records/today")
    public Result<List<HabitGoalRecordVO>> getTodayRecords() {
        return Result.success(goalRecordService.getByDate(null, LocalDate.now()));
    }

    /**
     * GET /api/goal-records?startDate=&endDate= — 按日期范围查询
     */
    @Operation(summary = "按日期范围查询自定义目标打卡")
    @GetMapping("/records")
    public Result<List<HabitGoalRecordVO>> getRecords(
            @Parameter(description = "开始日期", example = "2026-08-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期", example = "2026-08-02")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate != null && endDate != null) {
            // 按精确日期范围查询
            return Result.success(goalRecordService.getByDateRange(null, startDate, endDate));
        }
        return Result.success(goalRecordService.getRecent(null, 30));
    }

    /**
     * GET /api/goal-records/recent/{days} — 最近 N 天
     */
    @Operation(summary = "查询最近 N 天自定义目标打卡")
    @GetMapping("/records/recent/{days}")
    public Result<List<HabitGoalRecordVO>> getRecent(
            @Parameter(description = "天数", example = "7") @PathVariable int days) {
        return Result.success(goalRecordService.getRecent(null, days));
    }

    /**
     * GET /api/goal-records/goal/{goalId}/recent/{days} — 按目标查询最近 N 天
     */
    @Operation(summary = "按目标查询最近 N 天打卡")
    @GetMapping("/records/goal/{goalId}/recent/{days}")
    public Result<List<HabitGoalRecordVO>> getByGoalRecent(
            @Parameter(description = "目标ID", example = "1") @NotNull @PathVariable Long goalId,
            @Parameter(description = "天数", example = "7") @PathVariable int days) {
        return Result.success(goalRecordService.getByGoalIdRecent(goalId, days));
    }
}
