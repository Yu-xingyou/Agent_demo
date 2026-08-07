package com.habit.agent.service;

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
}
