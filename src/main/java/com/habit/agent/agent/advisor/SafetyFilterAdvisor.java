package com.habit.agent.agent.advisor;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * 阶段 6-2：安全过滤 Advisor。
 *
 * <p>职责：在请求进入模型之前做前置校验，命中规则时<b>直接短路返回</b>合规话术，
 * 不调用下游 Advisor 链与大模型，从而节省 Token 并规避风险内容。
 *
 * <p>校验规则：
 * <ol>
 *   <li><b>超长输入</b>：用户文本超过 {@link #MAX_INPUT_LENGTH} 字符时拒绝。</li>
 *   <li><b>敏感词</b>：命中内置词表（越界医疗诊断 / 违法危险行为 / 自伤）时拒绝，
 *       其中自伤类返回带干预引导的专门话术。</li>
 * </ol>
 *
 * <p>本 Advisor 是链上唯一会主动中断的组件，因此排在最前
 * （{@code HIGHEST_PRECEDENCE + 10}），确保拦截发生在任何昂贵操作之前。
 * 校验逻辑自身若抛异常则放行，遵循"安全组件不可反噬主流程"原则。
 */
@Slf4j
@Component
public class SafetyFilterAdvisor implements CallAdvisor, StreamAdvisor {

    /** 用户单次输入最大字符数。 */
    private static final int MAX_INPUT_LENGTH = 4000;

    /** 超长输入拒答话术。 */
    private static final String REPLY_TOO_LONG =
            "你发送的内容有点太长啦，我一次读不完 😅 能否精简一下，只保留最想问的部分？";

    /** 越界话题统一拒答话术。 */
    private static final String REPLY_OUT_OF_SCOPE =
            "这个话题超出了我的能力范围，我更擅长陪你聊睡眠、运动、饮食这些生活习惯。要不我们聊聊你今天的作息？";

    /** 自伤 / 危机干预话术。 */
    private static final String REPLY_CRISIS =
            "听起来你现在可能很不好受，我很在意你的状态。我只是一个习惯助手，没办法提供专业帮助，"
            + "但请一定联系身边信任的人，或拨打心理援助热线 400-161-9995。你并不孤单。";

    /** 危机干预类关键词（优先级最高，返回专门话术）。 */
    private static final List<String> CRISIS_WORDS = List.of(
            "自杀", "自残", "自伤", "轻生", "不想活");

    /** 越界医疗诊断类关键词（助手非医疗设备，不得给出诊断/处方）。 */
    private static final List<String> MEDICAL_WORDS = List.of(
            "处方", "开药", "剂量", "确诊", "癌症", "抗抑郁药", "安眠药买");

    /** 违法 / 危险行为类关键词。 */
    private static final List<String> ILLEGAL_WORDS = List.of(
            "毒品", "冰毒", "制毒", "枪支", "炸药", "赌博网站", "洗钱");

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String rejection = resolveRejection(request);
        if (rejection == null) {
            return chain.nextCall(request);
        }
        return shortCircuit(request, rejection);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        String rejection = resolveRejection(request);
        if (rejection == null) {
            return chain.nextStream(request);
        }
        return Flux.just(shortCircuit(request, rejection));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    // ===== 内部辅助 =====

    /**
     * 判定是否需要拒答。
     *
     * @return 需拒答时返回话术文本；放行时返回 {@code null}
     */
    private String resolveRejection(ChatClientRequest request) {
        try {
            String text = request.prompt().getUserMessage() == null
                    ? null
                    : request.prompt().getUserMessage().getText();
            if (text == null || text.isBlank()) {
                return null;
            }
            if (text.length() > MAX_INPUT_LENGTH) {
                log.warn("[Advisor:Safety] 拦截超长输入，长度={}", text.length());
                return REPLY_TOO_LONG;
            }
            String lower = text.toLowerCase();
            if (containsAny(lower, CRISIS_WORDS)) {
                log.warn("[Advisor:Safety] 命中危机干预关键词，返回引导话术");
                return REPLY_CRISIS;
            }
            if (containsAny(lower, MEDICAL_WORDS) || containsAny(lower, ILLEGAL_WORDS)) {
                log.warn("[Advisor:Safety] 命中越界话题关键词，已拒答");
                return REPLY_OUT_OF_SCOPE;
            }
            return null;
        } catch (Exception e) {
            // 安全组件自身异常不得反噬主流程，放行交由下游处理
            log.debug("[Advisor:Safety] 安全校验异常（已放行）：{}", e.getMessage());
            return null;
        }
    }

    private boolean containsAny(String text, List<String> words) {
        return words.stream().anyMatch(text::contains);
    }

    /** 构造一个不经过模型的本地响应，直接返回给调用方。 */
    private ChatClientResponse shortCircuit(ChatClientRequest request, String reply) {
        ChatResponse chatResponse = new ChatResponse(
                List.of(new Generation(new AssistantMessage(reply))));
        return ChatClientResponse.builder()
                .chatResponse(chatResponse)
                .context(Map.copyOf(request.context()))
                .build();
    }
}
