package com.habit.agent.controller;

import com.habit.agent.common.result.Result;
import com.habit.agent.common.vo.HabitGoalVO;
import com.habit.agent.entity.jpa.HabitGoal;
import com.habit.agent.service.GoalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 习惯目标 REST API（子模块 2-2，6 端点）
 */
@Slf4j
@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    /**
     * POST /api/goals — 创建目标（同类型重复报 40901）
     */
    @PostMapping
    public Result<HabitGoalVO> create(@RequestBody HabitGoal goal) {
        log.info("创建目标: type={}", goal.getGoalType());
        return Result.success(goalService.saveGoal(goal));
    }

    /**
     * GET /api/goals — 查询所有目标
     */
    @GetMapping
    public Result<List<HabitGoalVO>> getAll() {
        return Result.success(goalService.getAllGoals(null));
    }

    /**
     * GET /api/goals/active — 查询启用目标
     */
    @GetMapping("/active")
    public Result<List<HabitGoalVO>> getActive() {
        return Result.success(goalService.getActiveGoals(null));
    }

    /**
     * GET /api/goals/{type} — 按类型查询目标
     */
    @GetMapping("/{type}")
    public Result<HabitGoalVO> getByType(@PathVariable String type) {
        HabitGoal.GoalType goalType = HabitGoal.GoalType.valueOf(type.toUpperCase());
        return Result.success(goalService.getGoalByType(null, goalType));
    }

    /**
     * PUT /api/goals/{id} — 更新目标
     */
    @PutMapping("/{id}")
    public Result<HabitGoalVO> update(@PathVariable Long id, @RequestBody HabitGoal goalUpdate) {
        log.info("更新目标: id={}", id);
        return Result.success(goalService.updateGoal(id, goalUpdate));
    }

    /**
     * DELETE /api/goals/{id} — 删除目标
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        goalService.deleteGoal(id);
        return Result.success();
    }
}
