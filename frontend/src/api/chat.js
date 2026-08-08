import request from '@/utils/request'

/**
 * 流式对话（SSE）。
 * 后端 POST /chat 返回 SSE 事件：data:{"eventData":...,"eventType":1001|1002}
 *   eventType 1001 -> 数据事件（AI 文本片段，eventData 为字符串）
 *   eventType 1002 -> 停止事件（本次生成结束）
 *
 * @param {Object} opts
 * @param {string} opts.question  用户问题
 * @param {string} opts.sessionId 会话 id
 * @param {Function} [opts.onData]  收到文本片段回调 (text)
 * @param {Function} [opts.onStop]  收到停止事件回调 ()
 * @param {Function} [opts.onDone]  流自然结束回调（读流 EOF）
 * @param {Function} [opts.onError] 出错回调 (err)
 * @returns {Promise<AbortController>}
 */
export function streamMessage(opts) {
  const { question, sessionId, onData, onStop, onDone, onError } = opts
  const controller = new AbortController()

  fetch('/api/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
    },
    body: JSON.stringify({ question, sessionId }),
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
          if (done) {
            onDone && onDone()
            return
          }
          buffer += decoder.decode(value, { stream: true })
          // SSE 事件以空行分隔
          const events = buffer.split('\n\n')
          buffer = events.pop() || ''
          for (const raw of events) {
            const evt = parseSseEvent(raw)
            if (!evt) continue
            if (evt.eventType === 1001) {
              // 数据事件：eventData 可能是字符串或对象
              onData && onData(typeof evt.eventData === 'string' ? evt.eventData : JSON.stringify(evt.eventData))
            } else if (evt.eventType === 1002) {
              onStop && onStop()
            }
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

/** 解析单条 SSE 事件：data:{"eventData":"...","eventType":1001} */
function parseSseEvent(raw) {
  const lines = raw.split('\n')
  const dataLines = []
  for (const line of lines) {
    if (line.startsWith('data:')) dataLines.push(line.slice(5).trim())
  }
  if (dataLines.length === 0) return null
  let data = null
  try {
    data = JSON.parse(dataLines.join('\n'))
  } catch {
    data = { eventData: dataLines.join('\n'), eventType: 0 }
  }
  return data
}

/** 停止生成 */
export function stopChat(sessionId) {
  return request.post('/chat/stop', null, { params: { sessionId } })
}
