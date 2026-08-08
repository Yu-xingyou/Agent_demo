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
     * @return 新建的分析任务信息（状态 PROCESSING）
     */
    Map<String, Object> generate(int days);

    /**
     * 按任务 id 查询分析任务
     *
     * @param id 分析任务 id
     * @return 分析任务详情
     */
    Map<String, Object> getById(String id);

    /**
     * 历史分析列表（按创建时间倒序）
     *
     * @param limit 返回条数上限
     * @return 历史分析报告摘要列表
     */
    List<Map<String, Object>> history(int limit);

    /**
     * 最新已完成分析
     *
     * @return 当前用户最新一条已完成的分析报告；无则空数据
     */
    Map<String, Object> getLatest();

    /**
     * 重新生成分析报告
     *
     * @param days 分析天数
     * @return 重建的分析任务信息（状态 PROCESSING）
     */
    Map<String, Object> regenerate(int days);
}
