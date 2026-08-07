package com.habit.agent.service;

import com.habit.agent.vo.ChatEventVO;
import reactor.core.publisher.Flux;

/**
 * 流式对话服务。
 *
 * <p>返回 {@link Flux}&lt;{@link ChatEventVO}&gt;，事件类型仅含
 * {@code 1001}=数据分片 / {@code 1002}=停止（见 PRD 1.4）。</p>
 */
public interface ChatService {

    /**
     * 流式聊天。
     *
     * @param message    用户问题
     * @param sessionId 会话 ID（可为空，由调用方处理）
     * @return 流式回答事件（DATA 分片 + STOP 结束）
     */
    Flux<ChatEventVO> chat(String message, String sessionId);
}
