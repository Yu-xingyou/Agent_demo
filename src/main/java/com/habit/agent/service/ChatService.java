package com.habit.agent.service;

import com.habit.agent.common.constant.AgentConstants;
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

    /**
     * 停止指定会话的流式生成。
     *
     * <p>移除该会话的生成状态标记，使正在进行的 {@link #chat(String, String)} 流
     * 在 {@code takeWhile} 判定为 false 后终止输出（见 PRD 3.3）。</p>
     *
     * @param sessionId 会话 ID
     */
    void stop(String sessionId);

    /**
     * 获取对话 id，规则：用户id_会话id。
     *
     * <p>用于记忆仓库（MongoDB {@code ai_chat_memory}）按用户维度隔离会话。
     * 对话与写入侧（{@code ChatServiceImpl#chat}）、读取侧
     * （{@code ChatSessionServiceImpl#queryBySessionId}）必须使用同一规则，
     * 故提升为接口静态方法统一维护。</p>
     *
     * @param sessionId 会话 id
     * @return 记忆库使用的对话 id
     */
    static String getConversationId(String sessionId) {
        return AgentConstants.DEFAULT_USER_ID + "_" + sessionId;
    }
}
