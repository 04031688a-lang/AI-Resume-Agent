import request from './request'

export function getAdminUsers(params) {
  return request.get('/admin/users', { params })
}

export function updateUserStatus(id, status) {
  return request.put(`/admin/users/${id}/status`, { status })
}

export function deleteAdminUser(id) {
  return request.delete(`/admin/users/${id}`)
}

export function getAdminStats() {
  return request.get('/admin/stats')
}

export function getAiConfig() {
  return request.get('/admin/ai-config')
}

export function saveAiConfig(data) {
  return request.put('/admin/ai-config', data)
}
