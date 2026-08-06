package com.habit.agent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import com.habit.agent.agent.advisor.LoggingAdvisor;
import com.habit.agent.agent.advisor.SafeRetrievalAdvisor;
import com.habit.agent.agent.advisor.SafetyFilterAdvisor;
import com.habit.agent.agent.tools.GoalTools;
import com.habit.agent.agent.tools.HabitActionTools;
import com.habit.agent.agent.tools.HabitQueryTools;
import com.habit.agent.agent.tools.HabitStatTools;

import lombok.extern.slf4j.Slf4j;

/**
 * 阶段四/六（Spring AI 基础 + Tool Calling）：ChatClient 配置与通义千问连通验证。
 *
 * <p>职责：
 * <ol>
 *   <li>构建全局 {@link ChatClient} Bean，注入 defaultSystem（生活习惯助手人格）、
 *       对话记忆 Advisor、业务工具（通过 MethodToolCallbackProvider 自动扫描 @Tool 注解）。</li>
 *   <li>系统提示词从 {@code classpath:prompts/system-prompt.st} 读取。</li>
 *   <li>启动连通性探针验证通义千问 DashScope API Key 与模型可用。</li>
 * </ol>
 *
 * <p><b>Spring AI 2.0.0 架构要点</b>：
 * <ul>
 *   <li>{@code ToolCallingAdvisor} 自动注册：ChatClient 在检测到工具时自动注入，
 *       工具执行循环由框架内部透明完成，对 {@code call()} 与 {@code stream()} 均生效。</li>
 *   <li>记忆 Advisor 优先级：官方 {@code MessageChatMemoryAdvisor} 默认 order 为
 *       {@code HIGHEST+200}，位于工具 Advisor（+300）之外，工具调用中间消息不再写入
 *       ChatMemory（符合官方默认推荐布局，刻意保持，勿随意改大）。</li>
 *   <li>{@code streamToolCallResponses} 已移除：中间工具调用请求不再流式发出，
 *       仅最终助手回复逐字流式输出。工具调用轮次「中间无文本分片」是 2.0.0 的<b>预期行为</b>，
 *       并非流式失败——此前误判此现象为故障并退回非流式（方案 B）属误判。</li>
 *   <li>真流式根因修复：{@link SafeRetrievalAdvisor} 原有 order 为 {@code HIGHEST+300}，
 *       与自动注册的 {@code ToolCallingAdvisor}（同样 +300）order 冲突、相对顺序未定义；
 *       一旦 RAG Advisor 排在工具 Advisor 之前，整个工具循环被包进其 {@code onErrorResume}，
 *       任意瞬时异常都会静默重入下游链 → 流式错乱/重复。已前移至 {@code HIGHEST+150} 修复。</li>
 *   <li>{@code parallelToolCalls} 现状为 {@code false}（保守默认）：order 冲突修复后，
 *       通过 {@link com.habit.agent.StreamingProbeTest} 实测 DashScope 在并行工具调用下是否
 *       仍有分片异常；若稳定则改回 {@code true} 恢复多工具并行能力。当前取值为<b>预防措施</b>，
 *       不再归因于已证伪的「ChunkMerger 崩溃」推测。</li>
 * </ul>
 *
 * <p><b>Advisor 链执行顺序</b>（order 越小越先执行，阶段六/八装配）：
 * <table border="1">
 *   <caption>Advisor 顺序表</caption>
 *   <tr><th>order</th><th>Advisor</th><th>职责</th></tr>
 *   <tr><td>HIGHEST+10</td><td>{@link SafetyFilterAdvisor}</td>
 *       <td>敏感词/超长输入前置拦截，命中即短路，不消耗 Token</td></tr>
 *   <tr><td>HIGHEST+20</td><td>{@link LoggingAdvisor}</td>
 *       <td>包裹下游全链路，记录耗时与 Token 用量</td></tr>
 *   <tr><td>HIGHEST+150</td><td>{@link SafeRetrievalAdvisor}</td>
 *       <td>RAG 知识检索增强，带失败降级；前置于记忆与工具之前，异常兜底仅覆盖检索阶段</td></tr>
 *   <tr><td>HIGHEST+200</td><td>{@code MessageChatMemoryAdvisor}</td>
 *       <td>多轮对话记忆（官方 {@code DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER}）</td></tr>
 *   <tr><td>HIGHEST+300</td><td>{@code ToolCallingAdvisor}（框架自动注册）</td>
 *       <td>工具执行循环，独占该 order，避免与自定义 Advisor 冲突</td></tr>
 * </table>
 */
@Slf4j
@Configuration
public class ChatClientConfig {

