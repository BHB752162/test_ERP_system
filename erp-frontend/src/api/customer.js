import request from './request'

export function listCustomers(params) {
  return request.get('/customers', { params })
}

export function getCustomer(id) {
  return request.get(`/customers/${id}`)
}

export function createCustomer(data) {
  return request.post('/customers', data)
}

export function updateCustomer(id, data) {
  return request.put(`/customers/${id}`, data)
}

export function deleteCustomer(id) {
  return request.delete(`/customers/${id}`)
}

// Payment channels
export function listChannels(customerId) {
  return request.get(`/customers/${customerId}/payment-channels`)
}

export function createChannel(customerId, data) {
  return request.post(`/customers/${customerId}/payment-channels`, data)
}

export function updateChannel(customerId, id, data) {
  return request.put(`/customers/${customerId}/payment-channels/${id}`, data)
}

export function deleteChannel(customerId, id) {
  return request.delete(`/customers/${customerId}/payment-channels/${id}`)
}

// Contacts
export function listContacts(customerId) {
  return request.get(`/customers/${customerId}/contacts`)
}

export function createContact(customerId, data) {
  return request.post(`/customers/${customerId}/contacts`, data)
}

export function updateContact(customerId, id, data) {
  return request.put(`/customers/${customerId}/contacts/${id}`, data)
}

export function deleteContact(customerId, id) {
  return request.delete(`/customers/${customerId}/contacts/${id}`)
}
