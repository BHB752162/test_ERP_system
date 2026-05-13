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

// Categories
export function listCategories() {
  return request.get('/product-categories')
}

export function getCategory(id) {
  return request.get(`/product-categories/${id}`)
}

export function createCategory(data) {
  return request.post('/product-categories', data)
}

export function updateCategory(id, data) {
  return request.put(`/product-categories/${id}`, data)
}

export function deleteCategory(id) {
  return request.delete(`/product-categories/${id}`)
}
