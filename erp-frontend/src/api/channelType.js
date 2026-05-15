import request from './request'

export function listChannelTypes() {
  return request.get('/payment-channel-types')
}

export function getChannelType(id) {
  return request.get(`/payment-channel-types/${id}`)
}

export function createChannelType(data) {
  return request.post('/payment-channel-types', data)
}

export function updateChannelType(id, data) {
  return request.put(`/payment-channel-types/${id}`, data)
}

export function deleteChannelType(id) {
  return request.delete(`/payment-channel-types/${id}`)
}