    /** 系统提示词模板（生活习惯助手人格）。 */
    @Value("classpath:prompts/system-prompt.st")
    private Resource systemPromptResource;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                 MessageChatMemoryAdvisor memoryAdvisor,
                                 SafetyFilterAdvisor safetyFilterAdvisor,
                                 LoggingAdvisor loggingAdvisor,
                                 SafeRetrievalAdvisor safeRetrievalAdvisor,
                                 HabitQueryTools habitQueryTools,
                                 HabitStatTools habitStatTools,
                                 GoalTools goalTools,
                                 HabitActionTools habitActionTools) {
        // model / temperature 在 application.yml 的 spring.ai.openai.chat.options 中配置，
        // ChatClient.Builder 自动读取；此处仅注入 system prompt、Advisor 链、业务工具。
        // 记忆按 conversationId 隔离：调用方通过 advisorParams(ChatMemory.CONVERSATION_ID, id) 传入。
        // 工具通过 MethodToolCallbackProvider 自动扫描各 bean 上所有 @Tool 注解方法注册。
        ToolCallbackProvider toolProvider = MethodToolCallbackProvider.builder()
                .toolObjects(habitQueryTools, habitStatTools, goalTools, habitActionTools)
                .build();
        // Advisor 传入顺序不影响实际执行顺序，最终由各自 getOrder() 排序（见类注释顺序表）
        return builder
                .defaultSystem(systemPromptResource)
                .defaultAdvisors(safetyFilterAdvisor, loggingAdvisor,
                        memoryAdvisor, safeRetrievalAdvisor)
                .defaultTools(toolProvider)
                // parallelToolCalls=false：order 冲突已修复，此为预防 DashScope 并行工具调用分片异常的保守默认，
                // 待 StreamingProbeTest 实测稳定后可改回 true 恢复多工具并行。
                .defaultOptions(OpenAiChatOptions.builder().parallelToolCalls(false))
                .build();
    }

    /**
     * 路由智能体（Director）专用 ChatClient：仅做意图分类派单，不参与工具循环、不挂对话记忆。
     *
     * <p>复用同一 {@link ChatClient.Builder}（自动读取 model/temperature/api-key 配置），但：
     * <ul>
     *   <li><b>不挂 {@code MessageChatMemoryAdvisor}</b>——Director 是无状态分类器，不写用户记忆，
     *       避免路由决策结果污染会话记忆窗口（与 reportChatClient 同一考量）；</li>
     *   <li><b>不挂业务工具</b>——分类无需调用打卡/统计/目标等工具；</li>
     *   <li><b>低 temperature（0.0）</b>——保证意图判定稳定、可复现；</li>
     *   <li><b>结构化输出</b>——由 {@link com.habit.agent.agent.router.IntentRouter} 通过
     *       {@code .entity(IntentDecision.class)} 解析为枚举，避免自由文本解析脆弱。</li>
     * </ul>
     *
     * <p>该 Bean 是方案 A（LLM 路由智能体）的核心：将原本硬编码的 {@code IntentRouter.route()}
     * 升级为由模型决策的 Director，实现「路由智能体交班子智能体」。
     */
    @Bean
    public ChatClient directorChatClient(ChatClient.Builder builder,
                                         SafetyFilterAdvisor safetyFilterAdvisor,
                                         LoggingAdvisor loggingAdvisor) {
        return builder
                .defaultSystem("你是一个意图分类路由器。根据用户消息判断其意图，"
                        + "只返回如下 JSON 对象，不要输出任何 Markdown 围栏或多余文字："
                        + "{\"intent\":\"DATA_ANALYSIS\",\"reason\":\"简短依据\"}。"
                        + "intent 取值只能为 DATA_ANALYSIS、SUGGESTION 或 CHAT。"
                        + "DATA_ANALYSIS=数据分析/趋势/报告/达标率/统计；"
                        + "SUGGESTION=改善建议/怎么做/计划/方案；"
                        + "CHAT=闲聊/通用问答/问候。")
                .defaultAdvisors(safetyFilterAdvisor, loggingAdvisor)
                .defaultOptions(OpenAiChatOptions.builder()
                        .temperature(0.0)
                        .parallelToolCalls(false))
                .build();
    }

    /**
     * 报告生成专用 ChatClient：用于一次性「喂数据→出报告」任务。
     *
     * <p>复用同一 {@link ChatClient.Builder}（自动读取 model/temperature/api-key 配置），
     * 但<b>不挂 {@code MessageChatMemoryAdvisor}</b>——该 Advisor 在 before 阶段强制要求
     * {@code conversationId}，而报告生成无需多轮记忆、未传入会话 ID 会抛
     * {@code IllegalArgumentException: conversationId cannot be null} 并降级为「暂无」。
     *
     * <p>保留 safety / logging / rag 三个在无 conversationId 时均安全的 Advisor，
     * 其余配置与 {@link #chatClient} 一致。不挂对话工具（报告生成无需工具调用）。
     */
    @Bean
    public ChatClient reportChatClient(ChatClient.Builder builder,
                                       SafetyFilterAdvisor safetyFilterAdvisor,
                                       LoggingAdvisor loggingAdvisor,
                                       SafeRetrievalAdvisor safeRetrievalAdvisor) {
        return builder
                .defaultSystem(systemPromptResource)
                .defaultAdvisors(safetyFilterAdvisor, loggingAdvisor,
                        safeRetrievalAdvisor)
                // 报告生成无需并发工具调用，同样保持 false（待探针验证后可评估改 true）
                .defaultOptions(OpenAiChatOptions.builder().parallelToolCalls(false))
                .build();
    }

    /**
     * 启动时连通性验证：用最小 token 消耗向通义千问发一句话，确认打通。
     * 失败仅打印 WARN 不阻断启动（避免在本地无 Key 环境下无法启动）。
     */
    @Bean
    public ChatConnectivityProbe chatConnectivityProbe(OpenAiChatModel chatModel) {
        return new ChatConnectivityProbe(chatModel);
    }

    /** 通义千问连通性探针（阶段四验证产出）。 */
    @Slf4j
    static class ChatConnectivityProbe {

        private final OpenAiChatModel chatModel;

        ChatConnectivityProbe(OpenAiChatModel chatModel) {
            this.chatModel = chatModel;
            probe();
        }

        private void probe() {
            try {
                String reply = chatModel.call("ping");
                if (reply == null || reply.isBlank()) {
                    log.warn("[阶段四] 通义千问连通性验证返回空响应，请检查 DASHSCOPE_API_KEY 与模型权限。");
                } else {
                    log.info("[阶段四] 通义千问连通性验证成功，模型已就绪。");
                }
            } catch (Exception e) {
                log.warn("[阶段四] 通义千问连通性验证失败（不影响应用启动，请检查网络/DASHSCOPE_API_KEY）：{}",
                        e.getMessage());
            }
        }
    }
}
