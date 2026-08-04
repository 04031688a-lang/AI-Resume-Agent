import request from './request'
import { postSSE } from '@/utils/sse'

export function createInterview(data) {
  return request.post('/interviews', data)
}

export function getInterviews() {
  return request.get('/interviews')
}

export function getInterview(id) {
  return request.get(`/interviews/${id}`)
}

export function sendAnswer(id, content, handlers) {
  return postSSE(`/api/v1/interviews/${id}/messages`, { content }, handlers)
}

export function finishInterview(id) {
  return request.post(`/interviews/${id}/finish`)
}

export function getInterviewReport(id) {
  return request.get(`/interviews/${id}/report`)
}
