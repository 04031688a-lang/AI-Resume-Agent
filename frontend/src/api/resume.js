import request from './request'

export function uploadResume(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/resumes', formData)
}

export function getResumes() {
  return request.get('/resumes')
}

export function getResume(id) {
  return request.get(`/resumes/${id}`)
}

export function deleteResume(id) {
  return request.delete(`/resumes/${id}`)
}

export function analyzeResume(id) {
  return request.post(`/resumes/${id}/analyze`)
}

export function getResumeAnalysis(id) {
  return request.get(`/resumes/${id}/analysis`)
}
