import request from './request'
import { downloadBlob } from './download'

export function listOrders(params) {
  return request.get('/orders', { params })
}

export function getOrder(id) {
  return request.get(`/orders/${id}`)
}

export function createDraft(data) {
  return request.post('/orders', data)
}

export function updateOrder(id, data) {
  return request.put(`/orders/${id}`, data)
}

export function submitOrder(id) {
  return request.post(`/orders/${id}/submit`)
}

export function approveOrder(id) {
  return request.post(`/orders/${id}/approve`)
}

export function rejectOrder(id, data) {
  return request.post(`/orders/${id}/reject`, data)
}

export function cancelOrder(id) {
  return request.post(`/orders/${id}/cancel`)
}

export function completeOrder(id) {
  return request.post(`/orders/${id}/complete`)
}

export function getAuditLogs(orderId) {
  return request.get(`/orders/${orderId}/audit-logs`)
}

export function listAuditLogs(params) {
  return request.get('/audit-logs', { params })
}

export function listProductsForOrder() {
  return request.get('/products', { params: { page: 1, pageSize: 999 } })
}

export function listWechatsForOrder() {
  return request.get('/sales-wechats/my-wechats')
}

export function getBoundCustomers(wechatId) {
  return request.get(`/sales-bindings/bound-customers?wechatId=${wechatId}`)
}

export function exportOrders(mode, params) {
  return downloadBlob('/orders/export', { mode, ...params })
}
