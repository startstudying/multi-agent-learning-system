import { apiRequest, streamRequest } from './client'
import type { AiQaResponse } from '../types/api'

export interface AiQaPayload {
  answerMode?: string
  kbIds: string[]
  question: string
  courseId?: string
  topK: number
  requestId?: string
}

export function queryAiQa(payload: AiQaPayload): Promise<AiQaResponse> {
  return apiRequest<AiQaResponse>('/api/ai/qa', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export interface AiQaStreamHandlers {
  onStatus?: (data: { stage?: string }) => void
  onToken?: (data: { text?: string }) => void
  onDone?: (data: AiQaResponse & { latencyMs?: number }) => void
  onError?: (data: { message?: string }) => void
}

export function streamAiQa(payload: AiQaPayload, handlers: AiQaStreamHandlers): Promise<void> {
  return streamRequest(
    '/api/ai/qa/stream',
    {
      method: 'POST',
      body: JSON.stringify(payload),
    },
    {
      status: (data) => handlers.onStatus?.(data),
      token: (data) => handlers.onToken?.(data),
      done: (data) => handlers.onDone?.(data as unknown as AiQaResponse & { latencyMs?: number }),
      error: (data) => handlers.onError?.(data),
    },
  )
}
