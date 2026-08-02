import request from '@/utils/request'

export function getActive() {
  return request.get('/goals/active')
}

export function getActiveWithCustom() {
  return request.get('/goals/active-with-custom')
}

export function save(record) {
  return request.post('/goal-records/records', record)
}

export function listRecordsByDate(date) {
  return request.get('/goal-records/records/date', { params: { date } })
}
