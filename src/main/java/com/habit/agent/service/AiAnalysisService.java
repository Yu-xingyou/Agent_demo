package com.habit.agent.service;

import java.util.List;

import com.habit.agent.entity.mongo.AiAnalysisTask;

/**
 * 阶段十（AI 智能分析）业务接口。
 *
 * <p>负责异步生成可视化分析报告：聚合多维图表数据（趋势/雷达/达成率），
 * 交由 LLM 生成 Markdown 报告文本与关键结论标签，并持久化到 MongoDB。
 */
public interface AiAnalysisService {

    /**
     * 提交分析任务（异步生成），立即返回任务 ID。
     *
     * @param userId 用户 ID（默认单用户）
     * @param days   分析周期（天）
     * @return 新建的任务实体（状态 PENDING）
     */
    AiAnalysisTask submit(Long userId, int days);

    /**
     * 异步执行分析（内部调用，由 {@link #submit} 触发）。
     *
     * @param taskId 任务 ID
     */
    void executeAsync(String taskId);

    /**
     * 查询任务状态与结果。
     *
     * @param taskId 任务 ID
     * @return 任务实体
     */
    AiAnalysisTask getTask(String taskId);

    /**
     * 查询用户的分析历史列表（按创建时间倒序）。
     *
     * @param userId 用户 ID
     * @param limit  返回条数上限
     * @return 任务列表
     */
    List<AiAnalysisTask> listHistory(Long userId, int limit);

    /**
     * 查询用户最近一次已完成的分析报告。
     *
     * @param userId 用户 ID
     * @return 任务实体（无则 null）
     */
    AiAnalysisTask latestCompleted(Long userId);

    /**
     * 重新生成：针对同一周期新建任务（保留历史）。
     *
     * @param userId 用户 ID
     * @param days   分析周期
     * @return 新建的任务实体
     */
    AiAnalysisTask regenerate(Long userId, int days);
}
