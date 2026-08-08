package com.tianji.aigc.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.tianji.aigc.Constant.Constant;
import com.tianji.aigc.config.SystemPromptConfig;
import com.tianji.aigc.enums.ChatEventTypeEnum;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.vo.ChatEventVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final SystemPromptConfig systemPromptConfig;
    private final ChatMemory chatMemory;
    private final StringRedisTemplate stringRedisTemplate;
    //通过一个容器，保存当前回话ID，以及是否继续生成的标识
    //容器实现1.使用Mapper，2.使用分布式场景，使用Redis

    private static final String GENERATE_STATUS_KEY = "GENERATE_STATUS";
    private static final Map<String, Boolean> GENERATE_STATUS = new ConcurrentHashMap<>();

    @Override
    public Flux<ChatEventVO> chat(String question, String sessionId) {
        //获取到的对话id，将会话id转化为对话id
        var conversationId = ChatService.getConversationId(sessionId);
        var outputBuilder = new StringBuilder();
        //生成请求id
        var requestId = IdUtil.simpleUUID();
        BoundHashOperations<String, Object, Object> hasOps = this.stringRedisTemplate.boundHashOps("GENERATE_STATUS_KEY");
        return this.chatClient.prompt()
                .system(promptSystemSpec -> promptSystemSpec.text(this.systemPromptConfig.
                        getChatSystemMessage().get())
                        .params(Map.of("now", DateUtil.now()))
                )
                        .advisor(advisor -> advisor.param(ChatMemory.CONVERSATION_ID))
                .toolContext(Map.of(Constant.REQUEST_ID,requestId))//向工具中去传递参数
                .user(question)
                .stream()
                .chatResponse()
                .doFirst(()->hasOps.put(sessionId,"true"))
                .doOnError(throwable -> hasOps.delete(sessionId))
                .doOnComplete(()->hasOps.delete(sessionId))
                .doOnCancel(()->{
                    this.saveStopHistoryRecord(conversationId,outputBuilder.toString());
                })
                .takeWhile(chatResponse -> hasOps.get(sessionId)!=null)//后续生成条件
                .map(chatResponse -> (
                        var text =chatResponse.getResult().getOutput().getText();
                        return ChatEventVO.builder()
                                .enventData(text)
                                .enventType(ChatEventTypeEnum.DATA.getValue())
                                .build();
                ));
                .content()
                .concatWith(Flux.ChatEventVO.builder()
                        .enventType(ChatEventTypeEnum.STOP.getValue())
                        .build());//结束对话
    }

    @Override
    public void stop(String sessionId) {
        GENERATE_STATUS.remove(sessionId);
    }

    /**
     * 保存停止输出的记录
     *
     * @param conversationId 会话id
     * @param content        大模型输出的内容
     */
    private void saveStopHistoryRecord(String conversationId, String content) {
        BoundHashOperations<String, Object, Object> hasOps = this.stringRedisTemplate.boundHashOps("GENERATE_STATUS_KEY");
        hasOps.delete(conversationId, new AssistantMessage(content));
    }
}
