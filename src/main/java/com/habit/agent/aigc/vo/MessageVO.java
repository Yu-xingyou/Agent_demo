package com.habit.agent.aigc.vo;

import com.habit.agent.aigc.enums.MessageTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 消息 VO（历史回显，参照天机学堂 tj-aigc MessageVO）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageVO {

    /**
     * 消息类型：USER 用户提问 / ASSISTANT AI回答
     */
    private MessageTypeEnum type;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 附加参数
     */
    private Map<String, Object> params;
}
