import request from '@/utils/request'

export function getToday() {
  return request.get('/habits/today')
}

export function getRecent(days) {
  return request.get(`/habits/recent/${days}`)
}

export function listByRange(startDate, endDate) {
  return request.get('/habits', { params: { startDate, endDate } })
}

export function listAll() {
  return request.get('/habits/all')
}

export function saveOrUpdate(data) {
  return request.post('/habits', data)
}

export function getById(id) {
  return request.get(`/habits/${id}`)
}

export function remove(id) {
  return request.delete(`/habits/${id}`)
}
