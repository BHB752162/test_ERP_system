import request from './request'

export function listProducts(params) {
  return request.get('/products', { params })
}

export function getProduct(id) {
  return request.get(`/products/${id}`)
}

export function createProduct(data) {
  return request.post('/products', data)
}

export function updateProduct(id, data) {
  return request.put(`/products/${id}`, data)
}

export function deleteProduct(id) {
  return request.delete(`/products/${id}`)
}

export function listSets() {
  return request.get('/products/sets')
}

export function listChildren(id) {
  return request.get(`/products/${id}/children`)
}

export function updateChildren(id, childIds) {
  return request.put(`/products/${id}/children`, childIds)
}
