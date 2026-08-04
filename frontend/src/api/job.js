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
