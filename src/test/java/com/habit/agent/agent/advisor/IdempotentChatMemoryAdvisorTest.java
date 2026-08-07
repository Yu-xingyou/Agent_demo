package com.habit.agent.agent.advisor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import reactor.core.publisher.Flux;

/**
 * {@link IdempotentChatMemoryAdvisor} 单元测试。
 *
 * <p>覆盖修复的两个 bug 场景：
 * <ul>
 *   <li>正常多轮对话：每轮用户消息只写入一条，助手回复正常写入；</li>
 *   <li>重试/降级重入（流式失败后同步兜底或重新订阅）：同一轮用户消息不重复写入；</li>
 *   <li>历史注入：子智能体 prompt 中按序注入完整历史（bug 2 根因验证）；</li>
 *   <li>流式失败降级：before 已写用户消息，兜底响应返回，用户消息不重复。</li>
 * </ul>
 */
class IdempotentChatMemoryAdvisorTest {

    private static final String CID = "session-1";

    private ChatMemory memory;
    private IdempotentChatMemoryAdvisor advisor;

    @BeforeEach
    void setUp() {
        memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
        advisor = new IdempotentChatMemoryAdvisor(memory);
    }

    @Test
    void multiTurn_shouldPersistExactlyOneUserMessagePerTurn() {
        adviseCall("第一轮提问", "第一轮回复");
        adviseCall("第二轮提问", "第二轮回复");

        List<Message> history = memory.get(CID);
        assertEquals(4, history.size(), "两轮对话应保存 4 条消息");
        assertEquals(2, history.stream().filter(m -> m instanceof UserMessage).count(),
                "每轮用户消息应只保存一条");
        assertEquals(2, history.stream().filter(m -> m instanceof AssistantMessage).count(),
                "每轮助手回复应各保存一条");
        // 顺序：user -> assistant 交替
        assertTrue(history.get(0) instanceof UserMessage);
        assertTrue(history.get(1) instanceof AssistantMessage);
        assertTrue(history.get(2) instanceof UserMessage);
        assertTrue(history.get(3) instanceof AssistantMessage);
    }

    @Test
    void retryReentry_shouldNotDuplicateUserMessage() {
        // 模拟：第一次调用 before() 已把用户消息写入记忆（流中断，assistant 未写入），
        // 随后重试/降级重新执行同一轮对话（AbstractSubAgent 同步兜底 / ChatServiceImpl 重试场景）。
        memory.add(CID, new UserMessage("你好"));

        adviseCall("你好", "重试后的回复");

        List<Message> history = memory.get(CID);
        assertEquals(2, history.size(), "重试后应为 [user, assistant] 各一条");
        assertEquals(1, history.stream().filter(m -> m instanceof UserMessage).count(),
                "重试重入不应重复写入用户消息");
        assertTrue(history.get(history.size() - 1) instanceof AssistantMessage,
                "重试轮次的助手回复应正常写入");
    }

    @Test
    void historyShouldBeInjectedIntoPromptInOrder() {
        adviseCall("第一轮提问", "第一轮回复");

        AtomicReference<ChatClientRequest> captured = new AtomicReference<>();
        adviseCall("第二轮提问", "第二轮回复", captured);

        List<Message> instructions = captured.get().prompt().getInstructions();
        assertEquals(4, instructions.size(), "历史应注入为 [system, user1, assistant1, user2]");
        assertTrue(instructions.get(0) instanceof SystemMessage, "system 应在最前");
        assertEquals("第一轮提问", ((UserMessage) instructions.get(1)).getText());
        assertEquals("第一轮回复", ((AssistantMessage) instructions.get(2)).getText());
        assertEquals("第二轮提问", ((UserMessage) instructions.get(3)).getText());
    }

    @Test
    void streamFailureFallback_shouldNotDuplicateUser() {
        ChatClientRequest request = ChatClientRequest.builder()
                .prompt(new Prompt(List.of(new SystemMessage("sys"), new UserMessage("提问"))))
                .context(Map.of(ChatMemory.CONVERSATION_ID, CID))
                .build();
        StreamAdvisorChain chain = new StreamAdvisorChain() {
            @Override
            public Flux<ChatClientResponse> nextStream(ChatClientRequest req) {
                return Flux.error(new RuntimeException("stream failed"));
            }

            @Override
            public List<org.springframework.ai.chat.client.advisor.api.StreamAdvisor> getStreamAdvisors() {
                return List.of();
            }

            @Override
            public StreamAdvisorChain copy(org.springframework.ai.chat.client.advisor.api.StreamAdvisor advisor) {
                return this;
            }
        };
        // 模拟 AbstractSubAgent.handleStream 的 onErrorResume：流失败后返回兜底响应
        List<ChatClientResponse> results = advisor.adviseStream(request, chain)
                .onErrorResume(e -> Flux.just(ChatClientResponse.builder()
                        .chatResponse(new ChatResponse(
                                List.of(new Generation(new AssistantMessage("兜底回复")))))
                        .build()))
                .collectList()
                .block();

        assertEquals(1, results.size(), "兜底响应应正常返回");
        List<Message> history = memory.get(CID);
        assertEquals(1, history.size(), "流失败时仅 before 写入一条用户消息");
        assertTrue(history.get(0) instanceof UserMessage);
        assertEquals(1, history.stream().filter(m -> m instanceof UserMessage).count(),
                "流式失败降级不应重复写入用户消息");
    }

    private void adviseCall(String userText, String assistantText) {
        adviseCall(userText, assistantText, null);
    }

    private void adviseCall(String userText, String assistantText,
            AtomicReference<ChatClientRequest> captured) {
        ChatClientRequest request = ChatClientRequest.builder()
                .prompt(new Prompt(List.of(new SystemMessage("sys"), new UserMessage(userText))))
                .context(Map.of(ChatMemory.CONVERSATION_ID, CID))
                .build();
        CallAdvisorChain chain = new CallAdvisorChain() {
            @Override
            public ChatClientResponse nextCall(ChatClientRequest req) {
                if (captured != null) {
                    captured.set(req);
                }
                return ChatClientResponse.builder()
                        .chatResponse(new ChatResponse(
                                List.of(new Generation(new AssistantMessage(assistantText)))))
                        .context(Map.of(ChatMemory.CONVERSATION_ID, CID))
                        .build();
            }

            @Override
            public List<org.springframework.ai.chat.client.advisor.api.CallAdvisor> getCallAdvisors() {
                return List.of();
            }

            @Override
            public CallAdvisorChain copy(org.springframework.ai.chat.client.advisor.api.CallAdvisor advisor) {
                return this;
            }
        };
        advisor.adviseCall(request, chain);
    }
}
