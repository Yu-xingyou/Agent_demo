import request from '@/utils/request'
import { isRouteAgentName } from '@/constants/agent'

// 后端 /api/chat 是 POST + text/event-stream，需要读取 X-Session-Id 响应头，
// 因此用原生 fetch 实现，绕过 request（request 基于 axios，对 SSE 头读取不友好）。
export async function streamMessage({ message, conversationId = null, signal, onMeta, onChunk, onDone, onError }) {
  const base = (import.meta.env.VITE_API_BASE || '').replace(/\/$/, '')
  const url = `${base}/api/chat`
  const body = { message }
  if (conversationId) body.conversationId = conversationId

  let sessionId = null
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
      signal
    })
    if (!resp.ok) {
      const text = await resp.text().catch(() => '')
      onError && onError(new Error(`流式请求失败: ${resp.status} ${text}`))
      return
    }
    // 会话 ID 由后端通过响应头 X-Session-Id 下发（仅在首次创建会话时返回）
    const sid = resp.headers.get('X-Session-Id')
    if (sid) sessionId = sid

    const reader = resp.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      // SSE 以空行分隔事件
      const parts = buffer.split('\n\n')
      buffer = parts.pop() || ''
      for (const part of parts) {
        const line = part.split('\n').find((l) => l.startsWith('data:'))
        if (!line) continue
        const payload = line.slice(5).trim()
        if (!payload) continue
        let event
        try {
          event = JSON.parse(payload)
        } catch (e) {
          continue
        }
        const { eventType, eventData } = event
        if (eventType === 1001) {
          // 普通文本分片，过滤路由智能体内部名称
          if (isRouteAgentName(eventData)) continue
          onChunk && onChunk({ content: eventData, fullText: eventData })
        } else if (eventType === 1002) {
          // 流式完成（eventData 为纯文本提示，无统计字段）
          onDone && onDone({ conversationId: sessionId, text: eventData })
        }
      }
    }
  } catch (err) {
    if (err && err.name === 'AbortError') {
      onDone && onDone({ conversationId: sessionId })
      return
    }
    onError && onError(err)
  }
  return sessionId
}

// 停止当前流式响应
export function stopChat(conversationId) {
  return request.post('/chat/stop', null, {
    params: conversationId ? { conversationId } : {}
  })
}

// 加载会话历史（后端 GET /api/sessions/{id} 返回 List<MessageVO>，字段 type/content）
export function getChatHistory(sessionId) {
  return request.get(`/sessions/${sessionId}`)
}
