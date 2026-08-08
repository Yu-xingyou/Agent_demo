package com.habit.agent.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.habit.agent.aigc.entity.mongo.AiAnalysisDoc;
import com.habit.agent.aigc.repository.AiAnalysisRepository;
import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.common.vo.AchievementRateVO;
import com.habit.agent.common.vo.RadarDataVO;
import com.habit.agent.common.vo.TrendDataVO;
import com.habit.agent.service.AiAnalysisService;
import com.habit.agent.service.AnalysisService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 智能分析服务实现
 * <p>
 * 异步生成：创建 PROCESSING 任务 → 后台线程聚合业务统计（overview/trend/achievement/radar）+ 调用 LLM 生成文本结论 →
 * 解析为结构化 JSON（score/dailyEvaluation/trendSummary/riskWarning/suggestion/report/charts）存 MongoDB aiAnalysis。
 */
@Slf4j
@Service
public class AiAnalysisServiceImpl implements AiAnalysisService {

    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AiAnalysisRepository aiAnalysisRepository;
    private final AnalysisService analysisService;
    private final ChatClient analysisChatClient;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    public AiAnalysisServiceImpl(AiAnalysisRepository aiAnalysisRepository,
                                 AnalysisService analysisService,
                                 ChatClient.Builder chatClientBuilder) {
        this.aiAnalysisRepository = aiAnalysisRepository;
        this.analysisService = analysisService;
        // 独立 ChatClient：不挂记忆/工具，避免污染对话上下文
        this.analysisChatClient = chatClientBuilder.build();
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    /**
     * 生成分析报告（创建 PROCESSING 任务并异步执行）
     *
     * @param days 分析天数窗口
     * @return 新建的分析任务信息（状态 PROCESSING）
     */
    @Override
    public Map<String, Object> generate(int days) {
        return createTask(days);
    }

    /**
     * 重新生成分析报告（重建任务并异步执行）
     *
     * @param days 分析天数窗口
     * @return 重建的分析任务信息（状态 PROCESSING）
     */
    @Override
    public Map<String, Object> regenerate(int days) {
        return createTask(days);
    }

    /**
     * 按任务 id 查询分析任务
     *
     * @param id 分析任务 id
     * @return 分析任务详情；不存在时返回 null
     */
    @Override
    public Map<String, Object> getById(String id) {
        return aiAnalysisRepository.findById(id)
                .map(this::toTaskMap)
                .orElse(null);
    }

    /**
     * 历史分析列表（按创建时间倒序）
     *
     * @param limit 返回条数上限（<=0 时默认 10）
     * @return 历史分析报告摘要列表
     */
    @Override
    public List<Map<String, Object>> history(int limit) {
        int n = (limit <= 0) ? 10 : limit;
        return aiAnalysisRepository.findByUserIdOrderByCreateTimeDesc(AgentConstants.DEFAULT_USER_ID).stream()
                .limit(n)
                .map(this::toTaskMap)
                .toList();
    }

    /**
     * 最新已完成分析（DAILY 类型且状态 COMPLETED）
     *
     * @return 最新一条已完成的分析报告；无则 null
     */
    @Override
    public Map<String, Object> getLatest() {
        return aiAnalysisRepository.findTopByUserIdAndTypeOrderByCreateTimeDesc(
                        AgentConstants.DEFAULT_USER_ID, "DAILY")
                .filter(doc -> STATUS_COMPLETED.equals(doc.getStatus()))
                .map(this::toTaskMap)
                .orElse(null);
    }

    /**
     * 创建分析任务并异步执行
     *
     * @param days 分析天数窗口（<=0 时默认 7）
     * @return 新建任务的 Task 结构（状态 PROCESSING）
     */
    private Map<String, Object> createTask(int days) {
        int d = (days <= 0) ? 7 : days;
        LocalDateTime now = LocalDateTime.now();
        AiAnalysisDoc doc = AiAnalysisDoc.builder()
                .userId(AgentConstants.DEFAULT_USER_ID)
                .type("DAILY")
                .recordDate(now.toLocalDate().toString())
                .days(d)
                .status(STATUS_PROCESSING)
                .createTime(now)
                .build();
        aiAnalysisRepository.save(doc);
        String id = doc.getId();

        executor.submit(() -> doGenerate(id, d));
        return toTaskMap(doc);
    }

    /**
     * 后台生成报告主体
     */
    private void doGenerate(String id, int days) {
        AiAnalysisDoc doc = aiAnalysisRepository.findById(id).orElse(null);
        if (doc == null) {
            return;
        }
        try {
            // 1. 聚合业务统计
            Map<String, Object> overview = analysisService.getOverview(AgentConstants.DEFAULT_USER_ID, days);
            TrendDataVO trend = analysisService.getTrend(AgentConstants.DEFAULT_USER_ID, days);
            AchievementRateVO achievement = analysisService.getAchievementRate(AgentConstants.DEFAULT_USER_ID, days);
            RadarDataVO radar = analysisService.getRadar(AgentConstants.DEFAULT_USER_ID, days);

            // 2. 组装 charts（图表数据不依赖 LLM，由业务统计直接生成）
            Map<String, Object> charts = buildCharts(overview, trend, achievement, radar);

            // 3. 调用 LLM 生成文本结论
            String llmJson = callModel(overview, trend, achievement, days);

            // 4. 解析 LLM 输出并合并 charts
            Map<String, Object> content = new LinkedHashMap<>();
            JsonNode node = parseJson(llmJson);
            content.put("score", node.path("score").asInt(70));
            content.put("dailyEvaluation", node.path("dailyEvaluation").asText("暂无点评"));
            content.put("trendSummary", node.path("trendSummary").asText("暂无趋势总结"));
            content.put("riskWarning", node.path("riskWarning").asText("暂无风险"));
            content.put("suggestion", node.path("suggestion").asText(""));
            content.put("report", node.path("report").asText(llmJson));
            content.put("charts", charts);

            // 5. 落库
            doc.setStatus(STATUS_COMPLETED);
            doc.setTitle("生活习惯分析报告 · " + LocalDate.now());
            doc.setContent(MAPPER.writeValueAsString(content));
            aiAnalysisRepository.save(doc);
            log.info("AI 分析完成：id={}", id);
        } catch (Exception e) {
            log.error("AI 分析失败：id={}", id, e);
            doc.setStatus(STATUS_FAILED);
            doc.setError(e.getMessage());
            aiAnalysisRepository.save(doc);
        }
    }

    /**
     * 调用 LLM 生成结构化文本结论（JSON）
     *
     * @param overview     综合概览数据
     * @param trend        趋势数据
     * @param achievement  达成率数据
     * @param days         分析天数窗口
     * @return LLM 返回的 JSON 文本（含 score/dailyEvaluation 等字段）
     */
    private String callModel(Map<String, Object> overview, TrendDataVO trend, AchievementRateVO achievement, int days) {
        String dataJson;
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("overview", overview);
            data.put("trend", trend);
            data.put("achievement", achievement);
            dataJson = MAPPER.writeValueAsString(data);
        } catch (Exception e) {
            dataJson = "数据序列化失败";
        }

        String prompt = """
                你是生活习惯数据分析师。请根据用户最近 %d 天的打卡数据，生成一份结构化健康分析报告。

                分析要点：
                1. score：综合健康评分（0-100 整数）
                2. dailyEvaluation：今日点评（一句话）
                3. trendSummary：近 %d 天趋势总结（睡眠/运动/饮水/心情的变化）
                4. riskWarning：风险提示（如睡眠不足、运动缺失、饮水不足等，没有则写"暂无重大风险"）
                5. suggestion：改进建议（3-5 条，每行一条，以换行分隔）
                6. report：完整报告（300 字以内，自然语言段落）

                要求：
                - 严格输出 JSON，不要包含 markdown 代码块标记，不要输出 JSON 以外的内容
                - 字段名必须为：score, dailyEvaluation, trendSummary, riskWarning, suggestion, report
                - suggestion 用换行分隔多条

                用户数据：
                %s
                """.formatted(days, days, dataJson);

        return analysisChatClient.prompt()
                .system("你只输出合法 JSON，不输出任何其他内容。")
                .user(prompt)
                .call()
                .content();
    }

