import type { AgentTraceStepResponse, SourceCitationResponse } from './api'

export type ThoughtStatus = 'waiting' | 'running' | 'done' | 'warning' | 'failed'

export interface CurrentThoughtTask {
  title: string
  taskType: string
  model: string
  traceId: string
  startedAt: string
  status: ThoughtStatus
}

export interface ThoughtAgentStep {
  name: string
  status: ThoughtStatus
  duration: string
  summary: string
}

export interface ThoughtRagDocument {
  documentId?: string
  name: string
  pageNum?: number | null
  sectionTitle?: string | null
  score?: number
  excerpt?: string
}

export interface ThoughtRagSources {
  knowledgeBase: string
  chunkCount: number
  documents: ThoughtRagDocument[]
}

export interface ThoughtRuntimeMetrics {
  latency: string
  totalTokens: string
  modelCalls: string
  fallback: string
  safety: string
}

export interface ThoughtPanelData {
  currentTask: CurrentThoughtTask
  agentSteps: ThoughtAgentStep[]
  ragSources: ThoughtRagSources
  metrics: ThoughtRuntimeMetrics
}

export function createDefaultThoughtPanelData(): ThoughtPanelData {
  return {
    currentTask: {
      title: '暂无活动任务',
      taskType: '空闲',
      model: '',
      traceId: '',
      startedAt: '',
      status: 'waiting',
    },
    agentSteps: [],
    ragSources: {
      knowledgeBase: '',
      chunkCount: 0,
      documents: [],
    },
    metrics: {
      latency: '-',
      totalTokens: '0',
      modelCalls: '0',
      fallback: '-',
      safety: '-',
    },
  }
}

export function normalizeThoughtStatus(status: string | undefined | null): ThoughtStatus {
  const normalized = (status ?? '').trim().toLowerCase()
  if (['done', 'completed', 'complete', 'success', 'succeeded', 'approved'].includes(normalized)) {
    return 'done'
  }
  if (['running', 'active', 'executing', 'streaming', 'pending_critic'].includes(normalized)) {
    return 'running'
  }
  if (['warning', 'blocked', 'needs_review', 'revision_requested'].includes(normalized)) {
    return 'warning'
  }
  if (['failed', 'error', 'cancelled', 'rejected'].includes(normalized)) {
    return 'failed'
  }
  return 'waiting'
}

export function adaptAgentTraceSteps(steps: AgentTraceStepResponse[] | undefined): ThoughtAgentStep[] | undefined {
  if (!steps || steps.length === 0) {
    return undefined
  }
  return steps.map((step) => ({
    name: step.agentName,
    status: normalizeThoughtStatus(step.status),
    duration: `${step.latencyMs} ms`,
    summary: step.summary,
  }))
}

export function adaptRagSources(
  sources: SourceCitationResponse[] | undefined,
  knowledgeBase = '当前课程知识库',
): ThoughtRagSources | undefined {
  if (!sources || sources.length === 0) {
    return undefined
  }
  return {
    knowledgeBase,
    chunkCount: sources.length,
    documents: sources.map((source) => ({
      documentId: source.documentId,
      name: source.documentName,
      pageNum: source.pageNum,
      sectionTitle: source.sectionTitle,
      score: source.score,
      excerpt: source.excerpt,
    })),
  }
}
