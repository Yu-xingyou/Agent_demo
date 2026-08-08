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
import org.springframework.ai.content.Media;

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