    /**
     * 组装前端 ECharts 所需 charts 结构（overview/trend/achievement/radar）
     *
     * @param overview     综合概览数据
     * @param trend        趋势数据
     * @param achievement  达成率数据
     * @param radar        雷达数据
     * @return 前端图表所需的 charts 结构
     */
    private Map<String, Object> buildCharts(Map<String, Object> overview, TrendDataVO trend,
                                            AchievementRateVO achievement, RadarDataVO radar) {
        Map<String, Object> charts = new LinkedHashMap<>();

        // 综合概览统计卡
        Map<String, Object> overviewMap = new LinkedHashMap<>();
        if (overview != null) {
            overviewMap.put("recordDays", overview.get("recordDays"));
            overviewMap.put("avgSleep", overview.get("avgSleep"));
            overviewMap.put("avgExercise", overview.get("avgExercise"));
            overviewMap.put("avgWater", overview.get("avgWater"));
            overviewMap.put("avgMood", overview.get("avgMood"));
        }
        charts.put("overview", overviewMap);

        // 趋势图
        if (trend != null) {
            Map<String, Object> trendMap = new LinkedHashMap<>();
            trendMap.put("dates", trend.getDates());
            trendMap.put("sleep", trend.getSleep());
            trendMap.put("exercise", trend.getExercise());
            trendMap.put("water", trend.getWater());
            trendMap.put("mood", trend.getMood());
            trendMap.put("customSeries", trend.getCustomSeries());
            charts.put("trend", trendMap);
        } else {
            charts.put("trend", Map.of());
        }

        // 维度达标率
        if (achievement != null) {
            Map<String, Object> achMap = new LinkedHashMap<>();
            List<Map<String, Object>> dimensions = new ArrayList<>();
            if (achievement.getDimensions() != null) {
                for (AchievementRateVO.DimensionRate d : achievement.getDimensions()) {
                    Map<String, Object> dim = new LinkedHashMap<>();
                    dim.put("label", d.getLabel());
                    dim.put("actual", d.getActual());
                    dim.put("target", d.getTarget());
                    dim.put("rate", d.getRate());
                    dimensions.add(dim);
                }
            }
            achMap.put("dimensions", dimensions);
            charts.put("achievement", achMap);
        } else {
            charts.put("achievement", Map.of());
        }

        // 雷达图
        if (radar != null) {
            Map<String, Object> radarMap = new LinkedHashMap<>();
            radarMap.put("indicators", radar.getIndicators());
            radarMap.put("values", radar.getValues());
            radarMap.put("targets", radar.getTargets());
            charts.put("radar", radarMap);
        } else {
            charts.put("radar", Map.of());
        }

        return charts;
    }

