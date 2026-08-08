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
     *
     * @param days 分析的天数窗口，默认 7
     * @return 新建的分析任务信息（状态为 PROCESSING）
     */
    @PostMapping("/generate")
    public Result<Map<String, Object>> generate(@RequestParam(defaultValue = "7") int days) {
        return Result.success(aiAnalysisService.generate(days));
    }

    /**
     * 重新生成分析报告
     *
     * @param days 分析的天数窗口，默认 7
     * @return 重建的分析任务信息（状态为 PROCESSING）
     */
    @PostMapping("/regenerate")
    public Result<Map<String, Object>> regenerate(@RequestParam(defaultValue = "7") int days) {
        return Result.success(aiAnalysisService.regenerate(days));
    }

    /**
     * 查询分析任务
     *
     * @param id 分析任务 id（路径参数）
     * @return 分析任务详情
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getById(@PathVariable("id") String id) {
        return Result.success(aiAnalysisService.getById(id));
    }

    /**
     * 历史分析列表
     *
     * @param limit 返回条数上限，默认 10
     * @return 历史分析报告摘要列表
     */
    @GetMapping("/history")
    public Result<List<Map<String, Object>>> history(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(aiAnalysisService.history(limit));
    }

    /**
     * 最新已完成分析
     *
     * @return 当前用户最新一条已完成的分析报告；不存在时返回空数据
     */
    @GetMapping("/latest")
    public Result<Map<String, Object>> getLatest() {
        return Result.success(aiAnalysisService.getLatest());
    }
}
