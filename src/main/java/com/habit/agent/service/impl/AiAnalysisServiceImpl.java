package com.habit.agent.service.impl;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.entity.mongo.AiAnalysisTask;
import com.habit.agent.repository.mongo.AiAnalysisTaskRepository;
import com.habit.agent.service.AiAnalysisService;
import com.habit.agent.service.AnalysisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 阶段十（AI 智能分析）业务逻辑实现。
 *
 * <p>流程：
 * <ol>
 *   <li>{@link #submit} 落库 PENDING 任务并立即返回任务 ID；</li>
 *   <li>{@link #executeAsync}（@Async）聚合 AnalysisService 的图表数据，
 *       调用 LLM 生成 Markdown 报告与结论标签，更新任务状态为 COMPLETED/FAILED；</li>
 *   <li>前端轮询 {@code GET /api/ai-analysis/{id}} 获取状态与最终报告。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisServiceImpl implements AiAnalysisService {

    private final AiAnalysisTaskRepository taskRepository;
    private final AnalysisService analysisService;
    private final ChatClient chatClient;

    @Override
    public AiAnalysisTask submit(Long userId, int days) {
        Long uid = userId == null ? AgentConstants.DEFAULT_USER_ID : userId;
        AiAnalysisTask task = AiAnalysisTask.builder()
                .userId(uid)
                .days(days)
                .status(AiAnalysisTask.statusPending())
                .title(days + " 天生活习惯分析报告")
                .createTime(LocalDateTime.now())
                .build();
        AiAnalysisTask saved = taskRepository.save(task);
        executeAsync(saved.getId());
        return saved;
    }

    @Async("agentTaskExecutor")
    @Override
    public void executeAsync(String taskId) {
        AiAnalysisTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            log.warn("[AiAnalysis] 任务不存在，跳过 id={}", taskId);
            return;
        }
        task.setStatus(AiAnalysisTask.statusRunning());
        taskRepository.save(task);
        try {
            int days = task.getDays() == null ? 7 : task.getDays();
            // 1. 聚合图表数据
            Map<String, Object> charts = new LinkedHashMap<>();
            charts.put("trend", analysisService.getTrend(task.getUserId(), days));
            charts.put("overview", analysisService.getOverview(task.getUserId(), days));
            charts.put("achievement", analysisService.getAchievementRate(task.getUserId(), days));
            charts.put("radar", analysisService.getRadar(task.getUserId(), days));
            task.setCharts(charts);

            // 2. 生成报告
            String report = generateReport(task, charts);
            task.setReport(report);
            task.setTags(extractTags(report));
            task.setStatus(AiAnalysisTask.statusCompleted());
            task.setFinishTime(LocalDateTime.now());
            taskRepository.save(task);
            log.info("[AiAnalysis] 任务完成 id={} days={}", taskId, days);
        } catch (Exception e) {
            log.error("[AiAnalysis] 任务失败 id=" + taskId, e);
            task.setStatus(AiAnalysisTask.statusFailed());
            task.setError(truncate(e.getMessage(), 500));
            task.setFinishTime(LocalDateTime.now());
            taskRepository.save(task);
        }
    }

    @Override
    public AiAnalysisTask getTask(String taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new com.habit.agent.common.exception.BusinessException(
                        com.habit.agent.common.constant.AgentConstants.CODE_ANALYSIS_NOT_FOUND, "分析任务不存在"));
    }

    @Override
    public List<AiAnalysisTask> listHistory(Long userId, int limit) {
        Long uid = userId == null ? AgentConstants.DEFAULT_USER_ID : userId;
        List<AiAnalysisTask> all = taskRepository.findByUserIdOrderByCreateTimeDesc(uid);
        return all.size() <= limit ? all : all.subList(0, limit);
    }

    @Override
    public AiAnalysisTask latestCompleted(Long userId) {
        Long uid = userId == null ? AgentConstants.DEFAULT_USER_ID : userId;
        List<AiAnalysisTask> list = taskRepository.findTopByUserIdAndStatusOrderByCreateTimeDesc(
                uid, AiAnalysisTask.statusCompleted());
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public AiAnalysisTask regenerate(Long userId, int days) {
        return submit(userId, days);
    }

    private String generateReport(AiAnalysisTask task, Map<String, Object> charts) {
        String overviewJson = String.valueOf(charts.get("overview"));
        String achievementJson = String.valueOf(charts.get("achievement"));
        String prompt = String.format(
                "你是生活习惯助手的数据分析师。基于以下近 %d 天的真实分析数据，生成一份中文分析报告（Markdown 格式）：\n"
                        + "概览：%s\n达成率：%s\n"
                        + "要求：\n1. 开头用一句话总体评价；\n2. 用「## 各维度解读」分段说明睡眠/运动/饮水/饮食/心情；\n"
                        + "3. 用「## 改进建议」给出 3 条具体可执行的建议；\n4. 语气鼓励、客观，不编造数据。",
                task.getDays(), overviewJson, achievementJson);
        try {
            String content = chatClient.prompt()
                    .system("你是严谨的数据分析师，只依据给定数据输出 Markdown 报告，结论需可量化。")
                    .user(prompt)
                    .call()
                    .content();
            return content == null ? "报告生成失败，请重试。" : content;
        } catch (Exception e) {
            log.warn("[AiAnalysis] 报告 LLM 调用失败，降级", e);
            return "本次报告生成失败，但图表数据已就绪，可查看上方可视化图表。";
        }
    }

    private List<String> extractTags(String report) {
        // 简单启发式：提取「达标率」「偏低」「优秀」等关键词作为标签
        List<String> tags = new java.util.ArrayList<>();
        if (report == null) return tags;
        Map<String, String> keywords = Map.of(
                "睡眠", "睡眠", "运动", "运动", "饮水", "饮水", "饮食", "饮食", "心情", "心情",
                "偏低", "需改善", "不足", "需改善", "优秀", "表现优秀", "达标", "达标良好");
        for (Map.Entry<String, String> e : keywords.entrySet()) {
            if (report.contains(e.getKey()) && !tags.contains(e.getValue())) {
                tags.add(e.getValue());
            }
            if (tags.size() >= 5) break;
        }
        return tags;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
