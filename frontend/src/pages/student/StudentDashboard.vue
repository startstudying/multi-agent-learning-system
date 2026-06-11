<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  AlertTriangle,
  Bot,
  CheckCircle2,
  ClipboardCheck,
  Database,
  FileText,
  FileUp,
  GitBranch,
  LibraryBig,
  ListChecks,
  MessageSquareText,
  Route,
  Search,
  UploadCloud,
  UserRound,
} from 'lucide-vue-next'
import { submitAnswer } from '../../api/assessment'
import {
  createKnowledgeBase,
  fetchDocumentStatus,
  listKnowledgeBases,
  listKnowledgeBaseDocuments,
  uploadKnowledgeBaseDocument,
} from '../../api/documents'
import { createLearningPath, extractProfile } from '../../api/learning'
import { queryRag, streamChat, streamRagQuery } from '../../api/rag'
import { createResourceGeneration, fetchAgentTrace, fetchResourceGenerationTask } from '../../api/resources'
import type {
  AgentTraceResponse,
  AnswerSubmitResponse,
  DocumentRecord,
  DocumentStatusResponse,
  DocumentUploadResponse,
  GeneratedResource,
  GeneratedResourceStatus,
  GeneratedResourceResponse,
  LearningPathNodeResponse,
  PathNode,
  ProfileExtractResponse,
  RagQueryResponse,
  ResourceGenerationResponse,
  TraceStep,
  WorkbenchState,
} from '../../types/api'

const LEARNER_ID = 'stu_001'
const GOAL_ID = 'goal_java_backend'
const KNOWLEDGE_BASE_NAME = 'Java backend course materials'
const KNOWLEDGE_BASE_DESCRIPTION = 'Student workbench course materials'
const JOIN_NODE_ID = 'kp_sql_join'
const JOIN_QUESTION_ID = 'q_sql_join_cardinality'
const RESOURCE_TYPES = ['LECTURE', 'MIND_MAP', 'EXERCISE', 'READING', 'CODE_LAB']
const PROFILE_PROMPT =
  'I want to master Java backend project delivery. SQL JOIN diagnosis and cited RAG service are my weak points; I prefer code examples and project practice.'

const state = ref<WorkbenchState>({
  knowledgeBase: {
    id: 'kb_java_backend',
    name: KNOWLEDGE_BASE_NAME,
    visibility: 'PRIVATE',
    owner: 'stu_001',
  },
  learnerProfile: {
    learnerId: LEARNER_ID,
    major: 'Software Engineering',
    goal: 'Master Java backend project delivery',
    preference: 'Code examples and project practice',
    weakness: 'SQL JOIN diagnosis',
    dimensions: [
      { name: 'professional_background', value: 'Software engineering sophomore', confidence: 0.82, evidence: 'Learner stated major and year.' },
      { name: 'learning_goal', value: 'Java backend delivery', confidence: 0.88, evidence: 'Goal names Spring Boot APIs.' },
      { name: 'knowledge_base', value: 'Controller basics known', confidence: 0.74, evidence: 'Prior API practice.' },
      { name: 'knowledge_gap', value: 'SQL JOIN cardinality', confidence: 0.9, evidence: 'Repeated wrong answers.' },
      { name: 'cognitive_style', value: 'Worked examples before abstraction', confidence: 0.7, evidence: 'Prefers code labs.' },
      { name: 'resource_preference', value: 'Code labs and diagrams', confidence: 0.78, evidence: 'Explicit resource request.' },
    ],
  },
  documents: [
    {
      id: 'doc_001',
      name: 'database-course.md',
      type: 'Markdown',
      status: 'INDEXED',
      indexTaskId: 'idx_done_001',
      chunks: 84,
      updatedAt: '2026-06-04 20:10',
    },
    {
      id: 'doc_002',
      name: 'spring-boot-api-notes.pdf',
      type: 'PDF',
      status: 'READY',
      indexTaskId: 'idx_done_002',
      chunks: 126,
      updatedAt: '2026-06-04 20:18',
    },
  ],
  ragQuestion: 'Why does SQL JOIN duplicate rows?',
  ragTraceId: 'trc_idle',
  sseStage: 'IDLE',
  ragAnswer: 'Ask against kb_java_backend to retrieve grounded course guidance.',
  ragSources: [
    {
      documentName: 'database-course.md',
      pageNum: 12,
      sectionTitle: 'Multi table joins',
      excerpt: 'JOIN duplicates usually come from one-to-many relationships.',
      score: 0.87,
    },
  ],
  mastery: 42,
  assessmentStatus: 'Waiting for answer',
  assessmentAnswer:
    'A JOIN duplicates parent rows when multiple child rows match; aggregate or constrain child rows before projecting.',
  replanRecordId: 'Not created',
  profilePrompt: PROFILE_PROMPT,
  followUpQuestions: [],
  selectedFollowUpQuestion: '',
  goalId: GOAL_ID,
  resourceTaskId: 'res_task_draft',
  resourceTaskStatus: 'DRAFT',
  resourceReviewStatus: 'PENDING_CRITIC',
  resourceProgressPercent: 0,
  resourceSafetyStatus: 'PENDING',
  agentTaskId: 'agent_task_draft',
  resourceTraceId: 'trc_resource_local',
  profileTraceId: 'trc_profile_local',
  pathTraceId: 'trc_path_local',
  pathNodes: [
    {
      id: 'kp_http_controller',
      title: 'HTTP Controllers',
      status: 'READY',
      reason: 'Controller basics are stable enough for API workflow practice.',
      mastery: 76,
    },
    {
      id: JOIN_NODE_ID,
      title: 'SQL JOIN Diagnosis',
      status: 'ACTIVE',
      reason: 'Weak point detected from profile extraction and prior answers.',
      mastery: 42,
    },
    {
      id: 'kp_cited_rag_service',
      title: 'Cited RAG Service',
      status: 'LOCKED',
      reason: 'Unlock after retrieval, citation grounding, and JOIN correction.',
      mastery: 18,
    },
  ],
  resources: [
    {
      resourceId: 'res_local_001',
      type: 'LECTURE',
      modality: 'TEXT',
      title: 'JOIN duplication walkthrough',
      status: 'PENDING_CRITIC',
      reviewStatus: 'PENDING_CRITIC',
      reviewer: 'CriticAgent',
      citationSummary: 'database-course.md p.12',
      markdownContent: 'Local draft resource body',
      safetyStatus: 'PENDING',
    },
    {
      resourceId: 'res_local_002',
      type: 'EXERCISE',
      modality: 'TEXT',
      title: 'Fix a one-to-many query',
      status: 'PENDING_CRITIC',
      reviewStatus: 'PENDING_CRITIC',
      reviewer: 'CriticAgent',
      citationSummary: 'database-course.md p.14',
      markdownContent: 'Local exercise draft body',
      safetyStatus: 'PENDING',
    },
  ],
  traceSteps: [
    {
      actor: 'ProfileAgent',
      status: 'DONE',
      detail: 'Extracted major, goal, weak point, and resource preference.',
      latencyMs: 180,
    },
    {
      actor: 'PathPlannerAgent',
      status: 'DONE',
      detail: 'Selected active node from learner profile and dependency graph.',
      latencyMs: 240,
    },
    {
      actor: 'CourseRagAgent',
      status: 'PENDING',
      detail: 'Waiting for a query to retrieve, rerank, and cite sources.',
      latencyMs: 0,
    },
  ],
  loadingAction: '',
  errorMessage: '',
})

