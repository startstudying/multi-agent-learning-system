export interface ApiResponse<T> {
  code: string
  message: string
  data: T
}

export interface BackendProfileDimension {
  name: string
  value: string
  confidence: number
  evidence: string
}

export interface ProfileDraftResponse {
  learnerId: string
  target: string
  weakPoints: string[]
  preferences: string[]
  dimensions: BackendProfileDimension[]
  updatePolicy: string
}

export interface ProfileExtractResponse {
  profileDraft: ProfileDraftResponse
  followUpQuestions: string[]
  reasonSummary: string
  traceId: string
}

export interface LearningPathNodeResponse {
  nodeId: string
  title: string
  status: string
  mastery: number
  reasonSummary: string
}

export interface LearningPathResponse {
  pathId: string
  learnerId: string
  goalId: string
  reasonSummary: string
  nodes: LearningPathNodeResponse[]
  traceId: string
}

export interface SourceCitationResponse {
  documentId: string
  documentName: string
  pageNum: number | null
  sectionTitle: string | null
  excerpt: string
  score: number
}

export interface KnowledgeBaseResponse {
  id: string
  name: string
  description: string | null
  visibility: string
  ownerUserId: string
  createdAt: string
  updatedAt: string
}

export interface CreateKnowledgeBasePayload {
  name: string
  description?: string
  visibility?: string
}

export interface DocumentUploadResponse {
  documentId: string
  indexTaskId: string
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED'
}

export interface DocumentStatusResponse {
  documentId: string
  kbId: string
  name: string
  parseStatus: 'PENDING' | 'PARSING' | 'READY' | 'FAILED' | 'INDEXED'
  indexStatus: 'PENDING' | 'PARSING' | 'READY' | 'FAILED' | 'INDEXED'
  errorMessage: string | null
  version: number
}

export interface RagQueryResponse {
  answer: string
  sources: SourceCitationResponse[]
  traceId: string
}

export interface ResourceGenerationResponse {
  taskId: string
  agentTaskId: string
  status: string
  reviewStatus: string
  progressPercent: number
  safetyStatus: string
  resources: GeneratedResourceResponse[]
  traceId: string
}

export interface GeneratedResourceResponse {
  resourceId: string
  type: string
  modality: string
  title: string
  reviewStatus: string
  citationSummary: string
  markdownContent: string
  safetyStatus: string
}

export type GeneratedResourceStatus =
  | 'PENDING_CRITIC'
  | 'APPROVED'
  | 'REVISION_REQUESTED'
  | 'OTHER_REVIEW_STATUS'

export interface AgentTraceResponse {
  taskId: string
  status: string
  steps: AgentTraceStepResponse[]
  traceId: string
}

export interface AgentTraceStepResponse {
  stepId: string
  agentName: string
  status: string
  summary: string
  latencyMs: number
  model: string
  promptVersion: string
}

export interface ComponentHealthResponse {
  status: string
  detail: string
  metadata: Record<string, string | number | boolean | null>
}

export interface HealthResponse {
  application: ComponentHealthResponse
  database: ComponentHealthResponse
  redis: ComponentHealthResponse
  minio: ComponentHealthResponse
  model: ComponentHealthResponse
  vector?: ComponentHealthResponse
}

export interface OpsAlertItem {
  type: string
  severity: string
  triggered: boolean
  count: number
  threshold: string
  summary: string
  alertId?: string | null
  alertStatus?: string | null
}

export interface OpsAlertSummary {
  windowStart: string
  windowEnd: string
  alerts: OpsAlertItem[]
}

export interface OpsAlertRecord {
  alertId: string
  alertType: string
  severity: string
  summary: string
  status: string
  acknowledgedBy: string | null
  acknowledgedAt: string | null
  notificationStatus: string | null
  updatedAt: string
}

export interface ResourceReviewSummary {
  reviewId: string
  resourceId: string
  generationTaskId: string
  status: string
  summary: string
  resourceTitle: string | null
  resourceType: string | null
  resourceReviewStatus: string | null
}

export interface ReviewDecisionPayload {
  decision: 'APPROVED' | 'REVISION_REQUESTED'
  summary: string
}

export interface TokenUsageTotals {
  promptTokens: number
  completionTokens: number
  totalTokens: number
}

export interface AnalyticsOverview {
  agentTaskCount: number
  modelCallCount: number
  tokenUsage: TokenUsageTotals
  answerRecordCount: number
  wrongQuestionCount: number
  learningEventCount: number
  resourceReviewStatusCounts: Record<string, number>
}

export interface MasteryUpdateResponse {
  knowledgePointId: string
  beforeMastery: number
  afterMastery: number
  reasonSummary: string
}

export interface AnswerSubmitResponse {
  answerId: string
  gradingResultId: string
  score: number
  masteryUpdates: MasteryUpdateResponse[]
  feedbackId: string
  replanTriggered: boolean
  replanRecordId: string
  wrongCauseAnalysis: string
  resourcePushStrategy: string
  traceId: string
}

export interface DocumentRecord {
  id: string
  name: string
  type: string
  status: 'INDEXED' | 'PENDING' | 'READY' | 'FAILED'
  indexTaskId: string
  chunks: number
  updatedAt: string
}

export interface CitationSource {
  documentName: string
  pageNum: number | null
  sectionTitle: string | null
  excerpt: string
  score: number
}

export interface ProfileDimension {
  name: string
  value: string
  confidence: number
  evidence: string
}

export interface LearnerProfile {
  learnerId: string
  major: string
  goal: string
  preference: string
  weakness: string
  dimensions: ProfileDimension[]
}

export interface PathNode {
  id: string
  title: string
  status: 'READY' | 'ACTIVE' | 'LOCKED' | 'DONE'
  reason: string
  mastery: number
}

export interface GeneratedResource {
  resourceId: string
  type: string
  modality: string
  title: string
  status: GeneratedResourceStatus
  reviewStatus: string
  reviewer: string
  citationSummary: string
  markdownContent: string
  safetyStatus: string
}

export interface TraceStep {
  actor: string
  status: 'DONE' | 'RUNNING' | 'PENDING' | 'WAITING' | 'ERROR'
  detail: string
  latencyMs: number
}

export interface WorkbenchState {
  knowledgeBase: {
    id: string
    name: string
    visibility: string
    owner: string
  }
  learnerProfile: LearnerProfile
  documents: DocumentRecord[]
  ragQuestion: string
  ragTraceId: string
  sseStage: string
  ragAnswer: string
  ragSources: CitationSource[]
  mastery: number
  assessmentStatus: string
  assessmentAnswer: string
  replanRecordId: string
  profilePrompt: string
  followUpQuestions: string[]
  selectedFollowUpQuestion: string
  goalId: string
  resourceTaskId: string
  resourceTaskStatus: string
  resourceReviewStatus: string
  resourceProgressPercent: number
  resourceSafetyStatus: string
  agentTaskId: string
  resourceTraceId: string
  profileTraceId: string
  pathTraceId: string
  pathNodes: PathNode[]
  resources: GeneratedResource[]
  traceSteps: TraceStep[]
  loadingAction: string
  errorMessage: string
}
