package com.habit.agent.service;

import java.util.List;

import com.habit.agent.common.vo.SessionVO;

/**
 * 阶段五（会话管理）SessionService 接口：维护 MongoDB 会话元数据。
 */
public interface SessionService {

    /** 用户会话列表（按最后消息时间倒序）。 */
    List<SessionVO> listSessions();

    /** 创建新会话（指定或默认标题）。 */
    SessionVO createSession(String title);

    /** 查看指定会话详情。 */
    SessionVO getSession(String conversationId);

    /** 重命名会话。 */
    SessionVO renameSession(String conversationId, String title);

    /** 关闭会话（status=CLOSED，记忆保留）。 */
    void closeSession(String conversationId);

    /** 删除会话（仅删元数据）。 */
    void deleteSession(String conversationId);

    /** 清理过期会话（status=CLOSED 且 expireAt 早于 now）。 */
    int cleanExpiredSessions();
}
