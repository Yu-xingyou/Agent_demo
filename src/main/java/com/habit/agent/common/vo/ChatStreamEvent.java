package com.habit.agent.common.vo;

/**
 * 真流式对话 SSE 事件契约（阶段五 5-2 真流式改造）。
 *
 * <p>取代原先在 {@code ChatController} 中手工拼接 JSON 字符串的做法，由 Jackson 序列化。
 * 事件名（{@link #eventName()}）与前端解析逻辑保持一致，向后兼容。
 *
 * <p>字段设计与 PRD 协议对齐：
 * <ul>
 *   <li>{@code meta}：会话元信息（含路由意图 intent）。</li>
 *   <li>{@code chunk}：文本增量分片（真流式逐字输出）。</li>
 *   <li>{@code tool_call}：工具调用状态（工具轮次恢复发出）。</li>
 *   <li>{@code done}：结束汇总（含真实 Token 用量与耗时）。</li>
 *   <li>{@code error}：统一错误事件。</li>
 * </ul>
 *
 * <p>Token 字段使用包装类型 {@code Integer}：当 DashScope 未回传 usage 时为 {@code null}，
 * 前端据此隐藏该行，避免展示误导性的 0 值。
 */
public sealed interface ChatStreamEvent {

    /** SSE 事件名（对应前端监听的事件类型）。 */
    String eventName();

    /** 会话元信息：首个事件，告知前端 conversationId / 模型 / 路由意图。 */
    record Meta(String conversationId, String timestamp, String model, String intent)
            implements ChatStreamEvent {
        @Override
        public String eventName() {
            return "meta";
        }
    }

    /** 文本增量分片：真流式逐字下发，content 为本次新增文本、index 为分片序号。 */
    record Chunk(String content, int index) implements ChatStreamEvent {
        @Override
        public String eventName() {
            return "chunk";
        }
    }

    /** 工具调用状态：工具轮次期间下发，status 取 start/end，message 为可展示文案。 */
    record ToolCall(String status, String message, String toolName) implements ChatStreamEvent {
        @Override
        public String eventName() {
            return "tool_call";
        }
    }

    /** 结束汇总：携带真实 Token 用量、耗时与首字延迟。 */
    record Done(String conversationId, Integer promptTokens, Integer completionTokens,
                Integer totalTokens, long duration, Long firstTokenLatency,
                String streamingMode) implements ChatStreamEvent {
        @Override
        public String eventName() {
            return "done";
        }
    }

    /** 统一错误事件：retryable 标记前端是否可提示「重试」。 */
    record Error(String errorCode, String message, String conversationId, boolean retryable)
            implements ChatStreamEvent {
        @Override
        public String eventName() {
            return "error";
        }
    }
}
