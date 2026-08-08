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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.habit.agent.common.result.Result;
import com.habit.agent.common.vo.HabitGoalRecordVO;
import com.habit.agent.entity.jpa.HabitGoalRecord;
import com.habit.agent.service.HabitGoalRecordService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 自定义目标打卡记录 REST API（从 GoalController 拆分，避免双基路径 /api/goals 与 /api/goal-records 交叉注册）。
 *
 * 前端路径保持不变：/api/goal-records/records、/api/goal-records/records/today 等。
 */
@Slf4j
@Validated
@Tag(name = "自定义目标打卡", description = "自定义目标打卡记录的录入与查询")
@RestController
@RequestMapping("/api/goal-records")
@RequiredArgsConstructor
public class GoalRecordController {

    private final HabitGoalRecordService goalRecordService;

    /**
     * POST /api/goal-records/records — 录入/更新自定义目标打卡
     *
     * @param record 自定义目标打卡实体（含 goalId/recordDate/完成状态等）
     * @return 保存后的打卡记录视图
     */
    @Operation(summary = "录入/更新自定义目标打卡")
    @PostMapping("/records")
    public Result<HabitGoalRecordVO> saveRecord(@Valid @RequestBody HabitGoalRecord record) {
        log.info("录入自定义目标打卡: goalId={}, date={}", record.getGoalId(), record.getRecordDate());
        return Result.success(goalRecordService.saveOrUpdate(record));
    }

    /**
     * GET /api/goal-records/records/today — 查询今日所有自定义目标打卡
     *
     * @return 今日（默认用户）自定义目标打卡记录视图列表
     */
    @Operation(summary = "查询今日自定义目标打卡")
    @GetMapping("/records/today")
    public Result<List<HabitGoalRecordVO>> getTodayRecords() {
        return Result.success(goalRecordService.getByDate(null, LocalDate.now()));
    }

    /**
     * GET /api/goal-records/records?startDate=&endDate= — 按日期范围查询
     *
     * @param startDate 开始日期（含），可选
     * @param endDate   结束日期（含），可选
     * @return 日期范围内的打卡记录视图列表；未提供完整范围时返回最近 30 天
     */
    @Operation(summary = "按日期范围查询自定义目标打卡")
    @GetMapping("/records")
    public Result<List<HabitGoalRecordVO>> getRecords(
            @Parameter(description = "开始日期", example = "2026-08-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期", example = "2026-08-02")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate != null && endDate != null) {
            return Result.success(goalRecordService.getByDateRange(null, startDate, endDate));
        }
        return Result.success(goalRecordService.getRecent(null, 30));
    }

    /**
     * GET /api/goal-records/records/recent/{days} — 最近 N 天
     *
     * @param days 天数
     * @return 最近 N 天自定义目标打卡记录视图列表
     */
    @Operation(summary = "查询最近 N 天自定义目标打卡")
    @GetMapping("/records/recent/{days}")
    public Result<List<HabitGoalRecordVO>> getRecent(
            @Parameter(description = "天数", example = "7") @PathVariable int days) {
        return Result.success(goalRecordService.getRecent(null, days));
    }

    /**
     * GET /api/goal-records/records/goal/{goalId}/recent/{days} — 按目标查询最近 N 天
     *
     * @param goalId 目标 id
     * @param days   天数
     * @return 该目标最近 N 天打卡记录视图列表
     */
    @Operation(summary = "按目标查询最近 N 天打卡")
    @GetMapping("/records/goal/{goalId}/recent/{days}")
    public Result<List<HabitGoalRecordVO>> getByGoalRecent(
            @Parameter(description = "目标ID", example = "1") @NotNull @PathVariable Long goalId,
            @Parameter(description = "天数", example = "7") @PathVariable int days) {
        return Result.success(goalRecordService.getByGoalIdRecent(goalId, days));
    }
}
