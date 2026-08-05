package com.habit.agent.repository.mongo;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.habit.agent.entity.mongo.AiAnalysisTask;

/**
 * 阶段十（AI 智能分析）任务持久化 Repository（自管集合 {@code aiAnalysisTask}）。
 */
public interface AiAnalysisTaskRepository extends MongoRepository<AiAnalysisTask, String> {

    List<AiAnalysisTask> findByUserIdOrderByCreateTimeDesc(Long userId);

    List<AiAnalysisTask> findByUserIdAndStatus(Long userId, String status);

    List<AiAnalysisTask> findTopByUserIdAndStatusOrderByCreateTimeDesc(Long userId, String status);
}
