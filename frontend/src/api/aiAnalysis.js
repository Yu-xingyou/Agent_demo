import request from '@/utils/request'

// 阶段十：AI 智能分析接口封装
export function generateAnalysis(days = 7) {
  return request.post('/ai-analysis/generate', null, { params: { days } })
}

export function getAnalysisTask(id) {
  return request.get(`/ai-analysis/${id}`)
}

export function listAnalysisHistory(limit = 10) {
  return request.get('/ai-analysis/history', { params: { limit } })
}

export function getLatestAnalysis() {
  return request.get('/ai-analysis/latest')
}

export function regenerateAnalysis(days = 7) {
  return request.post('/ai-analysis/regenerate', null, { params: { days } })
}
