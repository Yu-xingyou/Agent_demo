import request from '@/utils/request'

// 对应后端 ChatController 的阶段五/六接口：/api/chat、/api/chat/stream、/api/chat/stop
// 阶段六：ChatClient 已注册业务 Tool，AI 可在对话中调用真实习惯数据。

export function sendMessage(message, conversationId) {
  return request.post('/api/chat', { message, conversationId })
}

/**
 * 流式对话（SSE）。通过 fetch + ReadableStream 解析后端事件：
 *   meta      -> { conversationId, timestamp, model }
 *   tool_call -> { status, message }     （工具调用降级轮次，前端可显示过渡提示）
 *   chunk     -> { content, index }
 *   done      -> { conversationId, totalTokens, duration, streaming_mode }
 *   error     -> { errorCode, message, conversationId }
 *
 * @param {{message:string, conversationId?:string, onMeta?:Function, onToolCall?:Function, onChunk?:Function, onDone?:Function, onError?:Function}} opts
 * @returns {Promise<AbortController>} 返回 AbortController，便于调用方在超时/主动停止时中断
 */
export function streamMessage(opts) {
  const { message, conversationId, onMeta, onToolCall, onChunk, onDone, onError } = opts
  const controller = new AbortController()

  const params = new URLSearchParams({ message })
  if (conversationId) params.set('conversationId', conversationId)

  fetch(`/api/chat/stream?${params.toString()}`, {
    method: 'GET',
    headers: { Accept: 'text/event-stream' },
    signal: controller.signal,
  })
    .then((res) => {
      if (!res.ok || !res.body) {
        onError && onError({ errorCode: 'HTTP_' + res.status, message: '对话请求失败' })
        return
      }
      const reader = res.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      const read = () => {
        reader.read().then(({ done, value }) => {
          if (done) return
          buffer += decoder.decode(value, { stream: true })
          // SSE 以空行分隔事件
          const events = buffer.split('\n\n')
          buffer = events.pop() || ''
          for (const raw of events) {
            const evt = parseSseEvent(raw)
            if (!evt) continue
            if (evt.event === 'meta') onMeta && onMeta(evt.data)
            else if (evt.event === 'tool_call') onToolCall && onToolCall(evt.data)
            else if (evt.event === 'chunk') onChunk && onChunk(evt.data)
            else if (evt.event === 'done') onDone && onDone(evt.data)
            else if (evt.event === 'error') onError && onError(evt.data)
          }
          read()
        }).catch((err) => {
          if (err.name !== 'AbortError') {
            onError && onError({ errorCode: 'STREAM_ERROR', message: '对话流中断' })
          }
        })
      }
      read()
    })
    .catch((err) => {
      if (err.name !== 'AbortError') {
        onError && onError({ errorCode: 'NETWORK', message: '网络异常' })
      }
    })

  return controller
}

function parseSseEvent(raw) {
  const lines = raw.split('\n')
  let event = 'message'
  const dataLines = []
  for (const line of lines) {
    if (line.startsWith('event:')) event = line.slice(6).trim()
    else if (line.startsWith('data:')) dataLines.push(line.slice(5).trim())
  }
  if (dataLines.length === 0) return null
  let data = null
  try {
    data = JSON.parse(dataLines.join('\n'))
  } catch {
    data = dataLines.join('\n')
  }
  return { event, data }
}

export function stopChat(conversationId) {
  return request.post('/api/chat/stop', null, { params: { conversationId } })
}
