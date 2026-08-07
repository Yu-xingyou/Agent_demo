package com.habit.agent.aigc.repository;

import com.habit.agent.aigc.entity.mongo.AiAnalysisDoc;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * AI 分析仓储（MongoDB aiAnalysis）
 */
public interface AiAnalysisRepository extends MongoRepository<AiAnalysisDoc, String> {

    List<AiAnalysisDoc> findByUserIdOrderByCreateTimeDesc(Long userId);

    List<AiAnalysisDoc> findByUserIdAndTypeOrderByCreateTimeDesc(Long userId, String type);

    Optional<AiAnalysisDoc> findTopByUserIdOrderByCreateTimeDesc(Long userId);

    Optional<AiAnalysisDoc> findTopByUserIdAndTypeOrderByCreateTimeDesc(Long userId, String type);
}
