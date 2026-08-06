package com.habit.agent.agent.router;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.stereotype.Component;

import com.habit.agent.common.constant.AgentConstants;

import lombok.extern.slf4j.Slf4j;

import reactor.core.publisher.Flux;

/**
 * 阶段九子智能体：数据分析智能体。
 *
 * <p>负责处理需要查询/分析习惯数据的请求，可借助工具（打卡查询、统计分析）获取真实数据。
 *
 * <p>流式路径（{@link #handleStream}）采用两段式，以规避 Spring AI 2.0.0 + DashScope 在工具轮次下
 * {@code ChatClientResponse} 分片聚合触发的 {@code ChunkMerger} NoSuchElementException：
 * <ol>
 *   <li>先用 {@code .call().content()} 同步完成工具取数（该路径在 DashScope 下已验证稳定）；</li>
 *   <li>插入一个携带 toolCall 的响应，供前端展示「数据查询完成」提示；</li>
 *   <li>再基于已取数据，用 {@code stream().content()} 逐字流式输出最终回答。</li>
 * </ol>
 *
 * @see AbstractSubAgent 基类封装了 call/stream 模板与降级逻辑。
 */
@Slf4j
@Component
public class DataAnalysisAgent extends AbstractSubAgent {

    public DataAnalysisAgent(ChatClient chatClient) {
        super(chatClient);
    }

    @Override
    public IntentRouter.Intent intent() {
        return IntentRouter.Intent.DATA_ANALYSIS;
    }

    @Override
    public String roleName() {
        return "数据分析智能体";
    }

    @Override
    protected String systemPrompt() {
        return """
                你是一个「数据分析智能体」，专门负责处理需要查询或分析用户习惯数据的请求。
                当用户的问题涉及以下任意情况时，请优先调用工具获取真实数据：
                - 查询某习惯的打卡记录、连续打卡天数；
                - 统计某时间段的完成率、趋势、环比；
                - 对比多个习惯的表现。
                你有可用的工具（打卡查询、统计分析），请主动使用，不要凭空编造数据。
                在给出分析结论时，用简洁、有条理的中文说明数据含义与可执行建议。
                """;
    }

    @Override
    protected String fallbackMessage() {
        return "抱歉，数据分析服务暂时不可用，请稍后再试。";
    }

    @Override
    public Flux<ChatClientResponse> handleStream(String message, String conversationId) {
        String convId = conversationId == null ? AgentConstants.DEFAULT_USERNAME : conversationId;
        try {
            // 第一段：同步工具取数（DashScope 下 .call() 工具轮已验证稳定），拿到真实数据
            String toolResult = chatClient.prompt()
                    .system(systemPrompt())
                    .user(message)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, convId))
                    .call()
                    .content();

            // 第二段：基于已取真实数据，用 stream().content() 逐字流式输出最终回答，
            // 全程绕开 DashScope 工具轮次下 chatClientResponse() 分片聚合的 ChunkMerger 缺陷
            return chatClient.prompt()
                    .system(systemPrompt() + "\n下面是为用户查询到的真实数据，请基于它作答：\n" + toolResult)
                    .user(message)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, convId))
                    .stream()
                    .content()
                    .map(text -> ChatClientResponse.builder()
                            .chatResponse(new ChatResponse(
                                    List.of(new Generation(new AssistantMessage(text)))))
                            .build())
                    .onErrorResume(e -> {
                        log.warn("[{}] 流式调用失败，降级为提示", roleName(), e);
                        ChatResponse fb = new ChatResponse(
                                List.of(new Generation(new AssistantMessage(fallbackMessage()))));
                        return Flux.just(ChatClientResponse.builder().chatResponse(fb).build());
                    });
        } catch (Exception e) {
            log.warn("[{}] 工具取数失败，降级为提示", roleName(), e);
            ChatResponse fallback = new ChatResponse(
                    List.of(new Generation(new AssistantMessage(fallbackMessage()))));
            return Flux.just(ChatClientResponse.builder().chatResponse(fallback).build());
        }
    }
}
