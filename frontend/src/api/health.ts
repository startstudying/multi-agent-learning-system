import { apiRequest } from './client'
import type { HealthResponse } from '../types/api'

export function fetchHealth(): Promise<HealthResponse> {
  return apiRequest<HealthResponse>('/api/health', {
    method: 'GET',
  })
}
