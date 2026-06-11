import { apiRequest } from './client'
import type { AnswerSubmitResponse } from '../types/api'

export interface AnswerSubmitPayload {
  learnerId: string
  questionId: string
  answer: string
}

export function submitAnswer(payload: AnswerSubmitPayload): Promise<AnswerSubmitResponse> {
  return apiRequest<AnswerSubmitResponse>('/api/assessment/answers', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}
