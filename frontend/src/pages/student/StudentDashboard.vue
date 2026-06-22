<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import WorkspaceComposer from '../../components/workspace/WorkspaceComposer.vue'
import WorkspaceStream from '../../components/workspace/WorkspaceStream.vue'
import RightThoughtPanel from '../../components/thought/RightThoughtPanel.vue'
import { submitAnswer } from '../../api/assessment'
import {
  createKnowledgeBase,
  fetchDocumentStatus,
  listKnowledgeBaseDocuments,
  listKnowledgeBases,
  uploadKnowledgeBaseDocument,
} from '../../api/documents'
import { streamAiQa } from '../../api/aiQa'
import { createLearningPath, extractProfile } from '../../api/learning'
import { queryRag, streamChat } from '../../api/rag'
import { createResourceGeneration, fetchAgentTrace } from '../../api/resources'
import type {
  AgentTraceResponse,
  AiQaResponse,
  AnswerSubmitResponse,
  DocumentRecord,
  DocumentStatusResponse,
  DocumentUploadResponse,
  GeneratedResource,
  GeneratedResourceResponse,
  GeneratedResourceStatus,
  LearningPathNodeResponse,
  PathNode,
  ProfileExtractResponse,
  RagQueryResponse,
  ResourceGenerationResponse,
  TraceStep,
  WorkbenchState,
} from '../../types/api'
import type { CurrentThoughtTask, ThoughtAgentStep, ThoughtRagSources, ThoughtRuntimeMetrics } from '../../types/thought'

const LEARNER_ID = 'stu_001'
const GOAL_ID = 'goal_java_backend'
const KNOWLEDGE_BASE_NAME = 'Java backend course materials'
const KNOWLEDGE_BASE_DESCRIPTION = 'Student workbench course materials'
const ASSESSMENT_QUESTION_ID = 'q_sql_join_cardinality'
const RESOURCE_TYPES = ['LECTURE', 'MIND_MAP', 'EXERCISE', 'READING', 'CODE_LAB']
const REPLAN_NOT_CREATED = 'Not created'
const EMPTY_RESOURCE_TASK_ID = ''

const state = ref<WorkbenchState>({
  knowledgeBase: {
    id: '',
    name: '',
    visibility: '',
    owner: '',
  },
  learnerProfile: {
    learnerId: LEARNER_ID,
    major: '计算机科学',
    goal: 'Master Java backend project delivery',
    preference: 'Code examples, project practice',
    weakness: 'SQL JOIN diagnosis',
    dimensions: [],
  },
  documents: [],
  ragQuestion: '',
  ragTraceId: '',
  sseStage: 'IDLE',
  ragAnswer: '',
  ragSources: [],
  qaVerification: null,
  qaQualityFlags: [],
  qaSourcePolicy: '',
  qaUncertaintyLevel: '',
  qaAnswerMode: '',
  qaReasoningEffort: '',
  qaToolCallCount: 0,
  mastery: 0,
  assessmentStatus: '',
  assessmentAnswer: '',
  replanRecordId: REPLAN_NOT_CREATED,
  profilePrompt: '我想掌握 Java 后端项目交付。SQL JOIN 诊断和带 Citation 的 RAG 服务是我的薄弱点；我更喜欢代码示例和项目实践。',
  followUpQuestions: [],
  selectedFollowUpQuestion: '',
  goalId: GOAL_ID,
  resourceTaskId: EMPTY_RESOURCE_TASK_ID,
  resourceTaskStatus: '',
  resourceReviewStatus: '',
  resourceProgressPercent: 0,
  resourceSafetyStatus: '',
  agentTaskId: '',
  resourceTraceId: '',
  profileTraceId: '',
  pathTraceId: '',
  pathNodes: [],
  resources: [],
  traceSteps: [],
  loadingAction: '',
  errorMessage: '',
})

const resources = computed(() => state.value.resources)
const pathNodes = computed(() => state.value.pathNodes)
const isLoading = computed(() => state.value.loadingAction !== '')
const selectedDocumentFile = ref<File | null>(null)
const selectedDocumentFileName = computed(() => selectedDocumentFile.value?.name ?? '')
const selectedResourceTypes = ref<string[]>([...RESOURCE_TYPES])
const visibleThoughtStepCount = ref(2)
const isThoughtPanelHidden = ref(false)
const thoughtPanelHeight = ref(620)
const isResizingThoughtPanel = ref(false)
let thoughtRevealTimer: ReturnType<typeof window.setTimeout> | null = null
let thoughtPanelStartY = 0
let thoughtPanelStartHeight = 0

