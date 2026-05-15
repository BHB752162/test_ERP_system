export async function downloadBlob(url, params = {}) {
  const token = localStorage.getItem('token')
  const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
  const query = new URLSearchParams(params).toString()
  const fullUrl = query ? `${baseURL}${url}?${query}` : `${baseURL}${url}`

  const response = await fetch(fullUrl, {
    headers: { Authorization: `Bearer ${token}` }
  })

  const contentType = response.headers.get('content-type') || ''

  // Backend may return JSON error wrapped in ApiResponse with HTTP 200
  if (contentType.includes('json') || !response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new Error(body.message || '导出失败')
  }

  return response.blob()
}
