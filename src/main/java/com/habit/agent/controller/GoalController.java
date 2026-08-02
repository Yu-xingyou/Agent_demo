package com.habit.agent.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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
@RestController
@RequestMapping({"/api/goals", "/api/goal-records"})
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;
    private final HabitGoalRecordService goalRecordService;

    // ===== 目标 CRUD =====

    @PostMapping
    public Result<HabitGoalVO> create(@RequestBody HabitGoal goal) {
        log.info("创建目标: type={}, customName={}", goal.getGoalType(), goal.getCustomName());
        return Result.success(goalService.saveGoal(goal));
    }

    @GetMapping
    public Result<List<HabitGoalVO>> getAll() {
        return Result.success(goalService.getAllGoals(null));
    }

    @GetMapping("/active")
    public Result<List<HabitGoalVO>> getActive() {
        return Result.success(goalService.getActiveGoals(null));
    }

    @GetMapping("/active-with-custom")
    public Result<List<HabitGoalVO>> getActiveWithCustom() {
        return Result.success(goalService.getActiveGoalsWithCustom(null));
    }

    @GetMapping("/{id}")
    public Result<HabitGoalVO> getById(@PathVariable Long id) {
        return Result.success(goalService.getGoalByType(null, null) != null
                ? null : null);
    }

    @GetMapping("/type/{type}")
    public Result<HabitGoalVO> getByType(@PathVariable String type) {
        HabitGoal.GoalType goalType = HabitGoal.GoalType.valueOf(type.toUpperCase());
        return Result.success(goalService.getGoalByType(null, goalType));
    }

    @PutMapping("/{id}")
    public Result<HabitGoalVO> update(@PathVariable Long id, @RequestBody HabitGoal goalUpdate) {
        log.info("更新目标: id={}", id);
        return Result.success(goalService.updateGoal(id, goalUpdate));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        goalService.deleteGoal(id);
        return Result.success();
    }

    // ===== 自定义目标打卡记录 =====

    /**
     * POST /api/goal-records — 录入/更新自定义目标打卡
     */
    @PostMapping("/records")
    public Result<HabitGoalRecordVO> saveRecord(@RequestBody HabitGoalRecord record) {
        log.info("录入自定义目标打卡: goalId={}, date={}", record.getGoalId(), record.getRecordDate());
        return Result.success(goalRecordService.saveOrUpdate(record));
    }

    /**
     * GET /api/goal-records/today — 查询今日所有自定义目标打卡
     */
    @GetMapping("/records/today")
    public Result<List<HabitGoalRecordVO>> getTodayRecords() {
        return Result.success(goalRecordService.getByDate(null, LocalDate.now()));
    }

    /**
     * GET /api/goal-records?startDate=&endDate= — 按日期范围查询
     */
    @GetMapping("/records")
    public Result<List<HabitGoalRecordVO>> getRecords(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate != null && endDate != null) {
            // 直接查询指定日期范围
            return Result.success(goalRecordService.getRecent(null,
                    (int) (endDate.toEpochDay() - startDate.toEpochDay() + 1)));
        }
        return Result.success(goalRecordService.getRecent(null, 30));
    }

    /**
     * GET /api/goal-records/recent/{days} — 最近 N 天
     */
    @GetMapping("/records/recent/{days}")
    public Result<List<HabitGoalRecordVO>> getRecent(@PathVariable int days) {
        return Result.success(goalRecordService.getRecent(null, days));
    }

    /**
     * GET /api/goal-records/goal/{goalId}/recent/{days} — 按目标查询最近 N 天
     */
    @GetMapping("/records/goal/{goalId}/recent/{days}")
    public Result<List<HabitGoalRecordVO>> getByGoalRecent(@PathVariable Long goalId, @PathVariable int days) {
        return Result.success(goalRecordService.getByGoalIdRecent(goalId, days));
    }
}