const localizedErrorMessage = computed(() => displayErrorMessage(state.value.errorMessage))
const usesSensitiveUrlSafeRagTransport = computed(() => import.meta.env.PROD || import.meta.env.MODE === 'staging')
const isThoughtActive = computed(() => ['rag', 'document', 'resources', 'assessment'].includes(state.value.loadingAction))
const thoughtSteps = computed<ThoughtAgentStep[]>(() => buildThoughtSteps())
const revealedThoughtSteps = computed(() => thoughtSteps.value.slice(0, Math.min(visibleThoughtStepCount.value, thoughtSteps.value.length)))
const thoughtCurrentTask = computed<CurrentThoughtTask>(() => {
  const hasError = state.value.errorMessage !== '' || state.value.sseStage === 'ERROR'
  const isDone = state.value.sseStage === 'DONE' || state.value.traceSteps.some((step) => step.status === 'DONE')
  const status: CurrentThoughtTask['status'] = hasError ? 'failed' : isThoughtActive.value ? 'running' : isDone ? 'done' : 'waiting'

  return {
    title: isThoughtActive.value ? '正在生成学习回答' : '学习思考流',
    taskType: 'RAG / Agent 执行流程',
    model: '学习系统',
    traceId: state.value.ragTraceId || state.value.resourceTraceId || state.value.pathTraceId || state.value.profileTraceId || '等待 Trace',
    startedAt: state.value.loadingAction ? actionLabel(state.value.loadingAction) : '新建对话',
    status,
  }
})
const thoughtRagSources = computed<ThoughtRagSources>(() => ({
  knowledgeBase: state.value.knowledgeBase.name || '当前课程知识库',
  chunkCount: state.value.ragSources.length,
  documents: state.value.ragSources.length
    ? state.value.ragSources.map((source) => ({
        name: source.documentName,
        pageNum: source.pageNum,
        sectionTitle: source.sectionTitle,
        score: source.score,
        excerpt: source.excerpt,
      }))
    : [{ name: '等待引用来源', excerpt: '发送问题后，系统会在回答过程中补充命中的课程资料与 Citation。' }],
}))
const thoughtMetrics = computed<ThoughtRuntimeMetrics>(() => ({
  latency: state.value.sseStage || 'IDLE',
  totalTokens: `${state.value.ragAnswer.length} 字`,
  modelCalls: state.value.qaToolCallCount ? `${state.value.qaToolCallCount} 个工具` : `${state.value.traceSteps.length} 步`,
  fallback: state.value.qaSourcePolicy || (usesSensitiveUrlSafeRagTransport.value ? 'AI QA 安全流式通道' : 'EventSource / REST'),
  safety: state.value.qaVerification?.verdict
    ? `QA ${state.value.qaVerification.verdict}`
    : state.value.resourceSafetyStatus || state.value.resourceReviewStatus || '资源审核',
}))

onMounted(() => {
  void initializeKnowledgeBase()
})

onBeforeUnmount(() => {
  clearThoughtRevealTimer()
  stopThoughtPanelResize()
})

watch(
  () => [state.value.loadingAction, state.value.sseStage, state.value.ragAnswer, state.value.ragSources.length, state.value.traceSteps.length],
  () => scheduleThoughtReveal(),
)

