package com.habit.agent.service;

import java.util.List;
import java.util.Map;

/**
 * AI 智能分析服务接口（结构化报告，存 MongoDB aiAnalysis）
 */
public interface AiAnalysisService {

    /**
     * 生成分析报告（异步，返回 PROCESSING 任务，前端轮询）
     *
     * @param days 分析天数
     */
    Map<String, Object> generate(int days);

    /**
     * 按任务 id 查询分析任务
     */
    Map<String, Object> getById(String id);

    /**
     * 历史分析列表（按创建时间倒序）
     *
     * @param limit 条数
     */
    List<Map<String, Object>> history(int limit);

    /**
     * 最新已完成分析
     */
    Map<String, Object> getLatest();

    /**
     * 重新生成分析报告
     *
     * @param days 分析天数
     */
    Map<String, Object> regenerate(int days);
}