const documents = computed(() => state.value.documents)
const resources = computed(() => state.value.resources)
const approvedResources = computed(() => resources.value.filter((resource) => resource.status === 'APPROVED'))
const pendingReviewResources = computed(() =>
  resources.value.filter((resource) => resource.status === 'PENDING_CRITIC'),
)
const revisionResources = computed(() =>
  resources.value.filter((resource) => resource.status === 'REVISION_REQUESTED'),
)
const otherReviewResources = computed(() =>
  resources.value.filter((resource) => resource.status === 'OTHER_REVIEW_STATUS'),
)
const pathNodes = computed(() => state.value.pathNodes)
const traceSteps = computed(() => state.value.traceSteps)
const isLoading = computed(() => state.value.loadingAction !== '')
const selectedDocumentFile = ref<File | null>(null)
const selectedResourceTypes = ref<string[]>([...RESOURCE_TYPES])

const workflowSteps = computed(() => [
  { id: 'profile', label: '画像', complete: Boolean(state.value.profileTraceId) },
  { id: 'knowledge-bases', label: '知识库', complete: Boolean(state.value.knowledgeBase.id) },
  {
    id: 'documents',
    label: '课程资料 Documents',
    complete: documents.value.length > 0 && documents.value.every((document) => document.status !== 'PENDING'),
  },
  { id: 'rag-chat', label: 'RAG 问答', complete: state.value.sseStage === 'DONE' },
  { id: 'citations', label: '引用来源', complete: state.value.ragSources.length > 0 && state.value.ragTraceId !== 'trc_idle' },
  { id: 'learning-path', label: '学习路径', complete: pathNodes.value.length > 0 && Boolean(state.value.pathTraceId) },
  { id: 'resources', label: '生成资源', complete: state.value.resourceTaskId !== 'res_task_draft' && resources.value.length > 0 },
  { id: 'assessment', label: '测评反馈', complete: state.value.replanRecordId !== 'Not created' },
])

const indexedDocuments = computed(
  () => documents.value.filter((document) => document.status === 'INDEXED').length,
)

const pendingDocuments = computed(
  () => documents.value.filter((document) => document.status === 'PENDING').length,
)

const averageMastery = computed(() => {
  if (pathNodes.value.length === 0) {
    return state.value.mastery
  }

  const nodeAverage =
    pathNodes.value.reduce((sum, node) => sum + node.mastery, 0) / pathNodes.value.length
  return Math.round((nodeAverage + state.value.mastery) / 2)
})

const usesSensitiveUrlSafeRagTransport = computed(
  () => import.meta.env.PROD || import.meta.env.MODE === 'staging',
)

onMounted(() => {
  void bootstrapWorkbench()
})

async function bootstrapWorkbench() {
  await initializeKnowledgeBase()
  await initializeLearningContext()
}

async function initializeKnowledgeBase() {
  try {
    const knowledgeBases = await listKnowledgeBases()
    const selected =
      knowledgeBases.find((knowledgeBase) => knowledgeBase.name === KNOWLEDGE_BASE_NAME) ??
      knowledgeBases[0]
    if (selected) {
      applyKnowledgeBase(selected)
      await initializeKnowledgeBaseDocuments(selected.id)
      return
    }

    const created = await createKnowledgeBase({
      name: KNOWLEDGE_BASE_NAME,
      description: KNOWLEDGE_BASE_DESCRIPTION,
      visibility: 'PRIVATE',
    })
    applyKnowledgeBase(created)
    await initializeKnowledgeBaseDocuments(created.id)
  } catch (error) {
    captureError('KnowledgeBaseController', error)
  }
}

async function initializeKnowledgeBaseDocuments(kbId: string) {
  try {
    const backendDocuments = await listKnowledgeBaseDocuments(kbId)
    applyDocumentListResponse(backendDocuments)
  } catch (error) {
    captureError('DocumentController', error)
  }
}

async function initializeLearningContext() {
  try {
    const profile = await extractProfile({
      learnerId: LEARNER_ID,
      message: state.value.profilePrompt,
    })
    applyProfileResponse(profile)
  } catch (error) {
    captureError('ProfileAgent', error)
  }

  try {
    const path = await createLearningPath({
      learnerId: LEARNER_ID,
      goalId: state.value.goalId,
    })
    applyLearningPathResponse(path)
  } catch (error) {
    captureError('PathPlannerAgent', error)
  }
}

