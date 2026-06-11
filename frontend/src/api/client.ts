import type { ApiResponse } from '../types/api'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
const DEFAULT_USER_ID = import.meta.env.VITE_DEV_USER_ID ?? 'stu_001'

let bearerToken: string | null = null

export function setApiBearerToken(token: string | null) {
  bearerToken = token && token.trim() ? token.trim() : null
}

export async function adminApiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('X-User-Id', import.meta.env.VITE_ADMIN_USER_ID ?? 'admin')
  return apiRequest<T>(path, { ...init, headers })
}

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = buildHeaders(init.headers, init.body)

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers,
  })
  const envelope = (await response.json()) as ApiResponse<T>
  if (!response.ok || envelope.code !== 'OK') {
    throw new Error(envelope.message || `Request failed: ${response.status}`)
  }
  return envelope.data
}

export function openSse(path: string): EventSource {
  return new EventSource(`${API_BASE_URL}${path}`)
}

export interface SseStreamHandlers {
  status?: (data: Record<string, unknown>) => void
  token?: (data: Record<string, unknown>) => void
  done?: (data: Record<string, unknown>) => void
  error?: (data: Record<string, unknown>) => void
}

export async function streamRequest(
  path: string,
  init: RequestInit,
  handlers: SseStreamHandlers,
): Promise<void> {
  const headers = buildHeaders(init.headers, init.body)
  headers.set('Accept', 'text/event-stream')

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers,
  })
  if (!response.ok) {
    throw new Error(await responseErrorMessage(response))
  }
  if (!response.body) {
    throw new Error('Streaming response body is unavailable')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    buffer = dispatchBufferedEvents(buffer, handlers)
  }

  buffer += decoder.decode()
  dispatchBufferedEvents(`${buffer}\n\n`, handlers)
}

function buildHeaders(headersInit: HeadersInit | undefined, body?: BodyInit | null): Headers {
  const headers = new Headers(headersInit)
  headers.set('X-User-Id', DEFAULT_USER_ID)
  if (bearerToken) {
    headers.set('Authorization', `Bearer ${bearerToken}`)
  }
  if (!headers.has('Content-Type') && !(body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }
  return headers
}

async function responseErrorMessage(response: Response): Promise<string> {
  try {
    const envelope = (await response.json()) as ApiResponse<unknown>
    return envelope.message || `Request failed: ${response.status}`
  } catch {
    return `Request failed: ${response.status}`
  }
}

function dispatchBufferedEvents(buffer: string, handlers: SseStreamHandlers): string {
  const normalized = buffer.replace(/\r\n/g, '\n')
  const blocks = normalized.split('\n\n')
  const remainder = blocks.pop() ?? ''
  blocks.forEach((block) => dispatchEventBlock(block, handlers))
  return remainder
}

function dispatchEventBlock(block: string, handlers: SseStreamHandlers) {
  const lines = block.split('\n').filter(Boolean)
  const eventName = lines
    .find((line) => line.startsWith('event:'))
    ?.slice('event:'.length)
    .trim()
  const dataText = lines
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.slice('data:'.length))
    .join('\n')

  if (!eventName || !dataText) return

  let data: Record<string, unknown>
  try {
    data = JSON.parse(dataText) as Record<string, unknown>
  } catch {
    throw new Error('Invalid SSE event payload')
  }

  if (eventName === 'status') handlers.status?.(data)
  if (eventName === 'token') handlers.token?.(data)
  if (eventName === 'done') handlers.done?.(data)
  if (eventName === 'error') {
    handlers.error?.(data)
    throw new Error(typeof data.message === 'string' ? data.message : 'RAG stream failed')
  }
}
