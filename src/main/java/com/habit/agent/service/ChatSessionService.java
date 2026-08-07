package com.habit.agent.service;

import com.habit.agent.vo.MessageVO;
import com.habit.agent.vo.SessionVO;

import java.util.List;

/**
 * 会话业务逻辑接口。
 *
 * <p>「会话」与「对话」是两个不同概念，见开发文档 1.6 节：
 * 本接口管理 session（{@code sessionId}），不参与流式对话（{@code conversationId}）。</p>
 */
public interface ChatSessionService {

    /**
     * 创建会话。生成唯一 sessionId 并持久化，同时组装首屏展示信息
     * （助手标题 / 描述 / 随机抽取 num 条快捷示例）。
     *
     * @param num 首屏展示的快捷示例数量（随机从配置中抽取）
     * @return 会话视图对象（含 sessionId + 首屏配置）
     */
    SessionVO createSession(Integer num);

    /**
     * 获取热门问题。
     *
     * <p>从配置的 {@code habit.ai.session.hot-questions} 中随机抽取 num 条返回，
     * 用于前端首屏「热门问题」榜。num 超出总数时返回全部。</p>
     *
     * @param num 返回的热门问题条数（默认 3，由 Controller 的 {@code n} 参数传入）
     * @return 热门问题列表
     */
    List<SessionVO.HotQuestion> hotQuestions(Integer num);

    /**
     * 根据会话 id 查询历史消息列表。
     *
     * <p>从会话记忆（MongoDB {@code ai_chat_memory}）中读取该会话的对话记录，
     * 仅保留用户提问（USER）与 AI 回答（ASSISTANT），过滤掉 SYSTEM / TOOL 等
     * 非展示类消息，用于前端回显历史对话（PRD 2.2）。</p>
     *
     * @param sessionId 会话 id
     * @return 消息列表（按时间正序；会话不存在或无记录时返回空列表）
     */
    List<MessageVO> queryBySessionId(String sessionId);

    /**
     * 异步更新会话标题（首条消息到达时触发）。
     *
     * <p>采用异步更新，避免阻塞主对话流、保证 AI 聊天的响应体验。
     * 仅在该会话<b>当前标题为空</b>且<b>传入 title 非空</b>时才写入，
     * 避免覆盖用户已自定义或 AI 已生成的标题。</p>
     *
     * @param sessionId 会话 ID，用于定位具体会话
     * @param title     新的会话标题（通常取首条用户消息），为空则不更新
     * @param userId    用户 ID
     */
    void update(String sessionId, String title, Long userId);
}
