import request from './request'

export function listBindings(params) {
  return request.get('/sales-bindings', { params })
}

export function createBinding(data) {
  return request.post('/sales-bindings', data)
}

export function unbind(id) {
  return request.delete(`/sales-bindings/${id}`)
}

export function getBoundCustomers(salesAccountId) {
  return request.get(`/sales-bindings/bound-customers?salesAccountId=${salesAccountId}`)
}

export function getBoundAccounts(customerId) {
  return request.get(`/sales-bindings/bound-accounts?customerId=${customerId}`)
}
