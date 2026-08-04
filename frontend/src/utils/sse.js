import { getToken } from '@/utils/auth'

/**
 * 基于 fetch 的 SSE POST 请求
 * 后端事件：delta（流式内容）、done（本轮结束）、error（错误）
 */
export async function postSSE(url, body, { onDelta, onEvent, onError, onDone } = {}) {
  const token = getToken()
  let res
  try {
    res = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: JSON.stringify(body)
    })
  } catch (e) {
    onError?.(e.message || '网络连接失败')
    return
  }

  if (!res.ok) {
    const err = await res.json().catch(() => null)
    onError?.(err?.message || `请求失败（${res.status}）`)
    return
  }
  if (!res.body) {
    onError?.('浏览器不支持流式响应')
    return
  }

  const reader = res.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      let sep
      while ((sep = buffer.indexOf('\n\n')) >= 0) {
        const block = buffer.slice(0, sep)
        buffer = buffer.slice(sep + 2)
        let eventName = 'message'
        for (const line of block.split('\n')) {
          if (line.startsWith('event:')) {
            eventName = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            const data = line.slice(5).trim()
            if (!data || data === '[DONE]') continue
            try {
              const obj = JSON.parse(data)
              if (eventName === 'delta') onDelta?.(obj)
              else onEvent?.(eventName, obj)
            } catch {
              /* 忽略无法解析的片段 */
            }
          }
        }
      }
    }
  } catch (e) {
    onError?.(e.message || '连接中断')
  } finally {
    onDone?.()
  }
}
