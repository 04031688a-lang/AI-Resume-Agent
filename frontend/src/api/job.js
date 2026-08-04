import request from './request'

export function getJobs(params) {
  return request.get('/jobs', { params })
}

export function getJob(id) {
  return request.get(`/jobs/${id}`)
}

export function matchJob(id) {
  return request.post(`/jobs/${id}/match`)
}

export function getMatches() {
  return request.get('/matches')
}

export function createJob(data) {
  return request.post('/jobs', data)
}

export function updateJob(id, data) {
  return request.put(`/jobs/${id}`, data)
}

export function toggleJobStatus(id, status) {
  return request.put(`/jobs/${id}/status`, { status })
}

export function deleteJob(id) {
  return request.delete(`/jobs/${id}`)
}
