package com.habit.agent.agent.advisor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.core.Ordered;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 幂等对话记忆 Advisor —— 替代官方 {@code MessageChatMemoryAdvisor}。
 *
 * <p><b>为什么替换官方实现</b>（经反编译 spring-ai-client-chat-2.0.0.jar 确认）：
 * 官方 {@code MessageChatMemoryAdvisor.before()} 在<b>每次请求（每次订阅）</b>执行时都
 * 无条件调用 {@code chatMemory.add(conversationId, userMessage)} 写入用户消息。当调用链因
 * 流式失败降级（{@code AbstractSubAgent} 同步兜底）或服务层重试（冷 Flux 重新订阅）被重复
 * 执行时，同一轮用户消息会被写入多次，造成聊天记录中用户消息重复；同时 memory 序列错乱
 * （{@code [user, user, assistant]} 或孤 user）会污染后续轮次的历史注入，导致路由智能体
 * 场景下子智能体「看不到过去的消息」。
 *
 * <p><b>本类修复策略</b>：
 * <ul>
 *   <li>before：仍注入历史到 prompt（system 之后，照搬官方注入/去重逻辑）；
 *       <b>幂等写 user</b> —— 先取记忆最后一条，若已是同内容 UserMessage（说明本轮已写入，
 *       如重试/降级重入）则跳过，否则写入；</li>
 *   <li>after：从聚合后的响应提取 assistant 消息，同样按「最后一条同内容」幂等写入；</li>
 *   <li>stream：照搬官方 {@code ChatClientMessageAggregator} 聚合模式，仅在聚合完成后调用
 *       once after；</li>
 *   <li>未传 {@code conversationId} 的调用（如标题生成、意图路由等内部元调用）直接放行，
 *       不注入、不写入，防御性跳过。</li>
 * </ul>
 *
 * <p>执行顺序 {@code HIGHEST_PRECEDENCE + 200}，与官方一致：保持 RAG（+150）在记忆之前、
 * ToolCalling（+300）在记忆之后，不破坏既有 Advisor 链序。
 */
@Slf4j
public class IdempotentChatMemoryAdvisor implements CallAdvisor, StreamAdvisor {

    /** 与官方 MessageChatMemoryAdvisor 一致的执行顺序。 */
    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 200;

    private final ChatMemory chatMemory;

    public IdempotentChatMemoryAdvisor(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientRequest advisedRequest = before(request);
        ChatClientResponse response = chain.nextCall(advisedRequest);
        return after(response);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        // 与官方 MessageChatMemoryAdvisor.adviseStream 相同的框架受控聚合模式：
        // before 注入历史并幂等写 user → 下游链（记忆之后的 ToolCalling + 模型）→ 聚合完成后 once after。
        return Mono.just(request)
                .map(this::before)
                .flatMapMany(chain::nextStream)
                .transform(flux -> new ChatClientMessageAggregator()
                        .aggregateChatClientResponse(flux, this::after));
    }

    /**
     * 请求前处理：
     * <ol>
     *   <li>从记忆读取历史，若尚未注入则注入到 prompt（system 之后，首个 SystemMessage 移到头部）；</li>
     *   <li>幂等写入用户消息（记忆最后一条已是同内容 UserMessage 则跳过）。</li>
     * </ol>
     */
    private ChatClientRequest before(ChatClientRequest request) {
        String conversationId = resolveConversationId(request.context());
        if (conversationId == null) {
            // 未传会话 ID（标题生成等内部元调用）：不注入历史、不写记忆，直接放行
            return request;
        }
        List<Message> memoryMessages = chatMemory.get(conversationId);
        List<Message> promptMessages = request.prompt().getInstructions();
        List<Message> processedMessages = new ArrayList<>();
        if (!isMemoryAlreadyInPrompt(promptMessages, memoryMessages)) {
            processedMessages.addAll(memoryMessages);
        }
        processedMessages.addAll(promptMessages);
        // 将首个 SystemMessage 移到最前，保证系统提示词在消息序列头部（照搬官方行为）
        for (int i = 0; i < processedMessages.size(); i++) {
            if (processedMessages.get(i) instanceof SystemMessage) {
                Message systemMessage = processedMessages.remove(i);
                processedMessages.add(0, systemMessage);
                break;
            }
        }
        ChatClientRequest processedRequest = request.mutate()
                .prompt(request.prompt().mutate().messages(processedMessages).build())
                .build();
        // 幂等写用户消息
        Message userMessage = processedRequest.prompt().getLastUserOrToolResponseMessage();
        if (userMessage != null) {
            addUserMessageIdempotently(conversationId, userMessage);
        }
        return processedRequest;
    }

    /**
     * 请求后处理：从聚合后的响应提取 assistant 消息，按「最后一条同内容」幂等写入记忆。
     */
    private ChatClientResponse after(ChatClientResponse response) {
        String conversationId = resolveConversationId(response.context());
        if (conversationId == null
                || response.chatResponse() == null
                || response.chatResponse().getResults() == null) {
            return response;
        }
        response.chatResponse().getResults().stream()
                .map(Generation::getOutput)
                .filter(Objects::nonNull)
                .forEach(assistant -> addMessageIdempotently(conversationId, assistant));
        return response;
    }

    /** 幂等写用户消息：记忆最后一条已是同内容消息则跳过，避免重试/降级重入造成重复。 */
    private void addUserMessageIdempotently(String conversationId, Message userMessage) {
        List<Message> current = chatMemory.get(conversationId);
        if (!current.isEmpty() && isSameMessage(current.get(current.size() - 1), userMessage)) {
            return;
        }
        chatMemory.add(conversationId, userMessage);
    }

    /** 幂等写单条消息：记忆最后一条已是同内容消息则跳过。 */
    private void addMessageIdempotently(String conversationId, Message message) {
        List<Message> current = chatMemory.get(conversationId);
        if (!current.isEmpty() && isSameMessage(current.get(current.size() - 1), message)) {
            return;
        }
        chatMemory.add(conversationId, message);
    }

    /** 按「消息类型 + 文本内容」比较两条消息是否相同（幂等判断依据）。 */
    private boolean isSameMessage(Message a, Message b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.getClass() != b.getClass()) {
            return false;
        }
        return Objects.equals(a.getText(), b.getText());
    }

    /** 从 advisor context 读取会话 ID；未传则返回 null。 */
    private String resolveConversationId(Map<String, Object> context) {
        Object id = context.get(ChatMemory.CONVERSATION_ID);
        return id == null ? null : String.valueOf(id);
    }

    /** 判断历史消息是否已被注入到 prompt（内容相等子序列匹配，照搬官方逻辑）。 */
    private boolean isMemoryAlreadyInPrompt(List<Message> promptMessages, List<Message> memoryMessages) {
        if (memoryMessages.isEmpty()) {
            return true;
        }
        if (promptMessages.size() < memoryMessages.size()) {
            return false;
        }
        for (int i = 0; i <= promptMessages.size() - memoryMessages.size(); i++) {
            if (startsWith(promptMessages, memoryMessages, i)) {
                return true;
            }
        }
        return false;
    }

    /** 判断 promptMessages 从 offset 开始是否与 memoryMessages 逐条内容相等。 */
    private boolean startsWith(List<Message> promptMessages, List<Message> memoryMessages, int offset) {
        if (promptMessages.size() - offset < memoryMessages.size()) {
            return false;
        }
        for (int i = 0; i < memoryMessages.size(); i++) {
            if (!promptMessages.get(i + offset).equals(memoryMessages.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}
