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

import com.habit.agent.agent.tools.GoalTools;
import com.habit.agent.agent.tools.HabitActionTools;
import com.habit.agent.agent.tools.HabitQueryTools;
import com.habit.agent.agent.tools.HabitStatTools;

import lombok.extern.slf4j.Slf4j;

/**
 * 阶段四（Spring AI 基础）：ChatClient 配置与通义千问连通验证。
 *
 * <p>职责：
 * <ol>
 *   <li>构建全局 {@link ChatClient} Bean，注入 defaultSystem（生活习惯助手人格）。</li>
 *   <li>系统提示词从 {@code classpath:prompts/system-prompt.st} 读取，便于维护。</li>
 *   <li>启动时对通义千问做一次轻量连通性 ping，验证 API Key 与模型可用。</li>
 * </ol>
 *
 * <p>阶段四搭建基础能力；阶段五接入对话记忆 Advisor；阶段六注册业务 Tool，
 * 使 AI 助手能调用真实习惯数据作答。
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
                                 HabitQueryTools habitQueryTools,
                                 HabitStatTools habitStatTools,
                                 GoalTools goalTools,
                                 HabitActionTools habitActionTools) {
        // model / temperature 已在 application.yml 的 spring.ai.openai.chat.options 配置，
        // ChatClient.Builder 自动读取，此处不再重复设置；仅注入系统提示词、对话记忆 Advisor 与业务工具。
        // 记忆按 conversationId 隔离：调用方通过 advisorParams(ChatMemory.CONVERSATION_ID, id) 传入。
        // 阶段六：将习惯查询 / 统计 / 目标管理工具注册到 ChatClient，使 AI 能调用真实业务数据。
        // 阶段六：将习惯查询 / 统计 / 目标管理工具注册到 ChatClient，使 AI 能调用真实业务数据。
        // 使用 MethodToolCallbackProvider 自动扫描各 bean 上所有 @Tool 注解方法，
        // 避免逐方法指定 toolName 导致 build() 报 "ToolDefinition is required"。
        ToolCallbackProvider toolProvider = MethodToolCallbackProvider.builder()
                .toolObjects(habitQueryTools, habitStatTools, goalTools, habitActionTools)
                .build();
        // 关闭并行工具调用：Spring AI 2.0.0 的流式 chunk 合并器（OpenAiChatModel.ChunkMerger）
        // 不支持一条 assistant 消息包含多个 tool call。通义千问 qwen-plus 默认允许并行工具调用，
        // 复杂意图下会一次返回多个 tool call，导致流式聚合抛出
        // "no more than one tool call per message currently supported"。此处从模型侧全局禁用并行调用。
        return builder
                .defaultSystem(systemPromptResource)
                .defaultAdvisors(memoryAdvisor)
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
