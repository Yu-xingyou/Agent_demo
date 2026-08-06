package com.habit.agent.service.impl;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final ChatClient reportChatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

            // 2. 生成结构化分析报告（每日评价/趋势总结/风险提示/建议/评分 + Markdown 正文）
            generateStructured(task, charts);
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

    /**
     * 生成结构化分析报告：让 LLM 直接返回 JSON（每日评价/趋势总结/风险提示/建议/评分/正文），
     * 解析失败时降级为纯 Markdown。所有结论以结构化字段存储，满足「输出数据」要求。
     */
    private void generateStructured(AiAnalysisTask task, Map<String, Object> charts) {
        String overviewJson = String.valueOf(charts.get("overview"));
        String achievementJson = String.valueOf(charts.get("achievement"));
        String prompt = String.format(
                "你是生活习惯助手的数据分析师。基于以下近 %d 天的真实分析数据生成中文分析。\n"
                        + "概览：%s\n达成率：%s\n"
                        + "请只返回一个 JSON 对象，不要使用 markdown 代码块、不要包含任何额外说明文字。JSON 字段：\n"
                        + "dailyEvaluation: 一句话总体/每日评价\n"
                        + "trendSummary: 周期趋势总结（2-3 句）\n"
                        + "riskWarning: 生活习惯风险提示（2-3 句，若无风险写「暂无明显风险」）\n"
                        + "suggestion: 3 条具体可执行的改进建议（用换行 \\n 分隔）\n"
                        + "score: 0-100 的整数综合评分\n"
                        + "report: 完整 Markdown 报告正文（含「## 各维度解读」「## 改进建议」标题）",
                task.getDays(), overviewJson, achievementJson);
        try {
            String raw = reportChatClient.prompt()
                    .system("你是严谨的数据分析师，只依据给定数据输出 JSON，不要编造数据。必须返回纯 JSON，"
                            + "字段为 dailyEvaluation/trendSummary/riskWarning/suggestion/score(整数0-100)/report。")
                    .user(prompt)
                    .call()
                    .content();
            AnalysisResult r = parseAnalysisResult(raw);
            if (r != null) {
                task.setDailyEvaluation(r.dailyEvaluation());
                task.setTrendSummary(r.trendSummary());
                task.setRiskWarning(r.riskWarning());
                task.setSuggestion(r.suggestion());
                task.setScore(r.score());
                String md = (r.report() != null && !r.report().isBlank()) ? r.report() : buildFallbackReport(r);
                task.setReport(md);
                task.setTags(extractTags(md));
                return;
            }
        } catch (Exception e) {
            log.warn("[AiAnalysis] 结构化解析失败，降级为 Markdown", e);
        }
        String md = generateMarkdownReport(task, charts);
        task.setReport(md);
        task.setTags(extractTags(md));
    }

    private String buildFallbackReport(AnalysisResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 总体评价\n\n").append(orEmpty(r.dailyEvaluation())).append("\n\n");
        sb.append("## 周期趋势总结\n\n").append(orEmpty(r.trendSummary())).append("\n\n");
        sb.append("## 生活习惯风险提示\n\n").append(orEmpty(r.riskWarning())).append("\n\n");
        sb.append("## 改进建议\n\n").append(orEmpty(r.suggestion())).append("\n");
        return sb.toString();
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }

    private String generateMarkdownReport(AiAnalysisTask task, Map<String, Object> charts) {
        String overviewJson = String.valueOf(charts.get("overview"));
        String achievementJson = String.valueOf(charts.get("achievement"));
        String prompt = String.format(
                "你是生活习惯助手的数据分析师。基于以下近 %d 天的真实分析数据，生成一份中文分析报告（Markdown 格式）：\n"
                        + "概览：%s\n达成率：%s\n"
                        + "要求：\n1. 开头用一句话总体评价；\n2. 用「## 各维度解读」分段说明睡眠/运动/饮水/饮食/心情；\n"
                        + "3. 用「## 改进建议」给出 3 条具体可执行的建议；\n4. 语气鼓励、客观，不编造数据。",
                task.getDays(), overviewJson, achievementJson);
        try {
            String content = reportChatClient.prompt()
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

    /**
     * 从 LLM 返回的纯文本（可能夹带 ```json 围栏或多余文字）中解析结构化分析结果。
     *
     * <p>复用 IntentRouter 已验证稳定的范式：{@code .content()} 取纯文本 JSON + 本地 Jackson 解析，
     * 彻底绕开 Spring AI 2.0.0 + DashScope 下 {@code .entity()} 触发的 ChunkMerger NoSuchElementException。
     *
     * @return 解析成功的 AnalysisResult；文本为空或解析失败返回 {@code null}（由调用方降级）
     */
    private AnalysisResult parseAnalysisResult(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String json = extractJson(raw);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, AnalysisResult.class);
        } catch (Exception e) {
            log.warn("[AiAnalysis] AnalysisResult 解析失败：{} | 原文={}", e.getMessage(), raw);
            return null;
        }
    }

    /**
     * 从可能包含 Markdown 围栏或前后缀文字的文本中提取首个 JSON 对象（与 IntentRouter.extractJson 同逻辑）。
     */
    private String extractJson(String raw) {
        String trimmed = raw.trim();
        int fence = trimmed.indexOf("```");
        if (fence >= 0) {
            int start = trimmed.indexOf('{', fence);
            int end = trimmed.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return trimmed.substring(start, end + 1);
            }
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return null;
    }

    /**
     * LLM 结构化输出映射（仅用于本地 Jackson 反序列化）。
     */
    private record AnalysisResult(
            String dailyEvaluation,
            String trendSummary,
            String riskWarning,
            String suggestion,
            Integer score,
            String report) {
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
