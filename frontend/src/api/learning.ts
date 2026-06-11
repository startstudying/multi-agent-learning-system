import { apiRequest } from './client'
import type { LearningPathResponse, ProfileExtractResponse } from '../types/api'

export interface ProfileExtractPayload {
  learnerId: string
  message: string
}

export interface LearningPathPayload {
  learnerId: string
  goalId: string
}

export function extractProfile(payload: ProfileExtractPayload): Promise<ProfileExtractResponse> {
  return apiRequest<ProfileExtractResponse>('/api/profile/dialogue/extract', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function createLearningPath(payload: LearningPathPayload): Promise<LearningPathResponse> {
  return apiRequest<LearningPathResponse>('/api/learning-paths', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}