function selectFollowUpQuestion(question: string) {
  state.value.profilePrompt = question
  state.value.selectedFollowUpQuestion = question
}

async function refineProfile() {
  const message = state.value.profilePrompt.trim()
  if (!message) {
    state.value.errorMessage = 'Enter learner context before updating profile.'
    return
  }

  startAction('profile')
  try {
    const profile = await extractProfile({
      learnerId: state.value.learnerProfile.learnerId,
      message,
    })
    applyProfileResponse(profile)
  } catch (error) {
    captureError('ProfileAgent', error)
  } finally {
    finishAction('profile')
  }
}

async function uploadDocument() {
  if (state.value.loadingAction === 'document') return

  startAction('document')
  try {
    const file = selectedDocumentFile.value
    if (!file) {
      state.value.errorMessage = 'Select a document before upload.'
      return
    }
    const activeNode = activePathNode()
    if (!activeNode) {
      state.value.errorMessage = 'Learning path is empty; create a path before uploading documents.'
      return
    }

    const response = await uploadKnowledgeBaseDocument(state.value.knowledgeBase.id, file, {
      courseId: state.value.goalId,
      chapterId: activeNode.id,
    })
    applyDocumentUploadResponse(response, file.name)
    const status = await fetchDocumentStatus(response.documentId)
    applyDocumentStatusResponse(status, response.indexTaskId)
  } catch (error) {
    captureError('DocumentController', error)
  } finally {
    finishAction('document')
  }
}

function selectDocumentFile(event: Event) {
  const input = event.target as HTMLInputElement
  selectedDocumentFile.value = input.files?.[0] ?? null
}

async function askRag() {
  if (state.value.loadingAction === 'rag') return

  startAction('rag')
  state.value.sseStage = 'RETRIEVING'
  state.value.ragAnswer = ''
  const useRestOnlyTransport = usesSensitiveUrlSafeRagTransport.value
  try {
    if (useRestOnlyTransport) {
      await streamRagQueryResponse()
    } else {
      await streamRagResponse()
    }
  } catch (error) {
    if (useRestOnlyTransport) {
      state.value.sseStage = 'ERROR'
      captureError('CourseRagAgent', error)
    } else {
      await fallbackToRagRest(error)
    }
  } finally {
    finishAction('rag')
  }
}

function streamRagResponse(): Promise<void> {
  return new Promise((resolve, reject) => {
    const source = streamChat(`session_${LEARNER_ID}`, state.value.ragQuestion, [
      state.value.knowledgeBase.id,
    ])
    const close = () => source.close()
    const failInvalidPayload = () => {
      close()
      reject(new Error('Invalid SSE event payload'))
    }

    source.addEventListener('status', (event) => {
      const data = parseSsePayload<{ stage?: string }>(event, failInvalidPayload)
      if (!data) return

      if (data.stage) {
        state.value.sseStage = data.stage
      }
    })
    source.addEventListener('token', (event) => {
      const data = parseSsePayload<{ text?: string }>(event, failInvalidPayload)
      if (!data) return

      state.value.ragAnswer += data.text ?? ''
    })
    source.addEventListener('done', (event) => {
      const data = parseSsePayload<{
        traceId: string
        sources: RagQueryResponse['sources']
      }>(event, failInvalidPayload)
      if (!data) return

      applyRagResponse({
        answer: state.value.ragAnswer,
        traceId: data.traceId,
        sources: data.sources ?? [],
      })
      close()
      resolve()
    })
    source.onerror = (event) => {
      close()
      reject(event)
    }
  })
}

async function streamRagQueryResponse() {
  await streamRagQuery(
    {
      kbIds: [state.value.knowledgeBase.id],
      question: state.value.ragQuestion,
      topK: 5,
    },
    {
      onStatus: ({ stage }) => {
        if (stage) {
          state.value.sseStage = stage
        }
      },
      onToken: ({ text }) => {
        state.value.ragAnswer += text ?? ''
      },
      onDone: ({ answer, traceId, sources }) => {
        applyRagResponse({
          answer: answer ?? state.value.ragAnswer,
          traceId: traceId ?? 'trc_stream_missing',
          sources: sources ?? [],
        })
      },
      onError: ({ message }) => {
        throw new Error(message || 'RAG stream failed')
      },
    },
  )
}

function parseSsePayload<T>(event: MessageEvent, onInvalid: () => void): T | null {
  try {
    return JSON.parse(event.data) as T
  } catch {
    onInvalid()
    return null
  }
}

async function fallbackToRagRest(streamError: unknown) {
  try {
    await queryRagRest()
  } catch (restError) {
    state.value.sseStage = 'ERROR'
    captureError('CourseRagAgent', streamError instanceof Error ? streamError : restError)
  }
}

async function queryRagRest() {
  const response = await queryRag({
    kbIds: [state.value.knowledgeBase.id],
    question: state.value.ragQuestion,
    topK: 5,
  })
  applyRagResponse(response)
}

async function generateResources() {
  startAction('resources')
  const activeNode = activePathNode()
  if (!activeNode) {
    state.value.errorMessage = 'Learning path is empty; create a path before generating resources.'
    finishAction('resources')
    return
  }

  try {
    const response = await createResourceGeneration({
      learnerId: state.value.learnerProfile.learnerId,
      goalId: state.value.goalId,
      pathNodeId: activeNode.id,
      resourceTypes: selectedResourceTypes.value,
    })
    applyResourceGenerationResponse(response)

    const trace = await fetchAgentTrace(response.agentTaskId)
    applyAgentTrace(trace)
  } catch (error) {
    captureError('ResourceAgent', error)
  } finally {
    finishAction('resources')
  }
}