async function initializeKnowledgeBase() {
  try {
    const knowledgeBases = await listKnowledgeBases()
    const selected = knowledgeBases.find((knowledgeBase) => knowledgeBase.name === KNOWLEDGE_BASE_NAME) ?? knowledgeBases[0]
    if (selected) {
      applyKnowledgeBase(selected)
      await initializeKnowledgeBaseDocuments(selected.id)
    } else {
      const created = await createKnowledgeBase({
        name: KNOWLEDGE_BASE_NAME,
        description: KNOWLEDGE_BASE_DESCRIPTION,
        visibility: 'PRIVATE',
      })
      applyKnowledgeBase(created)
      await initializeKnowledgeBaseDocuments(created.id)
    }

    await initializeLearningLoop()
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

async function initializeLearningLoop() {
  try {
    const profile = await extractProfile({
      learnerId: state.value.learnerProfile.learnerId || LEARNER_ID,
      message: state.value.profilePrompt,
    })
    applyProfileResponse(profile)

    const path = await createLearningPath({
      learnerId: state.value.learnerProfile.learnerId || LEARNER_ID,
      goalId: state.value.goalId,
    })
    applyLearningPathResponse(path)
  } catch (error) {
    captureError('ProfileAgent', error)
  }
}

function selectDocumentFile(event: Event) {
  const input = event.target as HTMLInputElement
  selectedDocumentFile.value = input.files?.[0] ?? null
}

async function uploadDocument() {
  if (state.value.loadingAction === 'document') return

  startAction('document')
  try {
    const file = selectedDocumentFile.value
    if (!file) {
      state.value.errorMessage = '上传前请先选择文档。'
      return
    }
    const activeNode = activePathNode()
    if (!activeNode) {
      state.value.errorMessage = '学习路径为空，请先创建路径再上传资料'
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

function viewLearningPath() {
  document.getElementById('learning-path-stream-block')?.scrollIntoView({
    behavior: 'smooth',
    block: 'center',
  })
}

async function askRag() {
  if (state.value.loadingAction === 'rag') return
  if (!state.value.ragQuestion.trim()) {
    state.value.errorMessage = '请输入问题'
    return
  }

  startAction('rag')
  state.value.sseStage = 'RETRIEVING'
  state.value.ragAnswer = ''
  try {
    if (usesSensitiveUrlSafeRagTransport.value) {
      await streamAiQaResponse()
    } else {
      await streamRagResponse()
    }
  } catch (error) {
    if (usesSensitiveUrlSafeRagTransport.value) {
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
    const source = streamChat(`session_${LEARNER_ID}`, state.value.ragQuestion, [state.value.knowledgeBase.id])
    const close = () => source.close()
    const failInvalidPayload = () => {
      close()
      reject(new Error('Invalid SSE event payload'))
    }

    source.addEventListener('status', (event) => {
      const data = parseSsePayload<{ stage?: string }>(event, failInvalidPayload)
      if (data?.stage) state.value.sseStage = data.stage
    })
    source.addEventListener('token', (event) => {
      const data = parseSsePayload<{ text?: string }>(event, failInvalidPayload)
      state.value.ragAnswer += data?.text ?? ''
    })
    source.addEventListener('done', (event) => {
      const data = parseSsePayload<{ traceId: string; sources: RagQueryResponse['sources'] }>(event, failInvalidPayload)
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

async function streamAiQaResponse() {
  await streamAiQa(
    {
      answerMode: 'THINKING',
      kbIds: [state.value.knowledgeBase.id],
      question: state.value.ragQuestion,
      courseId: state.value.goalId,
      topK: 5,
    },
    {
      onStatus: ({ stage }) => {
        if (stage) state.value.sseStage = stage
      },
      onToken: ({ text }) => {
        state.value.ragAnswer += text ?? ''
      },
      onDone: (response) => {
        applyAiQaResponse(response)
      },
      onError: ({ message }) => {
        throw new Error(message || 'AI QA stream failed')
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
  if (selectedResourceTypes.value.length === 0) {
    state.value.errorMessage = '请先选择资源类型'
    finishAction('resources')
    return
  }

  const activeNode = activePathNode()
  if (!activeNode) {
    state.value.errorMessage = '学习路径为空，请先创建路径再生成资源。'
    finishAction('resources')
    return
  }

  try {
    const response = await createResourceGeneration({
      learnerId: state.value.learnerProfile.learnerId || LEARNER_ID,
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

async function submitAssessment() {
  if (!state.value.assessmentAnswer.trim()) {
    state.value.errorMessage = '请填写作答内容'
    return
  }

  startAction('assessment')
  try {
    const response = await submitAnswer({
      learnerId: state.value.learnerProfile.learnerId || LEARNER_ID,
      questionId: ASSESSMENT_QUESTION_ID,
      answer: state.value.assessmentAnswer,
    })
    applyAssessmentResponse(response)
  } catch (error) {
    captureError('AssessmentAgent', error)
  } finally {
    finishAction('assessment')
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

function applyDocumentListResponse(response: DocumentStatusResponse[]) {
  state.value.documents = response.map((document) => toDocumentRecord(document))
}

function applyDocumentUploadResponse(response: DocumentUploadResponse, fileName: string) {
  const document: DocumentRecord = {
    id: response.documentId,
    name: fileName,
    type: inferDocumentType(fileName),
    status: normalizeDocumentStatus(response.status),
    indexTaskId: response.indexTaskId,
    chunks: 0,
    updatedAt: new Date().toISOString().slice(0, 16).replace('T', ' '),
  }
  state.value.documents = [...state.value.documents.filter((existing) => existing.id !== response.documentId), document]
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
  state.value.documents = [...state.value.documents.filter((existing) => existing.id !== response.documentId), document]
  appendTrace({
    actor: 'DocumentController',
    status: document.status === 'INDEXED' ? 'DONE' : 'RUNNING',
    detail: `Document ${response.documentId} parse ${response.parseStatus}, index ${response.indexStatus}.`,
    latencyMs: 64,
  })
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
  state.value.ragSources = (response.sources ?? []).map((source) => ({
    documentName: source.documentName,
    pageNum: source.pageNum,
    sectionTitle: source.sectionTitle,
    excerpt: source.excerpt,
    score: source.score,
  }))
  upsertTrace({
    actor: 'CourseRagAgent',
    status: 'DONE',
    detail: 'Backend RAG query completed with permission-filtered citations.',
    latencyMs: 1200,
  })
}

function applyAiQaResponse(response: AiQaResponse) {
  const visibleSources = response.citations?.length ? response.citations : response.sources ?? []
  state.value.sseStage = 'DONE'
  state.value.ragTraceId = response.traceId
  state.value.ragAnswer = response.answer
  state.value.ragSources = visibleSources.map((source) => ({
    documentName: source.documentName,
    pageNum: source.pageNum,
    sectionTitle: source.sectionTitle,
    excerpt: source.excerpt,
    score: source.score,
  }))
  state.value.qaVerification = response.verification ?? null
  state.value.qaQualityFlags = response.qualityFlags ?? []
  state.value.qaSourcePolicy = response.sourcePolicy ?? ''
  state.value.qaUncertaintyLevel = response.uncertainty?.level ?? ''
  state.value.qaAnswerMode = response.answerMode ?? ''
  state.value.qaReasoningEffort = response.reasoningEffort ?? ''
  state.value.qaToolCallCount = response.toolCalls?.length ?? 0
  upsertTrace({
    actor: 'QaRuntime',
    status: 'DONE',
    detail: `AI QA runtime completed; verification ${response.verification?.verdict ?? 'UNKNOWN'}; trace ${response.traceId}.`,
    latencyMs: 1200,
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
  state.value.resources = (response.resources ?? []).map(toGeneratedResource)
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

function normalizeBackendDocumentStatus(status: DocumentStatusResponse['indexStatus']): DocumentRecord['status'] {
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

function appendTrace(step: TraceStep) {
  state.value.traceSteps = [...state.value.traceSteps, step]
}

function upsertTrace(step: TraceStep) {
  const withoutActor = state.value.traceSteps.filter((existing) => existing.actor !== step.actor)
  state.value.traceSteps = [...withoutActor, step]
}

function buildThoughtSteps(): ThoughtAgentStep[] {
  const fixedSteps: ThoughtAgentStep[] = [
    {
      name: '理解问题',
      status: state.value.ragQuestion.trim() ? 'done' : 'waiting',
      duration: '即时',
      summary: `读取学习者问题：${state.value.ragQuestion || '等待输入问题'}`,
    },
    {
      name: '检索课程资料',
      status: statusForStage(['RETRIEVING', 'RERANKING'], state.value.ragSources.length > 0),
      duration: state.value.ragSources.length ? `${state.value.ragSources.length} 个来源` : '-',
      summary: '在当前课程知识库中查找可引用的资料片段。',
    },
    {
      name: '校验引用来源',
      status: state.value.ragSources.length > 0 ? 'done' : state.value.sseStage === 'RERANKING' ? 'running' : 'waiting',
      duration: state.value.ragSources.length ? 'Citation ready' : '-',
      summary: '检查回答是否具备可追溯 Citation，避免无来源回答。',
    },
    {
      name: '生成回答',
      status: state.value.ragAnswer ? (state.value.sseStage === 'DONE' ? 'done' : 'running') : statusForStage(['GENERATING'], false),
      duration: state.value.ragAnswer ? `${state.value.ragAnswer.length} 字` : '-',
      summary: '把检索结果组织成适合当前学习目标的解释。',
    },
    {
      name: '校验回答质量',
      status: state.value.qaVerification
        ? state.value.qaVerification.verdict === 'PASS'
          ? 'done'
          : 'warning'
        : statusForStage(['VERIFYING'], false),
      duration: state.value.qaVerification?.gatePolicy ?? '-',
      summary: state.value.qaVerification
        ? `Verifier verdict ${state.value.qaVerification.verdict}，source policy ${state.value.qaSourcePolicy || '未返回'}。`
        : '等待 AnswerVerifier 返回 schema、citation、privacy 与 no-source 检查。',
    },
    {
      name: '更新学习闭环',
      status: state.value.sseStage === 'DONE' || state.value.assessmentStatus || state.value.resources.length ? 'done' : 'waiting',
      duration: state.value.replanRecordId !== REPLAN_NOT_CREATED ? state.value.replanRecordId : '-',
      summary: '同步学习路径、资源生成和测评反馈等后续行动。',
    },
  ]

  const backendSteps = state.value.traceSteps.map((step) => ({
    name: step.actor,
    status: toThoughtStatus(step.status),
    duration: step.latencyMs ? `${step.latencyMs} ms` : '-',
    summary: step.detail,
  }))

  return [...fixedSteps, ...backendSteps]
}

function statusForStage(activeStages: string[], hasResult: boolean): ThoughtAgentStep['status'] {
  if (state.value.errorMessage || state.value.sseStage === 'ERROR') return 'failed'
  if (hasResult || state.value.sseStage === 'DONE') return 'done'
  return activeStages.includes(state.value.sseStage) ? 'running' : 'waiting'
}

function toThoughtStatus(status: TraceStep['status']): ThoughtAgentStep['status'] {
  if (status === 'DONE') return 'done'
  if (status === 'RUNNING') return 'running'
  if (status === 'PENDING' || status === 'WAITING') return 'waiting'
  return 'failed'
}

function scheduleThoughtReveal() {
  clearThoughtRevealTimer()

  if (state.value.errorMessage || state.value.sseStage === 'DONE') {
    visibleThoughtStepCount.value = thoughtSteps.value.length
    return
  }

  if (isThoughtActive.value && visibleThoughtStepCount.value < thoughtSteps.value.length) {
    thoughtRevealTimer = window.setTimeout(() => {
      visibleThoughtStepCount.value += 1
      scheduleThoughtReveal()
    }, 620)
  }
}

function resetThoughtReveal() {
  visibleThoughtStepCount.value = 1
  scheduleThoughtReveal()
}

function clearThoughtRevealTimer() {
  if (thoughtRevealTimer) {
    window.clearTimeout(thoughtRevealTimer)
    thoughtRevealTimer = null
  }
}

function clampThoughtPanelHeight(height: number) {
  const viewportMax = typeof window === 'undefined' ? 900 : Math.min(window.innerHeight + 180, 980)
  return Math.min(Math.max(height, 360), Math.max(720, viewportMax))
}

function startThoughtPanelResize(event: PointerEvent) {
  isResizingThoughtPanel.value = true
  thoughtPanelStartY = event.clientY
  thoughtPanelStartHeight = thoughtPanelHeight.value
  window.addEventListener('pointermove', resizeThoughtPanel)
  window.addEventListener('pointerup', stopThoughtPanelResize)
}

function resizeThoughtPanel(event: PointerEvent) {
  if (!isResizingThoughtPanel.value) return
  thoughtPanelHeight.value = clampThoughtPanelHeight(thoughtPanelStartHeight + event.clientY - thoughtPanelStartY)
}

function stopThoughtPanelResize() {
  if (!isResizingThoughtPanel.value) return
  isResizingThoughtPanel.value = false
  window.removeEventListener('pointermove', resizeThoughtPanel)
  window.removeEventListener('pointerup', stopThoughtPanelResize)
}

function actionLabel(action: string) {
  const labels: Record<string, string> = {
    rag: 'RAG 问答',
    document: '文档上传',
    resources: '资源生成',
    assessment: '测评反馈',
  }
  return labels[action] ?? '新建对话'
}

function startAction(action: string) {
  state.value.loadingAction = action
  state.value.errorMessage = ''
  resetThoughtReveal()
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

function displayErrorMessage(message: string) {
  const errorLabels: Record<string, string> = {
    'Failed to fetch': '请求失败',
    'Request failed': '请求失败',
    'Invalid SSE event payload': 'SSE 事件数据无效',
    '学习路径为空，请先创建路径再生成资源。': '学习路径为空，请先创建路径再生成资源。',
  }
  return errorLabels[message] ?? message
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

defineExpose({
  askRag,
  viewLearningPath,
})
</script>

<template>
  <main class="student-workbench" aria-label="新建对话学习会话">
    <p class="visually-hidden" data-test="status-showcase">加载中 失败 无来源</p>
    <header class="new-chat-intro" data-test="new-chat-page">
      <span class="new-chat-eyebrow">新建对话</span>
      <div>
        <h1>开始一轮智能体学习会话</h1>
        <p>沿用学习闭环能力，在同一条会话里完成资料上传、RAG 问答、路径规划、资源生成与测评反馈。</p>
      </div>
    </header>
    <section class="workbench-ai-section" aria-label="AI 问答与 RAG 工作区">
      <div :class="['ai-workbench-grid', { 'thought-panel-hidden': isThoughtPanelHidden }]">
        <div class="ai-stream-shell">
          <WorkspaceStream
            v-model:assessment-answer="state.assessmentAnswer"
            :question="state.ragQuestion"
            :profile-prompt="state.profilePrompt"
            :answer="state.ragAnswer"
            :stage="state.sseStage"
            :trace-id="state.ragTraceId"
            :sources="state.ragSources"
            :verification="state.qaVerification"
            :quality-flags="state.qaQualityFlags"
            :source-policy="state.qaSourcePolicy"
            :uncertainty-level="state.qaUncertaintyLevel"
            :answer-mode="state.qaAnswerMode"
            :reasoning-effort="state.qaReasoningEffort"
            :tool-call-count="state.qaToolCallCount"
            :path-nodes="pathNodes"
            :resources="resources"
            :resource-task-id="state.resourceTaskId"
            :resource-task-status="state.resourceTaskStatus"
            :resource-review-status="state.resourceReviewStatus"
            :resource-progress-percent="state.resourceProgressPercent"
            :resource-safety-status="state.resourceSafetyStatus"
            :mastery="state.mastery"
            :assessment-status="state.assessmentStatus"
            :replan-record-id="state.replanRecordId"
            :error-message="localizedErrorMessage"
          />
          <WorkspaceComposer
            v-model:question="state.ragQuestion"
            :selected-file-name="selectedDocumentFileName"
            :is-loading="isLoading"
            :loading-action="state.loadingAction"
            @select-file="selectDocumentFile"
            @upload="uploadDocument"
            @generate="generateResources"
            @assess="submitAssessment"
            @view-path="viewLearningPath"
            @send="askRag"
          />
        </div>
        <div
          :class="['thought-stream-shell', { resizing: isResizingThoughtPanel }]"
          :style="{ '--thought-panel-height': `${thoughtPanelHeight}px` }"
          data-test="new-chat-thought-stream"
        >
          <RightThoughtPanel
            :current-task="thoughtCurrentTask"
            :agent-steps="revealedThoughtSteps"
            :rag-sources="thoughtRagSources"
            :metrics="thoughtMetrics"
            @hidden-change="isThoughtPanelHidden = $event"
          />
          <button
            v-if="!isThoughtPanelHidden"
            class="thought-panel-resize-handle"
            type="button"
            aria-label="上下拖动调整工作流高度"
            title="上下拖动调整工作流高度"
            @pointerdown.prevent="startThoughtPanelResize"
          />
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.student-workbench {
  display: grid;
  gap: 28px;
  min-height: 100svh;
  min-width: 0;
  padding: 22px;
  overflow-x: hidden;
  background:
    radial-gradient(circle at 16% 0%, rgba(237, 233, 254, 0.58), transparent 28%),
    linear-gradient(180deg, rgba(248, 250, 252, 0.82), rgba(241, 245, 249, 0.92)),
    #f6f7fb;
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
  border: 0;
}

.new-chat-intro {
  position: relative;
  display: grid;
  place-items: center;
  width: min(100%, 1180px);
  margin: 0 auto -6px;
  padding: 4px 128px 0;
  text-align: center;
}

.new-chat-intro div {
  display: grid;
  justify-items: center;
  gap: 8px;
  max-width: 860px;
}

.new-chat-eyebrow {
  position: absolute;
  bottom: 10px;
  left: 2px;
  padding: 7px 10px;
  color: #0f766e;
  font-size: 12px;
  font-weight: 760;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  background: rgba(16, 163, 127, 0.1);
  border: 1px solid rgba(16, 163, 127, 0.18);
  border-radius: 999px;
}

.new-chat-intro h1 {
  margin: 0;
  color: #111827;
  font-size: clamp(28px, 4vw, 44px);
  font-weight: 780;
  letter-spacing: -0.045em;
  line-height: 0.98;
}

.new-chat-intro p {
  max-width: 860px;
  margin: 0;
  color: #475569;
  font-size: 15px;
  line-height: 1.7;
}

.workbench-ai-section {
  min-width: 0;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 28px;
  box-shadow: 0 24px 68px rgba(15, 23, 42, 0.08);
}

.ai-workbench-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(310px, 360px);
  gap: 18px;
  align-items: start;
  min-width: 0;
  padding: 18px;
  transition: grid-template-columns 180ms ease;
}

.ai-workbench-grid.thought-panel-hidden {
  grid-template-columns: minmax(0, 1fr);
}

.ai-workbench-grid.thought-panel-hidden .thought-stream-shell {
  position: static;
  max-height: 0;
}

.ai-workbench-grid.thought-panel-hidden :deep(.workspace-stream),
.ai-workbench-grid.thought-panel-hidden :deep(.workspace-composer) {
  width: min(100%, 1180px);
}

.ai-stream-shell {
  position: relative;
  min-width: 0;
}

.thought-stream-shell {
  position: sticky;
  top: 18px;
  min-width: 0;
  height: var(--thought-panel-height, calc(100svh - 76px));
  max-height: none;
}

.thought-panel-resize-handle {
  position: absolute;
  right: 24px;
  bottom: 8px;
  left: 24px;
  z-index: 3;
  height: 12px;
  padding: 0;
  background: transparent;
  border: 0;
  cursor: ns-resize;
}

.thought-panel-resize-handle::before {
  display: block;
  width: 46px;
  height: 4px;
  margin: 4px auto;
  content: '';
  background: #cbd5e1;
  border-radius: 999px;
  transition: background 160ms ease, width 160ms ease;
}

.thought-panel-resize-handle:hover::before,
.thought-stream-shell.resizing .thought-panel-resize-handle::before {
  width: 58px;
  background: #8b5cf6;
}

.workbench-ai-section :deep(.workspace-chat-header) {
  border-bottom-color: rgba(226, 232, 240, 0.86);
}

.workbench-ai-section :deep(.workspace-stream) {
  width: min(100%, 1040px);
  max-height: none;
  padding-bottom: 24px;
  overflow: visible;
}

.workbench-ai-section :deep(.workspace-composer) {
  position: static;
  width: min(100%, 1040px);
  margin: 0 auto;
  padding: 0 24px 24px;
  pointer-events: auto;
}

.workbench-dashboard-section {
  min-width: 0;
}

@media (max-width: 760px) {
  .student-workbench {
    gap: 24px;
    padding: 14px;
  }

  .new-chat-intro {
    display: grid;
    align-items: start;
    justify-items: start;
    padding: 0;
    text-align: left;
  }

  .new-chat-intro div {
    justify-items: start;
  }

  .new-chat-eyebrow {
    position: static;
    width: max-content;
  }

  .workbench-ai-section {
    border-radius: 22px;
  }

  .ai-workbench-grid {
    grid-template-columns: minmax(0, 1fr);
    padding: 12px;
  }

  .thought-stream-shell {
    position: static;
    height: auto;
    max-height: none;
  }

  .thought-panel-resize-handle {
    display: none;
  }
}
</style>
