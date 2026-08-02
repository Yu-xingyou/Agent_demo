import request from '@/utils/request'

// 以下接口对应开发流程阶段五（AI 流式对话），本期前端仅预留骨架，待后端实现后接入。

export function sendMessage(message) {
  return request.post('/chat/message', { message })
}

export function streamMessage(message) {
  // 阶段五：通过 EventSource / fetch stream 消费 SSE
  return request.post('/chat/stream', { message }, { responseType: 'stream' })
}

export function stop() {
  return request.post('/chat/stop')
}