async function refreshResourceStatus() {
  if (state.value.resourceTaskId === 'res_task_draft') return

  startAction('resource-status')
  try {
    const response = await fetchResourceGenerationTask(state.value.resourceTaskId)
    applyResourceGenerationResponse(response)
    appendTrace({
      actor: 'ReviewGovernance',
      status: 'DONE',
      detail: `Refreshed generation task ${response.taskId}; review status ${response.reviewStatus}.`,
      latencyMs: 88,
    })
  } catch (error) {
    captureError('ReviewGovernance', error)
  } finally {
    finishAction('resource-status')
  }
}

async function submitAssessment() {
  startAction('assessment')
  try {
    const response = await submitAnswer({
      learnerId: state.value.learnerProfile.learnerId,
      questionId: JOIN_QUESTION_ID,
      answer: state.value.assessmentAnswer,
    })
    applyAssessmentResponse(response)
  } catch (error) {
    captureError('AssessmentAgent', error)
  } finally {
    finishAction('assessment')
  }
}

function appendTrace(step: TraceStep) {
  state.value.traceSteps = [...state.value.traceSteps, step]
}

function applyDocumentUploadResponse(response: DocumentUploadResponse, fileName: string) {
  const document: DocumentRecord = {
    id: response.documentId,
    name: fileName,
    type: 'Markdown',
    status: normalizeDocumentStatus(response.status),
    indexTaskId: response.indexTaskId,
    chunks: 0,
    updatedAt: new Date().toISOString().slice(0, 16).replace('T', ' '),
  }
  state.value.documents = [
    ...state.value.documents.filter((existing) => existing.id !== response.documentId),
    document,
  ]
  appendTrace({
    actor: 'DocumentController',
    status: 'RUNNING',
    detail: `Uploaded ${response.documentId} and created index task ${response.indexTaskId}.`,
    latencyMs: 96,
  })
}

function applyDocumentStatusResponse(response: DocumentStatusResponse, indexTaskId: string) {
  const document: DocumentRecord = {
    id: response.documentId,
    name: response.name,
    type: inferDocumentType(response.name),
    status: normalizeBackendDocumentStatus(response.indexStatus),
    indexTaskId,
    chunks: 0,
    updatedAt: new Date().toISOString().slice(0, 16).replace('T', ' '),
  }
  state.value.documents = [
    ...state.value.documents.filter((existing) => existing.id !== response.documentId),
    document,
  ]
  appendTrace({
    actor: 'DocumentController',
    status: document.status === 'INDEXED' ? 'DONE' : 'RUNNING',
    detail: `Document ${response.documentId} parse ${response.parseStatus}, index ${response.indexStatus}.`,
    latencyMs: 64,
  })
}

function applyDocumentListResponse(response: DocumentStatusResponse[]) {
  state.value.documents = response.map((document) => toDocumentRecord(document))
}

function toDocumentRecord(response: DocumentStatusResponse): DocumentRecord {
  return {
    id: response.documentId,
    name: response.name,
    type: inferDocumentType(response.name),
    status: normalizeBackendDocumentStatus(response.indexStatus),
    indexTaskId: `v${response.version}`,
    chunks: 0,
    updatedAt: 'Backend document',
  }
}

function applyKnowledgeBase(response: {
  id: string
  name: string
  visibility: string
  ownerUserId: string
}) {
  state.value.knowledgeBase = {
    id: response.id,
    name: response.name,
    visibility: response.visibility,
    owner: response.ownerUserId,
  }
}

function applyProfileResponse(response: ProfileExtractResponse) {
  const draft = response.profileDraft
  state.value.profileTraceId = response.traceId
  state.value.followUpQuestions = response.followUpQuestions
  if (!response.followUpQuestions.includes(state.value.selectedFollowUpQuestion)) {
    state.value.selectedFollowUpQuestion = ''
  }
  state.value.learnerProfile = {
    learnerId: draft.learnerId,
    major: state.value.learnerProfile.major,
    goal: draft.target,
    preference: draft.preferences.join(', '),
    weakness: draft.weakPoints.join(', '),
    dimensions: draft.dimensions.map((dimension) => ({
      name: dimension.name,
      value: dimension.value,
      confidence: dimension.confidence,
      evidence: dimension.evidence,
    })),
  }
  upsertTrace({
    actor: 'ProfileAgent',
    status: 'DONE',
    detail: `${response.reasonSummary} Trace ${response.traceId}.`,
    latencyMs: 180,
  })
}

function applyResourceGenerationResponse(response: ResourceGenerationResponse) {
  state.value.resourceTaskId = response.taskId
  state.value.resourceTaskStatus = response.status
  state.value.resourceReviewStatus = response.reviewStatus
  state.value.resourceProgressPercent = response.progressPercent
  state.value.resourceSafetyStatus = response.safetyStatus
  state.value.agentTaskId = response.agentTaskId
  state.value.resourceTraceId = response.traceId
  state.value.resources = response.resources.map(toGeneratedResource)
}

function applyLearningPathResponse(response: {
  goalId: string
  nodes: LearningPathNodeResponse[]
  reasonSummary: string
  traceId: string
}) {
  state.value.goalId = response.goalId
  state.value.pathTraceId = response.traceId
  state.value.pathNodes = response.nodes.map(toPathNode)
  upsertTrace({
    actor: 'PathPlannerAgent',
    status: 'DONE',
    detail: `${response.reasonSummary} Trace ${response.traceId}.`,
    latencyMs: 240,
  })
}

