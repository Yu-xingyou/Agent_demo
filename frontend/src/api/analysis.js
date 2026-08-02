import request from '../utils/request'

// 习惯分析接口封装（阶段九）
// 后端 AnalysisController: /api/analysis/{trends|overview|achievement|radar}

export function getTrends(days = 7) {
  return request.get('/analysis/trends', { params: { days } })
}

export function getOverview(days = 7) {
  return request.get('/analysis/overview', { params: { days } })
}

export function getAchievement(days = 7) {
  return request.get('/analysis/achievement', { params: { days } })
}

export function getRadar(days = 7) {
  return request.get('/analysis/radar', { params: { days } })
}
