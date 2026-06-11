import { apiRequest } from './client'
import type { AgentTraceResponse, ResourceGenerationResponse } from '../types/api'

export interface ResourceGenerationPayload {
  learnerId: string
  goalId: string
  pathNodeId: string
  resourceTypes: string[]
}

export function createResourceGeneration(
  payload: ResourceGenerationPayload,
): Promise<ResourceGenerationResponse> {
  return apiRequest<ResourceGenerationResponse>('/api/resources/generation-tasks', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function fetchResourceGenerationTask(taskId: string): Promise<ResourceGenerationResponse> {
  return apiRequest<ResourceGenerationResponse>(`/api/resources/generation-tasks/${taskId}`, {
    method: 'GET',
  })
}

export function fetchAgentTrace(agentTaskId: string): Promise<AgentTraceResponse> {
  return apiRequest<AgentTraceResponse>(`/api/agent/tasks/${agentTaskId}/trace`)
}