function applyRagResponse(response: RagQueryResponse) {
  state.value.sseStage = 'DONE'
  state.value.ragTraceId = response.traceId
  state.value.ragAnswer = response.answer
  state.value.ragSources = response.sources.map((source) => ({
    documentName: source.documentName,
    pageNum: source.pageNum,
    sectionTitle: source.sectionTitle,
    excerpt: source.excerpt,
    score: source.score,
  }))
  state.value.traceSteps = state.value.traceSteps.map((step) =>
    step.actor === 'CourseRagAgent'
      ? {
          ...step,
          status: 'DONE',
          detail: 'Backend RAG query completed with permission-filtered citations.',
          latencyMs: 1200,
        }
      : step,
  )
}

function upsertTrace(step: TraceStep) {
  const withoutActor = state.value.traceSteps.filter((existing) => existing.actor !== step.actor)
  state.value.traceSteps = [...withoutActor, step]
}

function applyAgentTrace(response: AgentTraceResponse) {
  const traceStepsFromBackend = response.steps.map((step) => ({
    actor: step.agentName,
    status: normalizeTraceStatus(step.status),
    detail: step.summary,
    latencyMs: step.latencyMs,
  }))
  state.value.traceSteps = [
    ...state.value.traceSteps.filter(
      (step) => !traceStepsFromBackend.some((backendStep) => backendStep.actor === step.actor),
    ),
    ...traceStepsFromBackend,
  ]
}

function applyAssessmentResponse(response: AnswerSubmitResponse) {
  const update = response.masteryUpdates[0]
  const nextMastery = update ? asPercent(update.afterMastery) : asPercent(response.score)
  state.value.mastery = nextMastery
  state.value.assessmentStatus = response.wrongCauseAnalysis
  state.value.replanRecordId = response.replanRecordId
  if (update) {
    state.value.pathNodes = state.value.pathNodes.map((node) =>
      node.id === update.knowledgePointId
        ? {
            ...node,
            mastery: nextMastery,
            reason: update.reasonSummary,
          }
        : node,
    )
  }
  appendTrace({
    actor: 'AssessmentAgent',
    status: 'DONE',
    detail: `Graded answer ${response.answerId}; remediation strategy ${response.resourcePushStrategy}.`,
    latencyMs: 320,
  })
}

function toPathNode(node: LearningPathNodeResponse): PathNode {
  return {
    id: node.nodeId,
    title: node.title,
    status: normalizePathStatus(node.status),
    reason: node.reasonSummary,
    mastery: asPercent(node.mastery),
  }
}

function toGeneratedResource(resource: GeneratedResourceResponse): GeneratedResource {
  return {
    resourceId: resource.resourceId,
    type: resource.type,
    modality: resource.modality,
    title: resource.title,
    status: normalizeResourceStatus(resource.reviewStatus),
    reviewStatus: resource.reviewStatus,
    reviewer: 'CriticAgent',
    citationSummary: resource.citationSummary,
    markdownContent: resource.markdownContent,
    safetyStatus: resource.safetyStatus,
  }
}

function normalizeResourceStatus(status: string): GeneratedResourceStatus {
  if (status === 'PENDING_CRITIC' || status === 'APPROVED' || status === 'REVISION_REQUESTED') {
    return status
  }
  return 'OTHER_REVIEW_STATUS'
}

function normalizePathStatus(status: string): PathNode['status'] {
  if (status === 'READY' || status === 'ACTIVE' || status === 'LOCKED' || status === 'DONE') {
    return status
  }
  return 'LOCKED'
}

function normalizeDocumentStatus(status: DocumentUploadResponse['status']): DocumentRecord['status'] {
  if (status === 'COMPLETED') return 'INDEXED'
  return status === 'FAILED' ? 'FAILED' : 'PENDING'
}

function normalizeBackendDocumentStatus(
  status: DocumentStatusResponse['indexStatus'],
): DocumentRecord['status'] {
  if (status === 'INDEXED') return 'INDEXED'
  if (status === 'FAILED') return 'FAILED'
  return status === 'READY' ? 'READY' : 'PENDING'
}

function inferDocumentType(name: string): string {
  const extension = name.split('.').pop()?.toLowerCase()
  if (extension === 'pdf') return 'PDF'
  if (extension === 'md' || extension === 'markdown') return 'Markdown'
  return 'Document'
}

function activePathNode(): PathNode | null {
  return pathNodes.value.find((node) => node.status === 'ACTIVE') ?? pathNodes.value[0]
}

function startAction(action: string) {
  state.value.loadingAction = action
  state.value.errorMessage = ''
}

function finishAction(action: string) {
  if (state.value.loadingAction === action) {
    state.value.loadingAction = ''
  }
}

function captureError(actor: string, error: unknown) {
  const message = error instanceof Error ? error.message : 'Request failed'
  state.value.errorMessage = message
  appendTrace({
    actor,
    status: 'ERROR',
    detail: message,
    latencyMs: 0,
  })
}

function normalizeTraceStatus(status: string): TraceStep['status'] {
  if (status === 'DONE' || status === 'RUNNING' || status === 'PENDING' || status === 'WAITING') {
    return status
  }
  if (status === 'COMPLETED') return 'DONE'
  return 'ERROR'
}

function asPercent(value: number): number {
  return Math.round(value <= 1 ? value * 100 : value)
}
</script>

