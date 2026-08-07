package com.habit.agent.aigc.service;

import com.habit.agent.aigc.vo.ChatEventVO;
import com.habit.agent.common.constant.AgentConstants;
import reactor.core.publisher.Flux;

/**
 * 对话服务接口（参照天机学堂 tj-aigc ChatService）
 */
public interface ChatService {

    /**
     * 流式对话：返回 SSE 数据事件流（eventType 1001 数据 / 1002 停止）
     *
     * @param question  用户问题
     * @param sessionId 会话 id
     */
    Flux<ChatEventVO> chat(String question, String sessionId);

    /**
     * 停止生成
     *
     * @param sessionId 会话 id
     */
    void stop(String sessionId);

    /**
     * 获取对话 id，规则：用户id_会话id（单用户演示场景固定 userId=1）
     *
     * @param sessionId 会话 id
     */
    static String getConversationId(String sessionId) {
        return AgentConstants.DEFAULT_USER_ID + "_" + sessionId;
    }
}
