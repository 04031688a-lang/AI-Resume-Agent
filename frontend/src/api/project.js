import request from './request'

export function optimizeProject(data) {
  return request.post('/projects/optimize', data)
}

export function getProjects() {
  return request.get('/projects')
}

export function getProject(id) {
  return request.get(`/projects/${id}`)
}
