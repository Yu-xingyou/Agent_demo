package com.habit.agent.agent.router;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 阶段九（多智能体路由）意图路由器。
 *
 * <p>承担 Director 协调者角色：将用户消息按意图分类到三个子 Agent：
 * <ul>
 *   <li>{@link Intent#DATA_ANALYSIS} —— 数据分析 / 趋势 / 报告 / 达标率类诉求，交由 {@code DataAnalysisAgent}；</li>
 *   <li>{@link Intent#SUGGESTION} —— 改善建议 / 怎么做 / 计划类诉求，交由 {@code SuggestionAgent}；</li>
 *   <li>{@link Intent#CHAT} —— 闲聊 / 通用问答，交由 {@code ChatAgent}。</li>
 * </ul>
 *
 * <p>采用「关键词 + 正则」轻量分类，避免额外 LLM 调用带来的延迟与成本；
 * 命中多意图时按优先级 DATA_ANALYSIS > SUGGESTION > CHAT 取最高优先级意图。
 */
public final class IntentRouter {

    private IntentRouter() {
    }

    /** 智能体意图类型（技术点 13）。 */
    public enum Intent {
        /** 数据分析（趋势/报告/达标率/雷达） */
        DATA_ANALYSIS,
        /** 改善建议（怎么做/计划/方案） */
        SUGGESTION,
        /** 通用闲聊 */
        CHAT
    }

    private static final List<Pattern> DATA_ANALYSIS_PATTERNS = Arrays.asList(
            Pattern.compile("分析|报告|趋势|统计|达标率|达成率|雷达|周报|月报|数据|图表|对比|总结|评价|评分|得分"),
            Pattern.compile("最近.{0,4}(睡|运动|饮水|饮食|心情)|(睡|运动|饮水|饮食|心情).{0,4}(怎么样|如何|好不好|达标)")
    );

    private static final List<Pattern> SUGGESTION_PATTERNS = Arrays.asList(
            Pattern.compile("建议|怎么|如何|怎样|计划|方案|改善|调整|提升|提高|帮助|该|应该|推荐|攻略|技巧"),
            Pattern.compile("(睡|运动|饮水|饮食|心情).{0,4}(不够|差|不好|不足|想改善|想调整)")
    );

    /**
     * 路由用户消息到具体意图。
     *
     * @param message 用户原始输入（已去除首尾空白）
     * @return 命中的意图，默认 {@link Intent#CHAT}
     */
    public static Intent route(String message) {
        if (message == null || message.isBlank()) {
            return Intent.CHAT;
        }
        String text = message.trim();

        boolean dataHit = DATA_ANALYSIS_PATTERNS.stream().anyMatch(p -> p.matcher(text).find());
        boolean sugHit = SUGGESTION_PATTERNS.stream().anyMatch(p -> p.matcher(text).find());

        // 优先级：数据分析 > 建议 > 闲聊
        if (dataHit) {
            return Intent.DATA_ANALYSIS;
        }
        if (sugHit) {
            return Intent.SUGGESTION;
        }
        return Intent.CHAT;
    }
}
