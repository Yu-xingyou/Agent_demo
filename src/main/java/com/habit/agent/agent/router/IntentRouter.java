package com.habit.agent.agent.router;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.extern.slf4j.Slf4j;

/**
 * 阶段九（多智能体路由）意图路由器 —— 升级为 LLM 驱动的路由智能体（Director）。
 *
 * <p>承担 Director 协调者角色：将用户消息按意图分类到三个子 Agent：
 * <ul>
 *   <li>{@link Intent#DATA_ANALYSIS} —— 数据分析 / 趋势 / 报告 / 达标率类诉求，交由 {@code DataAnalysisAgent}；</li>
 *   <li>{@link Intent#SUGGESTION} —— 改善建议 / 怎么做 / 计划类诉求，交由 {@code SuggestionAgent}；</li>
 *   <li>{@link Intent#CHAT} —— 闲聊 / 通用问答，交由 {@code ChatAgent}。</li>
 * </ul>
 *
 * <p>路由策略（方案 A）：优先由 LLM Director（{@code directorChatClient}）做意图判定，输出结构化
 * {@link IntentDecision}；当 LLM 调用失败 / 超时 / 解析异常时，安全降级到原有的「关键词 + 正则」规则路由
 * （{@link #fallbackRoute}），保证可用性。LLM 判定仅做派单，不重新执行工具循环。
 */
@Slf4j
@Component
public class IntentRouter {

    private final ChatClient directorChatClient;

    public IntentRouter(ChatClient directorChatClient) {
        this.directorChatClient = directorChatClient;
    }

    /** 智能体意图类型。 */
    public enum Intent {
        /** 数据分析（趋势/报告/达标率/雷达） */
        DATA_ANALYSIS,
        /** 改善建议（怎么做/计划/方案） */
        SUGGESTION,
        /** 通用闲聊 */
        CHAT
    }

    /**
     * LLM 路由智能体的结构化输出契约。
     *
     * <p>用 Jackson 注解约束 {@code intent} 取值，保证 {@code .entity(IntentDecision.class)}
     * 能稳定反序列化；{@code reason} 为可选路由依据。
     */
    @JsonClassDescription("用户意图分类结果")
    public static class IntentDecision {
        @JsonProperty(required = true)
        public Intent intent;
        @JsonProperty(required = false)
        public String reason;
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
     * <p>内部先尝试 LLM Director 决策，失败（异常 / 解析为空）则降级到关键词规则路由。
     *
     * @param message 用户原始输入（已去除首尾空白）
     * @return 命中的意图，默认 {@link Intent#CHAT}
     */
    public Intent route(String message) {
        if (message == null || message.isBlank()) {
            return Intent.CHAT;
        }
        Intent llmIntent = routeByLlm(message);
        if (llmIntent != null) {
            return llmIntent;
        }
        log.debug("[路由] LLM 决策不可用，降级到关键词规则路由");
        return fallbackRoute(message);
    }

    /**
     * 由 LLM 路由智能体（Director）判定意图。
     *
     * @return 解析成功的意图；任何异常或解析为空返回 {@code null}（交由调用方降级）
     */
    private Intent routeByLlm(String message) {
        try {
            IntentDecision decision = directorChatClient.prompt()
                    .user("判断以下用户消息的意图：\n" + message)
                    .call()
                    .entity(IntentDecision.class);
            if (decision != null && decision.intent != null) {
                log.debug("[路由] LLM Director 判定意图={} 依据={}", decision.intent, decision.reason);
                return decision.intent;
            }
            return null;
        } catch (Exception e) {
            log.warn("[路由] LLM Director 调用失败，将降级：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 关键词 + 正则 规则路由（LLM 不可用时的安全降级）。
     *
     * <p>命中多意图时按优先级 DATA_ANALYSIS > SUGGESTION > CHAT 取最高优先级意图。
     */
    private Intent fallbackRoute(String message) {
        String text = message.trim();
        boolean dataHit = DATA_ANALYSIS_PATTERNS.stream().anyMatch(p -> p.matcher(text).find());
        boolean sugHit = SUGGESTION_PATTERNS.stream().anyMatch(p -> p.matcher(text).find());
        if (dataHit) {
            return Intent.DATA_ANALYSIS;
        }
        if (sugHit) {
            return Intent.SUGGESTION;
        }
        return Intent.CHAT;
    }
}
