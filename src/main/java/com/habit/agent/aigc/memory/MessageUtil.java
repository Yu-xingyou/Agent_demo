package com.habit.agent.aigc.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * 消息转换工具类，提供 Message 对象与 JSON 字符串之间的转换，主要用于 MongoDB 记忆存储格式转换
 * 序列化组件使用 Jackson
 */
public class MessageUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private MessageUtil() {
    }

    /**
     * 将 Message 对象转换为 JSON 字符串（MongoDB 存储格式）
     *
     * @param message 待转换的 Spring AI 消息对象
     * @return 序列化后的 JSON 字符串
     * @throws RuntimeException 当 Jackson 序列化失败时抛出（包装 JsonProcessingException）
     */
    public static String toJson(Message message) {
        MyMessage myMessage = new MyMessage();
        myMessage.setMessageType(message.getMessageType().name());
        myMessage.setMetadata(message.getMetadata());
        myMessage.setTextContent(message.getText());
        if (message instanceof AssistantMessage assistantMessage) {
            myMessage.setToolCalls(assistantMessage.getToolCalls());
        }
        if (message instanceof UserMessage userMessage) {
            myMessage.setMedia(userMessage.getMedia());
        }
        if (message instanceof ToolResponseMessage toolResponseMessage) {
            myMessage.setToolResponses(toolResponseMessage.getResponses());
        }
        try {
            return MAPPER.writeValueAsString(myMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Message to JSON conversion failed.", e);
        }
    }

    /**
     * 将 MongoDB 存储的 JSON 字符串反序列化为对应的 Message 对象
     *
     * @param json 由 {@link #toJson(Message)} 序列化得到的 JSON 字符串
     * @return 还原后的 Spring AI 消息对象（System/User/Assistant/Tool 四种类型）
     * @throws RuntimeException 当 JSON 格式非法或反序列化失败时抛出
     */
    public static Message toMessage(String json) {
        try {
            MyMessage myMessage = MAPPER.readValue(json, MyMessage.class);
            MessageType messageType = MessageType.valueOf(myMessage.getMessageType());
            return switch (messageType) {
                case SYSTEM -> new SystemMessage(myMessage.getTextContent());
                case USER -> UserMessage.builder()
                        .text(myMessage.getTextContent())
                        .metadata(myMessage.getMetadata())
                        .media(myMessage.getMedia())
                        .build();
                case ASSISTANT -> new AssistantMessage(myMessage.getTextContent(),
                        myMessage.getMetadata(), myMessage.getToolCalls());
                case TOOL -> new ToolResponseMessage(myMessage.getToolResponses(), myMessage.getMetadata());
            };
        } catch (Exception e) {
            throw new RuntimeException("Message data conversion failed.", e);
        }
    }
}
