package com.habit.agent.aigc.entity.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 智能分析文档（本地 MongoDB aiAnalysis 集合）
 * 存储 AI 生成的结构化分析报告（综合评分/今日点评/趋势/风险/建议）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "aiAnalysis")
@CompoundIndex(name = "idx_user_type_time", def = "{'userId': 1, 'type': 1, 'createTime': -1}")
public class AiAnalysisDoc implements Serializable {

    @Id
    private String id;

    /** 用户 id */
    @Indexed
    private Long userId;

    /** 分析类型：DAILY / WEEKLY / MONTHLY / CUSTOM */
    private String type;

    /** 分析日期（yyyy-MM-dd） */
    @Indexed
    private String recordDate;

    /** 分析天数窗口 */
    private Integer days;

    /** 报告标题 */
    private String title;

    /** 状态：PROCESSING / COMPLETED / FAILED */
    private String status;

    /** 失败原因（status=FAILED 时） */
    private String error;

    /** 报告内容（JSON 字符串，含 score/dailyEvaluation/trendSummary/riskWarning/suggestion/report/charts） */
    private String content;

    /** 创建时间 */
    private LocalDateTime createTime;
}
