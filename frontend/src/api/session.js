import request from '@/utils/request'

/**
 * 会话管理（会话侧边栏扩展）。
 * 返回结构均为 { code, message, data }，request.js 已解包 data。
 */

/** 新建会话（返回 { sessionId, title, describe, examples }） */
export function createSession(n = 3) {
  return request.post('/session', null, { params: { n } })
}

/** 热门示例列表 */
export function hotExamples(n = 3) {
  return request.get('/session/hot', { params: { n } })
}

/** 会话列表（按最后消息时间倒序） */
export function listSessions() {
  return request.get('/session/list')
}

/** 查询单个会话历史消息（[{ type, content }]） */
export function getSession(sessionId) {
  return request.get(`/session/${sessionId}`)
}

/** 删除会话（级联清理） */
export function deleteSession(sessionId) {
  return request.delete(`/session/${sessionId}`)
}
