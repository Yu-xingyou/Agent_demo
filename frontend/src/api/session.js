import request from '@/utils/request'

/** 获取当前用户所有会话列表（按最后消息时间倒序） */
export function listSessions() {
  return request.get('/sessions')
}

/** 获取单个会话详情 */
export function getSession(conversationId) {
  return request.get(`/sessions/${conversationId}`)
}

/** 重命名会话 */
export function renameSession(conversationId, title) {
  return request.put(`/sessions/${conversationId}/rename`, null, { params: { title } })
}

/** 关闭会话 */
export function closeSession(conversationId) {
  return request.post(`/sessions/${conversationId}/close`)
}

/** 删除会话 */
export function deleteSession(conversationId) {
  return request.delete(`/sessions/${conversationId}`)
}
