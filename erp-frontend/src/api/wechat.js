import request from './request'

export function listWechats(params) {
  return request.get('/sales-wechats', { params })
}

export function getWechat(id) {
  return request.get(`/sales-wechats/${id}`)
}

export function createWechat(data) {
  return request.post('/sales-wechats', data)
}

export function updateWechat(id, data) {
  return request.put(`/sales-wechats/${id}`, data)
}

export function deleteWechat(id) {
  return request.delete(`/sales-wechats/${id}`)
}

export function listMyWechats() {
  return request.get('/sales-wechats/my-wechats')
}
