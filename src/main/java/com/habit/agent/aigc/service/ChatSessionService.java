package com.habit.agent.aigc.service;

import com.habit.agent.aigc.vo.MessageVO;
import com.habit.agent.aigc.vo.SessionVO;

import java.util.List;

/**
 * 会话服务接口（扩展列表/删除）
 */
public interface ChatSessionService {

    /**
     * 创建会话
     *
     * @param num 热门示例数量（非法值默认 3）
     * @return 新建会话视图（含标题、描述、随机示例）
     */
    SessionVO createSession(Integer num);

    /**
     * 获取热门示例
     *
     * @param num 示例数量（非法值默认 3）
     * @return 随机抽取的示例列表
     */
    List<SessionVO.Example> hotExamples(Integer num);

    /**
     * 根据会话 id 查询历史消息（读 chatMessage 集合）
     *
     * @param sessionId 会话 id
     * @return 该会话下用户与 AI 的历史消息列表（按时间升序）
     */
    List<MessageVO> queryBySessionId(String sessionId);

    /**
     * 会话列表（按最后消息时间倒序）
     *
     * @return 当前用户全部会话视图列表
     */
    List<SessionVO> listSessions();

    /**
     * 删除会话（级联清理 chatSession + chatMessage + chatMemory）
     *
     * @param sessionId 会话 id
     */
    void deleteSession(String sessionId);
}
