package com.habit.agent.tools;

import java.util.ArrayList;
import java.util.List;

import com.habit.agent.tools.constant.ToolConstants;
import com.habit.agent.tools.result.KnowledgeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 健康知识检索工具（对应 PRD 第 10 节 T10 search_knowledge）。
 *
 * <p>当前为本地健康常识库检索（RAG 接入前的过渡实现）：按关键词/类型从内置科普片段中
 * 匹配并返回带相关度的建议。后续接入 MongoDB Atlas Vector Search 后，将替换为
 * {@code RagService.search} 的真实语义检索（见 PRD 4.3）。</p>
 */
@Slf4j
@Component
public class KnowledgeTools {

    /** 本地健康常识片段（docType + 内容 + 关键词）。RAG 接入后移除。 */
    private record Knowledge(String docType, String content, String[] keywords) {}

    private final List<Knowledge> KNOWLEDGE_BASE = List.of(
            new Knowledge("SLEEP", "成年人建议每日睡眠 7-9 小时，固定作息比补觉更重要；睡前 1 小时避免蓝光与咖啡因。", new String[]{"睡眠", "失眠", "作息", "sleep"}),
            new Knowledge("SLEEP", "午睡建议控制在 20-30 分钟，过长会影响夜间入睡。", new String[]{"午睡", "午休"}),
            new Knowledge("EXERCISE", "每周建议中等强度运动 150 分钟以上，如快走、骑行；运动前后做好热身与拉伸。", new String[]{"运动", "锻炼", "跑步", "健身", "exercise"}),
            new Knowledge("EXERCISE", "久坐人群建议每 45 分钟起身活动 3-5 分钟，缓解颈椎与腰椎压力。", new String[]{"久坐", "活动"}),
            new Knowledge("WATER", "成人每日饮水建议 1500-2000 毫升，少量多次；运动或高温环境需适量增加。", new String[]{"水", "饮水", "喝水", "water"}),
            new Knowledge("DIET", "饮食建议少油少盐、粗细搭配，每日摄入足量蔬菜与优质蛋白；晚餐宜七分饱。", new String[]{"饮食", "吃", "营养", "diet"}),
            new Knowledge("DIET", "早餐应含碳水+蛋白+蔬果，避免空腹或过油；规律三餐有助于稳定血糖。", new String[]{"早餐", "三餐"})
    );

    @Tool(description = ToolConstants.Tools.SEARCH_KNOWLEDGE)
    public List<KnowledgeResult> searchKnowledge(
            @ToolParam(description = ToolConstants.ToolParams.KNOWLEDGE_QUERY) String query,
            @ToolParam(description = ToolConstants.ToolParams.KNOWLEDGE_TYPE, required = false) String docType,
            @ToolParam(description = ToolConstants.ToolParams.TOP_K, required = false) Integer topK) {

        int k = (topK != null && topK > 0) ? topK : 3;
        String q = (query == null ? "" : query).toLowerCase();
        String type = (docType == null ? null : docType.trim().toUpperCase());

        List<KnowledgeResult> matched = new ArrayList<>();
        for (Knowledge kb : KNOWLEDGE_BASE) {
            if (type != null && !kb.docType.equals(type)) {
                continue;
            }
            double score = 0.0;
            if (!q.isBlank()) {
                for (String kw : kb.keywords()) {
                    if (q.contains(kw.toLowerCase())) {
                        score += 0.5;
                    }
                }
                if (!kb.content.toLowerCase().contains(q) && score == 0.0) {
                    continue;
                }
                if (kb.content.toLowerCase().contains(q)) {
                    score += 0.3;
                }
            } else {
                score = 0.6; // 无关键词时给基础分
            }
            if (score > 0) {
                matched.add(KnowledgeResult.builder()
                        .docType(kb.docType).content(kb.content).score(Math.min(score, 1.0)).build());
            }
        }
        matched.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        log.info("Agent 知识检索: query={}, type={}, hit={}", query, type, matched.size());
        return matched.stream().limit(k).toList();
    }
}
