package com.habit.agent.aigc.repository;

import com.habit.agent.aigc.entity.mongo.AiAnalysisDoc;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * AI 分析仓储（MongoDB aiAnalysis）
 */
public interface AiAnalysisRepository extends MongoRepository<AiAnalysisDoc, String> {

    /**
     * 按用户 id 查询全部分析报告（按创建时间倒序）
     *
     * @param userId 用户 id
     * @return 分析报告文档列表
     */
    List<AiAnalysisDoc> findByUserIdOrderByCreateTimeDesc(Long userId);

    /**
     * 按用户 id 与类型查询分析报告（按创建时间倒序）
     *
     * @param userId 用户 id
     * @param type   分析类型（DAILY/WEEKLY/MONTHLY/CUSTOM）
     * @return 符合条件的分析报告文档列表
     */
    List<AiAnalysisDoc> findByUserIdAndTypeOrderByCreateTimeDesc(Long userId, String type);

    /**
     * 查询用户最新一条分析报告
     *
     * @param userId 用户 id
     * @return 最新报告文档；不存在时返回空 Optional
     */
    Optional<AiAnalysisDoc> findTopByUserIdOrderByCreateTimeDesc(Long userId);

    /**
     * 查询用户指定类型的最新一条分析报告
     *
     * @param userId 用户 id
     * @param type   分析类型（DAILY/WEEKLY/MONTHLY/CUSTOM）
     * @return 最新报告文档；不存在时返回空 Optional
     */
    Optional<AiAnalysisDoc> findTopByUserIdAndTypeOrderByCreateTimeDesc(Long userId, String type);
}