    /**
     * 容错解析 LLM 输出的 JSON（剥离可能的 markdown 代码块）
     *
     * @param text LLM 原始输出文本
     * @return 解析得到的 JSON 节点；解析失败时返回空对象节点
     */
    private JsonNode parseJson(String text) {
        String cleaned = text == null ? "" : text.trim();
        // 剥离 ```json ... ``` 包裹
        if (cleaned.startsWith("```")) {
            int firstNl = cleaned.indexOf('\n');
            int lastFence = cleaned.lastIndexOf("```");
            if (firstNl >= 0 && lastFence > firstNl) {
                cleaned = cleaned.substring(firstNl + 1, lastFence).trim();
            }
        }
        try {
            return MAPPER.readTree(cleaned);
        } catch (Exception e) {
            log.warn("LLM 分析输出非合法 JSON，降级为空节点：{}", text);
            return MAPPER.createObjectNode();
        }
    }

    /**
     * 任务文档 → 前端 Task 结构（解析 content 中的结构化字段）
     *
     * @param doc 分析任务文档
     * @return 前端展示用的 Task 键值对（含 status/score/report/charts 等）
     */
    private Map<String, Object> toTaskMap(AiAnalysisDoc doc) {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("id", doc.getId());
        task.put("status", doc.getStatus());
        task.put("type", doc.getType());
        task.put("days", doc.getDays());
        task.put("createTime", doc.getCreateTime());
        if (doc.getError() != null) {
            task.put("error", doc.getError());
        }
        if (doc.getContent() != null) {
            try {
                JsonNode node = MAPPER.readTree(doc.getContent());
                task.put("score", node.path("score").asInt(0));
                task.put("dailyEvaluation", node.path("dailyEvaluation").asText(""));
                task.put("trendSummary", node.path("trendSummary").asText(""));
                task.put("riskWarning", node.path("riskWarning").asText(""));
                task.put("suggestion", node.path("suggestion").asText(""));
                task.put("report", node.path("report").asText(""));
                task.put("charts", node.path("charts"));
            } catch (Exception e) {
                log.warn("解析分析内容失败：id={}", doc.getId(), e);
            }
        }
        return task;
    }
}
