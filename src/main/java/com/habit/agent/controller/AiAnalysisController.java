package com.habit.agent.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.common.result.Result;
import com.habit.agent.entity.mongo.AiAnalysisTask;
import com.habit.agent.service.AiAnalysisService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 阶段十（AI 智能分析）接口。
 *
 * <p>异步生成可视化分析报告：提交后轮询状态，完成后获取 Markdown 报告与图表数据。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai-analysis")
@Tag(name = "AI 智能分析", description = "异步生成可视化分析报告（趋势/雷达/达成率 + AI 结论）")
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    @PostMapping("/generate")
    @Operation(summary = "提交分析任务", description = "异步生成近 days 天的分析报告，立即返回任务 ID，前端轮询状态")
    public Result<AiAnalysisTask> generate(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "7") int days) {
        return Result.success(aiAnalysisService.submit(userId, days));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询任务状态/结果", description = "按任务 ID 返回状态（PENDING/RUNNING/COMPLETED/FAILED）与报告/图表")
    public Result<AiAnalysisTask> getTask(@PathVariable String id) {
        return Result.success(aiAnalysisService.getTask(id));
    }

    @GetMapping("/history")
    @Operation(summary = "分析历史列表", description = "返回当前用户的分析任务历史（倒序，最多 limit 条）")
    public Result<List<AiAnalysisTask>> history(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        Long uid = userId == null ? AgentConstants.DEFAULT_USER_ID : userId;
        return Result.success(aiAnalysisService.listHistory(uid, limit));
    }

    @GetMapping("/latest")
    @Operation(summary = "最近一次报告", description = "返回当前用户最近一次已完成的分析报告")
    public Result<AiAnalysisTask> latest(
            @RequestParam(required = false) Long userId) {
        Long uid = userId == null ? AgentConstants.DEFAULT_USER_ID : userId;
        return Result.success(aiAnalysisService.latestCompleted(uid));
    }

    @PostMapping("/{id}/regenerate")
    @Operation(summary = "重新生成", description = "针对同一周期新建分析任务（保留历史），返回新任务 ID")
    public Result<AiAnalysisTask> regenerate(
            @PathVariable String id,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "7") int days) {
        // 忽略路径 id（仅用于语义），按周期重新提交
        return Result.success(aiAnalysisService.regenerate(userId, days));
    }
}
