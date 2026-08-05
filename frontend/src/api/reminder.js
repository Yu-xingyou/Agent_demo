import request from '@/utils/request'

// 阶段十：打卡提醒接口封装
export function listReminders() {
  return request.get('/reminder')
}

export function createReminder(payload) {
  return request.post('/reminder', payload)
}

export function updateReminder(id, payload) {
  return request.put(`/reminder/${id}`, payload)
}

export function deleteReminder(id) {
  return request.delete(`/reminder/${id}`)
}

export function toggleReminder(id, active) {
  return request.put(`/reminder/${id}/toggle`, null, { params: { active } })
}
