import request from '@/utils/request'

// 以下接口对应开发流程阶段九（统计/图表），本期前端仅预留容器，待后端实现后接入。

export function getTrends(params) {
  return request.get('/analysis/trends', { params })
}

export function getAchievement(params) {
  return request.get('/analysis/achievement', { params })
}

export function getOverview() {
  return request.get('/analysis/overview')
}

export function getRadar(params) {
  return request.get('/analysis/radar', { params })
}
