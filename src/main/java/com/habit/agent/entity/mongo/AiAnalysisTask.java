package com.habit.agent.entity.mongo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.habit.agent.common.constant.AgentConstants;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 阶段十（AI 智能分析）异步分析任务实体（自管集合 {@code aiAnalysisTask}）。
 *
 * <p>一次分析报告对应一个任务，状态机：{@code PENDING -> RUNNING -> COMPLETED / FAILED}。
 * 报告文本与可视化图表数据（趋势/雷达/达成率）一并持久化，供前端报告页与历史列表消费。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "aiAnalysisTask")
public class AiAnalysisTask {

    @Id
    private String id;

    /** 用户 ID（默认单用户） */
    @Field("userId")
    private Long userId;

    /** 分析周期（天） */
    @Field("days")
    private Integer days;

    /** 任务状态：PENDING / RUNNING / COMPLETED / FAILED */
    @Field("status")
    private String status;

    /** 分析标题（前端历史列表展示） */
    @Field("title")
    private String title;

    /** AI 生成的报告正文（Markdown） */
    @Field("report")
    private String report;

    /** 可视化图表数据：趋势、雷达、达成率（聚合 AnalysisService 输出） */
    @Field("charts")
    private Map<String, Object> charts;

    /** 关键结论标签（如「睡眠达标率偏低」） */
    @Field("tags")
    private List<String> tags;

    /** 每日/总体评价（结构化输出） */
    @Field("dailyEvaluation")
    private String dailyEvaluation;

    /** 周期趋势总结（结构化输出） */
    @Field("trendSummary")
    private String trendSummary;

    /** 生活习惯风险提示（结构化输出） */
    @Field("riskWarning")
    private String riskWarning;

    /** 改进建议，多条以换行分隔（结构化输出） */
    @Field("suggestion")
    private String suggestion;

    /** 综合评分 0-100（结构化输出） */
    @Field("score")
    private Integer score;

    /** 失败原因（status=FAILED 时填充） */
    @Field("error")
    private String error;

    /** 创建时间 */
    @Field("createTime")
    private LocalDateTime createTime;

    /** 完成时间 */
    @Field("finishTime")
    private LocalDateTime finishTime;

    public static Long defaultUserId() {
        return AgentConstants.DEFAULT_USER_ID;
    }

    public static String statusPending() {
        return "PENDING";
    }

    public static String statusRunning() {
        return "RUNNING";
    }

    public static String statusCompleted() {
        return "COMPLETED";
    }

    public static String statusFailed() {
        return "FAILED";
    }
}
