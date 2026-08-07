import request from '@/utils/request'

/** 获取历史会话列表（按最后消息时间倒序分组） */
export function listSessions() {
  return request.get('/sessions/history')
}

/** 获取单个会话详情（对话记录） */
export function getSession(conversationId) {
  return request.get(`/sessions/${conversationId}`)
}

/** 重命名会话标题 */
export function renameSession(conversationId, title) {
  return request.put('/sessions/history', null, { params: { sessionId: conversationId, title } })
}

/** 删除会话（同时清空对话记忆） */
export function deleteSession(conversationId) {
  return request.delete('/sessions/history', { params: { sessionId: conversationId } })
}
