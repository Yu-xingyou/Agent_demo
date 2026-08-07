package com.habit.agent.controller;

import com.habit.agent.common.result.Result;
import com.habit.agent.service.AiAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * AI 智能分析接口（对齐前端 aiAnalysis.js）
 */
@RestController
@RequestMapping("/ai-analysis")
@RequiredArgsConstructor
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    /**
     * 生成分析报告（异步，返回 PROCESSING 任务）
     */
    @PostMapping("/generate")
    public Result<Map<String, Object>> generate(@RequestParam(defaultValue = "7") int days) {
        return Result.success(aiAnalysisService.generate(days));
    }

    /**
     * 重新生成分析报告
     */
    @PostMapping("/regenerate")
    public Result<Map<String, Object>> regenerate(@RequestParam(defaultValue = "7") int days) {
        return Result.success(aiAnalysisService.regenerate(days));
    }

    /**
     * 查询分析任务
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getById(@PathVariable("id") String id) {
        return Result.success(aiAnalysisService.getById(id));
    }

    /**
     * 历史分析列表
     */
    @GetMapping("/history")
    public Result<List<Map<String, Object>>> history(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(aiAnalysisService.history(limit));
    }

    /**
     * 最新已完成分析
     */
    @GetMapping("/latest")
    public Result<Map<String, Object>> getLatest() {
        return Result.success(aiAnalysisService.getLatest());
    }
}
