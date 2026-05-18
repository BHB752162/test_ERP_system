import request from './request'

export function listSalesAccounts(params) {
  return request.get('/sales-accounts', { params })
}

export function getSalesAccount(id) {
  return request.get(`/sales-accounts/${id}`)
}

export function createSalesAccount(data) {
  return request.post('/sales-accounts', data)
}

export function updateSalesAccount(id, data) {
  return request.put(`/sales-accounts/${id}`, data)
}

export function deleteSalesAccount(id) {
  return request.delete(`/sales-accounts/${id}`)
}

export function getBoundUserIds(accountId) {
  return request.get(`/sales-accounts/${accountId}/users`)
}

export function bindUser(accountId, userId) {
  return request.post(`/sales-accounts/${accountId}/users`, { userId })
}

export function unbindUser(accountId, userId) {
  return request.delete(`/sales-accounts/${accountId}/users/${userId}`)
}