<template>
  <section class="workspace" aria-label="Learning workbench">
    <header class="workspace-header">
      <div>
        <p class="eyebrow">学生端 / Course RAG + Agent Generation + Assessment</p>
        <h2>学生端 Learning Loop 工作台 / Student Learning Loop</h2>
        <p class="header-note">围绕画像、RAG 引用、学习路径、资源审核和测评反馈完成一条可解释的学习闭环。</p>
      </div>
      <button
        class="primary-action ai-action"
        type="button"
        data-test="ask-rag"
        :disabled="state.loadingAction === 'rag'"
        @click="askRag"
      >
        <Search :size="18" aria-hidden="true" />
        <span class="desktop-label">{{ state.loadingAction === 'rag' ? 'RAG 检索中 / Running RAG' : '发送问题 / Run RAG Chat' }}</span>
        <span class="mobile-label">{{ state.loadingAction === 'rag' ? 'RAG 检索中' : '发送问题' }}</span>
      </button>
    </header>

    <section class="workflow-strip compact-progress" aria-label="Workflow status">
      <div
        v-for="step in workflowSteps"
        :key="step.id"
        class="workflow-step"
        :data-test="`workflow-${step.id}`"
      >
        <CheckCircle2 v-if="step.complete" :size="16" aria-hidden="true" />
        <span v-else class="step-dot" aria-hidden="true"></span>
        <strong>{{ step.label }}</strong>
      </div>
    </section>

    <section class="summary-strip" aria-label="Learning cockpit summary">
      <article>
        <span>学习目标摘要</span>
        <strong>{{ state.knowledgeBase.visibility }}</strong>
        <p>{{ state.knowledgeBase.name }} / 掌握 RAG 的核心概念与实践应用</p>
      </article>
      <article>
        <span>今日进度</span>
        <strong>{{ state.sseStage }}</strong>
        <p>SSE stage: status / token / done events</p>
      </article>
      <article>
        <span>当前主题</span>
        <strong>{{ state.resourceTaskId }}</strong>
        <p>RAG 检索流程与向量检索基础</p>
      </article>
      <article>
        <span>重规划记录</span>
        <strong>{{ state.replanRecordId }}</strong>
        <p>assessment-triggered path update</p>
      </article>
      <article>
        <span>课程资料 Documents</span>
        <strong>{{ indexedDocuments }} / {{ pendingDocuments }}</strong>
        <p>indexed / pending async index tasks</p>
      </article>
      <article>
        <span>平均掌握度</span>
        <strong>{{ averageMastery }}%</strong>
        <p>path and assessment aggregate</p>
      </article>
    </section>

    <section class="student-cockpit">
      <div class="student-primary-workspace" data-test="student-primary-workspace">
        <article class="panel chat-panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">RAG 问答区 / RAG Chat</p>
              <h3>{{ state.ragQuestion }}</h3>
            </div>
            <MessageSquareText :size="20" aria-hidden="true" />
          </div>
          <label class="field-control">
            <span>问题输入</span>
            <input
              v-model="state.ragQuestion"
              type="text"
              data-test="rag-question-input"
              placeholder="请输入你的问题..."
            />
          </label>
          <p class="answer-text ai-answer">{{ state.ragAnswer }}</p>
          <div class="stage-line">
            <Bot :size="16" aria-hidden="true" />
            <span>流式阶段 SSE stage</span>
            <strong>{{ state.sseStage }}</strong>
          </div>
          <div class="trace-chip">
            traceId <strong>{{ state.ragTraceId }}</strong>
          </div>
          <div class="no-source-card" data-test="no-source-card">
            <AlertTriangle :size="18" aria-hidden="true" />
            <div>
              <strong>无可靠来源，系统暂不回答</strong>
              <p>未检索到足够可靠的课程来源时，系统拒绝编造答案。你可以换一种问法，或请教师补充课程资料。</p>
              <span>no source / traceId: trace_xyz789 / 最低相似度阈值 0.70</span>
            </div>
          </div>
        </article>

        <article class="panel citation-panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">引用来源列表 / Citations</p>
              <h3>可解释来源</h3>
            </div>
            <LibraryBig :size="20" aria-hidden="true" />
          </div>
          <ol class="citation-list">
            <li v-for="source in state.ragSources" :key="`${source.documentName}-${source.pageNum}`">
              <strong>{{ source.documentName }}</strong>
              <span>页码 p.{{ source.pageNum }} / {{ source.sectionTitle }} / score {{ source.score }}</span>
              <p>{{ source.excerpt }}</p>
              <small>documentId: doc_001 / chunkId: chunk_001</small>
            </li>
          </ol>
          <div class="api-source-list">
            <strong>接口数据来源</strong>
            <span>POST /api/rag/query</span>
            <span>GET /api/agent/tasks/{taskId}/trace</span>
          </div>
        </article>

        <article class="panel path-panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">学习路径节点 / Learning Path</p>
              <h3>可追踪下一步</h3>
            </div>
            <Route :size="20" aria-hidden="true" />
          </div>
          <div class="path-board">
            <section v-for="node in pathNodes" :key="node.title" class="path-node">
              <div>
                <strong>{{ node.title }}</strong>
                <em :class="['status-pill', node.status.toLowerCase()]">{{ node.status }}</em>
              </div>
              <p>{{ node.reason }}</p>
              <div class="mini-meter" :aria-label="`${node.title} mastery`">
                <span :style="{ width: `${node.mastery}%` }"></span>
              </div>
            </section>
          </div>
        </article>
      </div>

      <div class="student-support-workspace" data-test="student-support-workspace">
      <article class="panel profile-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">当前画像 / Learning Profile</p>
            <h3>{{ state.learnerProfile.learnerId }}</h3>
          </div>
          <UserRound :size="20" aria-hidden="true" />
        </div>
        <label class="field-control">
          <span>画像补充信息</span>
          <textarea
            v-model="state.profilePrompt"
            data-test="profile-prompt-input"
            rows="4"
            placeholder="描述学习目标、当前卡点和资源偏好"
          ></textarea>
        </label>
        <div v-if="state.followUpQuestions.length" class="follow-up-list" aria-label="Profile follow-up questions">
          <button
            v-for="(question, index) in state.followUpQuestions"
            :key="question"
            :class="['follow-up-chip', { selected: state.selectedFollowUpQuestion === question }]"
            type="button"
            :aria-pressed="state.selectedFollowUpQuestion === question"
            :data-test="`profile-follow-up-${index}`"
            @click="selectFollowUpQuestion(question)"
          >
            {{ question }}
          </button>
        </div>
        <button
          class="tool-button"
          type="button"
          data-test="refine-profile"
          :disabled="isLoading"
          @click="refineProfile"
        >
          <UserRound :size="17" aria-hidden="true" />
          {{ state.loadingAction === 'profile' ? '更新画像中' : '更新画像' }}
        </button>
        <dl class="detail-grid">
          <div>
            <dt>专业</dt>
            <dd>{{ state.learnerProfile.major }}</dd>
          </div>
          <div>
            <dt>学习目标</dt>
            <dd>{{ state.learnerProfile.goal }}</dd>
          </div>
          <div>
            <dt>薄弱点</dt>
            <dd>{{ state.learnerProfile.weakness }}</dd>
          </div>
          <div>
            <dt>偏好</dt>
            <dd>{{ state.learnerProfile.preference }}</dd>
          </div>
        </dl>
      </article>

      <article class="panel kb-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">知识库 / Knowledge Bases</p>
            <h3>{{ state.knowledgeBase.name }}</h3>
          </div>
          <Database :size="20" aria-hidden="true" />
        </div>
        <label class="file-picker">
          <FileUp :size="17" aria-hidden="true" />
          <span>{{ selectedDocumentFile?.name ?? '选择课程资料' }}</span>
          <input
            type="file"
            data-test="document-file-input"
            accept=".md,.markdown,.pdf,.txt"
            @change="selectDocumentFile"
          />
        </label>
        <button class="tool-button" type="button" data-test="upload-document" @click="uploadDocument">
          <UploadCloud :size="17" aria-hidden="true" />
          上传课程资料
        </button>
        <p v-if="state.errorMessage" class="error-text" role="status">{{ state.errorMessage }}</p>
        <ul class="document-list" aria-label="Documents">
          <li v-for="document in documents" :key="document.id">
            <FileUp :size="16" aria-hidden="true" />
            <div>
              <strong>{{ document.name }}</strong>
              <span>{{ document.type }} / {{ document.indexTaskId }} / {{ document.chunks }} chunks</span>
            </div>
            <em :class="['status-pill', document.status.toLowerCase()]">{{ document.status }}</em>
          </li>
        </ul>
      </article>

      <article class="panel resource-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">生成资源状态 / Generated Resources</p>
            <h3>审核闸口资源架</h3>
          </div>
          <FileText :size="20" aria-hidden="true" />
        </div>
        <fieldset class="resource-type-picker">
          <legend>资源类型</legend>
          <label v-for="type in RESOURCE_TYPES" :key="type" class="resource-type-option">
            <input
              v-model="selectedResourceTypes"
              type="checkbox"
              :value="type"
              :data-test="`resource-type-${type}`"
            />
            <span>{{ type }}</span>
          </label>
        </fieldset>
        <button
          class="tool-button"
          type="button"
          data-test="generate-resources"
          :disabled="isLoading"
          @click="generateResources"
        >
          <ListChecks :size="17" aria-hidden="true" />
          {{ state.loadingAction === 'resources' ? 'AI 生成资源中' : 'AI 生成资源' }}
        </button>
        <button
          class="tool-button secondary"
          type="button"
          data-test="refresh-resource-status"
          :disabled="isLoading || state.resourceTaskId === 'res_task_draft'"
          @click="refreshResourceStatus"
        >
          <Search :size="17" aria-hidden="true" />
          {{ state.loadingAction === 'resource-status' ? '检查审核状态中' : '查看状态' }}
        </button>
        <div class="trace-chip">
          Resource traceId <strong>{{ state.resourceTraceId }}</strong>
        </div>
        <dl class="resource-task-summary" data-test="resource-task-summary">
          <div>
            <dt>任务状态</dt>
            <dd>{{ state.resourceTaskStatus }}</dd>
          </div>
          <div>
            <dt>审核状态</dt>
            <dd>{{ state.resourceReviewStatus }}</dd>
          </div>
          <div>
            <dt>进度</dt>
            <dd>{{ state.resourceProgressPercent }}%</dd>
          </div>
          <div>
            <dt>安全</dt>
            <dd>{{ state.resourceSafetyStatus }}</dd>
          </div>
        </dl>

        <div class="status-board resource-status-board" aria-label="Resource review status board">
          <section class="resource-shelf approved" data-test="resource-shelf-approved">
            <div class="resource-shelf-heading">
              <strong>已批准 approved</strong>
              <span>{{ approvedResources.length }}</span>
            </div>
            <ul class="resource-list compact">
              <li v-for="resource in approvedResources" :key="resource.resourceId">
                <div class="resource-title-row">
                  <strong>{{ resource.type }}</strong>
                  <span>{{ resource.title }}</span>
                  <em :class="['status-pill', resource.status.toLowerCase()]">{{ resource.reviewStatus }}</em>
                </div>
                <small>{{ resource.resourceId }} / {{ resource.modality }} / {{ resource.safetyStatus }}</small>
                <p>{{ resource.citationSummary }}</p>
                <blockquote>{{ resource.markdownContent }}</blockquote>
              </li>
            </ul>
            <p v-if="approvedResources.length === 0" class="empty-shelf">暂无已批准资源。</p>
          </section>

          <section class="resource-shelf pending" data-test="resource-shelf-pending">
            <div class="resource-shelf-heading">
              <strong>待教师审核 pending review</strong>
              <span>{{ pendingReviewResources.length }}</span>
            </div>
            <ul class="resource-list compact">
              <li v-for="resource in pendingReviewResources" :key="resource.resourceId">
                <div class="resource-title-row">
                  <strong>{{ resource.type }}</strong>
                  <span>{{ resource.title }}</span>
                  <em :class="['status-pill', resource.status.toLowerCase()]">{{ resource.reviewStatus }}</em>
                </div>
                <small>{{ resource.resourceId }} / {{ resource.modality }} / {{ resource.safetyStatus }}</small>
                <p>{{ resource.citationSummary }}</p>
                <blockquote>{{ resource.markdownContent }}</blockquote>
              </li>
            </ul>
            <p v-if="pendingReviewResources.length === 0" class="empty-shelf">暂无待教师审核资源。</p>
          </section>

          <section class="resource-shelf revision" data-test="resource-shelf-revision">
            <div class="resource-shelf-heading">
              <strong>退回修改 returned</strong>
              <span>{{ revisionResources.length }}</span>
            </div>
            <ul class="resource-list compact">
              <li v-for="resource in revisionResources" :key="resource.resourceId">
                <div class="resource-title-row">
                  <strong>{{ resource.type }}</strong>
                  <span>{{ resource.title }}</span>
                  <em :class="['status-pill', resource.status.toLowerCase()]">{{ resource.reviewStatus }}</em>
                </div>
                <small>{{ resource.resourceId }} / {{ resource.modality }} / {{ resource.safetyStatus }}</small>
                <p>{{ resource.citationSummary }}</p>
                <blockquote>{{ resource.markdownContent }}</blockquote>
              </li>
            </ul>
            <p v-if="revisionResources.length === 0" class="empty-shelf">暂无退回修改。</p>
          </section>

          <section class="resource-shelf other" data-test="resource-shelf-other">
            <div class="resource-shelf-heading">
              <strong>其他状态</strong>
              <span>{{ otherReviewResources.length }}</span>
            </div>
            <ul class="resource-list compact">
              <li v-for="resource in otherReviewResources" :key="resource.resourceId">
                <div class="resource-title-row">
                  <strong>{{ resource.type }}</strong>
                  <span>{{ resource.title }}</span>
                  <em :class="['status-pill', resource.status.toLowerCase()]">{{ resource.reviewStatus }}</em>
                </div>
                <small>{{ resource.resourceId }} / {{ resource.modality }} / {{ resource.safetyStatus }}</small>
                <p>{{ resource.citationSummary }}</p>
                <blockquote>{{ resource.markdownContent }}</blockquote>
              </li>
            </ul>
            <p v-if="otherReviewResources.length === 0" class="empty-shelf">暂无其他审核状态。</p>
          </section>
        </div>
      </article>

      <article class="panel assessment-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">测评反馈 / Assessment</p>
            <h3>JOIN 诊断测评</h3>
          </div>
          <ClipboardCheck :size="20" aria-hidden="true" />
        </div>
        <button
          class="tool-button"
          type="button"
          data-test="submit-assessment"
          :disabled="isLoading"
          @click="submitAssessment"
        >
          {{ state.loadingAction === 'assessment' ? '提交中' : '开始测评' }}
        </button>
        <label class="field-control">
          <span>作答内容</span>
          <textarea
            v-model="state.assessmentAnswer"
            data-test="assessment-answer-input"
            rows="4"
            placeholder="提交前请说明你的推理过程"
          ></textarea>
        </label>
        <p class="answer-text">{{ state.assessmentStatus }}</p>
        <div>
          <div class="meter-heading">
            <span>掌握度 Mastery</span>
            <strong>{{ state.mastery }}%</strong>
          </div>
          <div class="mastery-meter" aria-label="Mastery">
            <span :style="{ width: `${state.mastery}%` }"></span>
          </div>
        </div>
        <div class="trace-chip">
          重规划 Replan <strong>{{ state.replanRecordId }}</strong>
        </div>
      </article>

      <article class="panel profile-dimensions-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">画像维度 / Profile Dimensions</p>
            <h3>维度证据</h3>
          </div>
          <UserRound :size="20" aria-hidden="true" />
        </div>
        <ul class="dimension-list">
          <li v-for="dimension in state.learnerProfile.dimensions" :key="dimension.name">
            <strong>{{ dimension.name }}</strong>
            <span>{{ dimension.value }} / confidence {{ dimension.confidence }}</span>
            <p>{{ dimension.evidence }}</p>
          </li>
        </ul>
      </article>
      </div>

      <div class="student-diagnostics" data-test="student-diagnostics">
        <article class="panel trace-panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">Agent Trace</p>
              <h3>小时间线 Timeline</h3>
            </div>
            <GitBranch :size="20" aria-hidden="true" />
          </div>
          <ol class="trace-list">
            <li v-for="step in traceSteps" :key="`${step.actor}-${step.detail}`">
              <div>
                <strong>{{ step.actor }}</strong>
                <em :class="['status-pill', step.status.toLowerCase()]">{{ step.status }}</em>
              </div>
              <p>{{ step.detail }}</p>
              <span>{{ step.latencyMs }} ms</span>
            </li>
          </ol>
        </article>
        <article class="panel interface-panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">接口数据来源</p>
              <h3>当前页面主要 API</h3>
            </div>
            <Database :size="20" aria-hidden="true" />
          </div>
          <ul class="api-source-list expanded">
            <li>GET /api/student/profile</li>
            <li>GET /api/learning/goals</li>
            <li>GET /api/learning/path</li>
            <li>POST /api/rag/query</li>
            <li>GET /api/rag/citations</li>
            <li>GET /api/resources/generation-tasks/{taskId}</li>
            <li>POST /api/assessment/answers</li>
          </ul>
        </article>
        <article class="panel status-showcase" data-test="status-showcase">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">状态展示方式</p>
              <h3>关键状态示例</h3>
            </div>
            <ListChecks :size="20" aria-hidden="true" />
          </div>
          <div class="state-token-grid">
            <span class="state-token loading">loading 骨架屏</span>
            <span class="state-token failed">failed 重试</span>
            <span class="state-token empty">empty 空状态</span>
            <span class="state-token pending">pending review 待教师审核</span>
            <span class="state-token no-source">no source 拒答</span>
            <span class="state-token approved">approved 已批准</span>
          </div>
        </article>
      </div>
    </section>
  </section>
</template>
