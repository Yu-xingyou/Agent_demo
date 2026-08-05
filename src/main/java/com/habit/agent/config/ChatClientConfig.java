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

import com.habit.agent.agent.advisor.ContextInjectionAdvisor;
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
 *       工具执行循环内部管理对话历史，无需手动注册。</li>
 *   <li>记忆 Advisor 优先级调整：{@code Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER}
 *       从 1.x 的 {@code HIGHEST+1000} 调整为 {@code HIGHEST+200}，使记忆 Advisor 位于
 *       工具 Advisor 之外，工具调用中间消息不再写入 ChatMemory（避免存储实现不支持工具消息类型）。</li>
 *   <li>{@code streamToolCallResponses} 已移除：中间工具调用请求不再流式发出，
 *       仅最终助手回复流式输出（纯文本轮次）或分片降级（工具调用轮次）。</li>
 *   <li>{@code parallelToolCalls} 设为 {@code false}：规避 DashScope 流式工具调用分片
 *       缺 index 字段导致 ChunkMerger 断言失败（单 tool call 时通过 ChunkMerger 合并更稳定）。</li>
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
 *   <tr><td>HIGHEST+100</td><td>{@link ContextInjectionAdvisor}</td>
 *       <td>注入日期与今日打卡概况，须在记忆之前以便改写后的消息入库</td></tr>
 *   <tr><td>HIGHEST+200</td><td>{@code MessageChatMemoryAdvisor}</td>
 *       <td>多轮对话记忆（现有，{@code DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER}）</td></tr>
 *   <tr><td>HIGHEST+300</td><td>{@link SafeRetrievalAdvisor}</td>
 *       <td>RAG 知识检索增强，带失败降级；置于记忆之后避免知识片段污染记忆窗口</td></tr>
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
                                 ContextInjectionAdvisor contextInjectionAdvisor,
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
                .defaultAdvisors(safetyFilterAdvisor, loggingAdvisor, contextInjectionAdvisor,
                        memoryAdvisor, safeRetrievalAdvisor)
                .defaultTools(toolProvider)
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
