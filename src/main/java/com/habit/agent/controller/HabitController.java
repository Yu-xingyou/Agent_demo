package com.habit.agent.controller;

import com.habit.agent.common.result.Result;
import com.habit.agent.common.vo.HabitRecordVO;
import com.habit.agent.entity.jpa.HabitRecord;
import com.habit.agent.service.HabitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 习惯记录 REST API（子模块 2-2，7 端点）
 *
 * 所有端点默认使用 DEFAULT_USER_ID=1（单用户演示场景）。
 */
@Slf4j
@Validated
@Tag(name = "习惯记录", description = "生活习惯打卡记录的录入、查询与删除")
@RestController
@RequestMapping("/api/habits")
@RequiredArgsConstructor
public class HabitController {

    private final HabitService habitService;

    /**
     * POST /api/habits — 录入/更新打卡
     * 同一天重复打卡为更新（uk_user_date 唯一约束）
     */
    @Operation(summary = "录入/更新打卡", description = "同一天重复打卡为更新（uk_user_date 唯一约束）")
    @PostMapping
    public Result<HabitRecordVO> saveOrUpdate(@Valid @RequestBody HabitRecord record) {
        log.info("录入打卡: date={}, sleepTime={}", record.getRecordDate(), record.getSleepTime());
        return Result.success(habitService.saveOrUpdate(record));
    }

    /**
     * GET /api/habits/today — 查询今日记录
     */
    @Operation(summary = "查询今日记录")
    @GetMapping("/today")
    public Result<HabitRecordVO> getTodayRecord() {
        return Result.success(habitService.getTodayRecord(null));
    }

    /**
     * GET /api/habits?startDate=&endDate= — 按日期范围查询
     */
    @Operation(summary = "按日期范围查询记录")
    @GetMapping
    public Result<List<HabitRecordVO>> getByDateRange(
            @Parameter(description = "开始日期", example = "2026-08-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期", example = "2026-08-02")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(habitService.getRecordsByDateRange(null, startDate, endDate));
    }

    /**
     * GET /api/habits/recent/{days} — 最近 N 天记录
     */
    @Operation(summary = "查询最近 N 天记录")
    @GetMapping("/recent/{days}")
    public Result<List<HabitRecordVO>> getRecent(@Parameter(description = "天数", example = "7") @PathVariable int days) {
        return Result.success(habitService.getRecentRecords(null, days));
    }

    /**
     * GET /api/habits/all — 所有记录
     */
    @Operation(summary = "查询所有记录")
    @GetMapping("/all")
    public Result<List<HabitRecordVO>> getAll() {
        return Result.success(habitService.getAllRecords(null));
    }

    /**
     * GET /api/habits/{id} — 按 ID 查询
     */
    @Operation(summary = "按 ID 查询记录")
    @GetMapping("/{id}")
    public Result<HabitRecordVO> getById(
            @Parameter(description = "记录ID", example = "1") @NotNull @PathVariable Long id) {
        return Result.success(habitService.getRecordById(id));
    }

    /**
     * DELETE /api/habits/{id} — 删除记录
     */
    @Operation(summary = "删除记录")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "记录ID", example = "1") @NotNull @PathVariable Long id) {
        habitService.deleteRecord(id);
        return Result.success();
    }
}
