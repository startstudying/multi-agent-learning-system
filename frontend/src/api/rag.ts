import { apiRequest, openSse, streamRequest } from './client'
import type { RagQueryResponse } from '../types/api'

export interface RagQueryPayload {
  kbIds: string[]
  question: string
  topK: number
  requestId?: string
}

export function queryRag(payload: RagQueryPayload): Promise<RagQueryResponse> {
  return apiRequest<RagQueryResponse>('/api/rag/query', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function streamChat(sessionId: string, question: string, kbIds: string[]): EventSource {
  const params = new URLSearchParams({ question })
  kbIds.forEach((kbId) => params.append('kbIds', kbId))
  return openSse(`/api/chat/sessions/${sessionId}/stream?${params.toString()}`)
}

export interface RagStreamHandlers {
  onStatus?: (data: { stage?: string }) => void
  onToken?: (data: { text?: string }) => void
  onDone?: (data: Partial<RagQueryResponse>) => void
  onError?: (data: { message?: string }) => void
}

export function streamRagQuery(payload: RagQueryPayload, handlers: RagStreamHandlers): Promise<void> {
  return streamRequest(
    '/api/rag/query/stream',
    {
      method: 'POST',
      body: JSON.stringify(payload),
    },
    {
      status: (data) => handlers.onStatus?.(data),
      token: (data) => handlers.onToken?.(data),
      done: (data) => handlers.onDone?.(data as Partial<RagQueryResponse>),
      error: (data) => handlers.onError?.(data),
    },
  )
}
