package com.habit.agent.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.habit.agent.common.result.Result;
import com.habit.agent.common.vo.HabitGoalVO;
import com.habit.agent.entity.jpa.HabitGoal;
import com.habit.agent.service.GoalService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 习惯目标 REST API（目标管理：CRUD + 自定义目标支持）。
 *
 * 自定义目标打卡记录已拆分到 {@link GoalRecordController}，避免双基路径交叉注册。
 */
@Slf4j
@Validated
@Tag(name = "习惯目标", description = "习惯目标管理(CRUD + 自定义目标)")
@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

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
}
