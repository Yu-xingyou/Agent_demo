package com.habit.agent.agent.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 阶段 6-2：对话日志 Advisor。
 *
 * <p>职责：记录每轮对话的会话 ID、用户输入摘要、响应耗时与 Token 用量，
 * 便于排障与成本观测。
 *
 * <p>设计约束：
 * <ul>
 *   <li>不打印完整 prompt / response，统一截断至 {@link #PREVIEW_LIMIT} 字符，避免日志膨胀与隐私泄漏。</li>
 *   <li>自身逻辑异常一律吞掉（仅 debug 记录），绝不影响对话主链路。</li>
 *   <li>耗时通过 {@code context} 在 before/after 之间传递，天然支持并发（context 随请求走）。</li>
 * </ul>
 *
 * <p>执行顺序 {@code HIGHEST_PRECEDENCE + 20}：位于安全过滤之后、业务 Advisor 之前，
 * 从而包裹住上下文注入 / 记忆 / RAG 检索的完整耗时。
 */
@Slf4j
@Component
public class LoggingAdvisor implements BaseAdvisor {

    /** 日志中输入 / 输出文本的最大预览长度。 */
    private static final int PREVIEW_LIMIT = 200;

    /** 在 advisor context 中暂存起始时间戳的 key。 */
    private static final String CTX_START_TIME = "habit.logging.startTime";

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        try {
            log.info("[Advisor:Logging] 对话开始 | conversationId={} | 输入={}",
                    resolveConversationId(request), preview(resolveUserInput(request)));
            return request.mutate()
                    .context(CTX_START_TIME, System.currentTimeMillis())
                    .build();
        } catch (Exception e) {
            log.debug("[Advisor:Logging] 请求日志记录失败（已忽略）：{}", e.getMessage());
            return request;
        }
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        try {
            log.info("[Advisor:Logging] 对话完成 | 耗时={}ms | {} | 输出={}",
                    resolveCostMs(response), formatUsage(response), preview(resolveOutput(response)));
        } catch (Exception e) {
            log.debug("[Advisor:Logging] 响应日志记录失败（已忽略）：{}", e.getMessage());
        }
        return response;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }

    // ===== 内部辅助 =====

    /** 从 advisor context 取会话 ID（由 ChatService 通过 advisorParams 传入）。 */
    private String resolveConversationId(ChatClientRequest request) {
        Object id = request.context().get(ChatMemory.CONVERSATION_ID);
        return id == null ? "-" : String.valueOf(id);
    }

    /**
     * 提取本轮用户输入。
     *
     * <p>使用 {@code getUserMessage()} 而非 {@code prompt().getContents()}：
     * 后者会把系统提示词与历史消息一并拼进来（实测约 2500 字），日志噪音大且无助于排障。
     */
    private String resolveUserInput(ChatClientRequest request) {
        return request.prompt().getUserMessage() == null
                ? ""
                : request.prompt().getUserMessage().getText();
    }

    /** 计算耗时；取不到起始时间戳时返回 -1（表示未知）。 */
    private long resolveCostMs(ChatClientResponse response) {
        Object start = response.context().get(CTX_START_TIME);
        return (start instanceof Long startMs) ? System.currentTimeMillis() - startMs : -1L;
    }

    /** 提取模型输出文本。 */
    private String resolveOutput(ChatClientResponse response) {
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null || chatResponse.getResult() == null
                || chatResponse.getResult().getOutput() == null) {
            return "";
        }
        return chatResponse.getResult().getOutput().getText();
    }

    /** 格式化 Token 用量，缺失时返回占位说明。 */
    private String formatUsage(ChatClientResponse response) {
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null || chatResponse.getMetadata() == null) {
            return "token=未知";
        }
        Usage usage = chatResponse.getMetadata().getUsage();
        if (usage == null) {
            return "token=未知";
        }
        return "token[prompt=%s, completion=%s, total=%s]".formatted(
                usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
    }

    /** 文本截断预览，避免日志过长。 */
    private String preview(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String oneLine = text.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= PREVIEW_LIMIT
                ? oneLine
                : oneLine.substring(0, PREVIEW_LIMIT) + "...(共" + oneLine.length() + "字)";
    }
}
