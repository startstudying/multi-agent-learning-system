import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory } from 'vue-router'
import App from './App.vue'
import { createAppRouter } from './router'

class MockEventSource {
  static instances: MockEventSource[] = []

  readonly url: string
  readonly listeners = new Map<string, Array<(event: MessageEvent) => void>>()
  onerror: ((event: Event) => void) | null = null
  close = vi.fn()

  constructor(url: string) {
    this.url = url
    MockEventSource.instances.push(this)
  }

  addEventListener(eventName: string, listener: (event: MessageEvent) => void) {
    const listeners = this.listeners.get(eventName) ?? []
    listeners.push(listener)
    this.listeners.set(eventName, listeners)
  }

  emit(eventName: string, data: unknown) {
    const event = { data: JSON.stringify(data) } as MessageEvent
    this.listeners.get(eventName)?.forEach((listener) => listener(event))
  }

  emitRaw(eventName: string, data: string) {
    const event = { data } as MessageEvent
    this.listeners.get(eventName)?.forEach((listener) => listener(event))
  }

  fail() {
    this.onerror?.(new Event('error'))
  }
}

function apiEnvelope<T>(data: T) {
  return {
    ok: true,
    json: async () => ({ code: 'OK', message: 'success', data }),
  } as Response
}

function apiErrorEnvelope(message: string, status = 500) {
  return {
    ok: false,
    status,
    json: async () => ({ code: 'ERROR', message, data: null }),
  } as Response
}

function sseResponse(events: Array<{ event: string; data: unknown }>) {
  const encoder = new TextEncoder()
  const body = new ReadableStream({
    start(controller) {
      events.forEach((event) => {
        controller.enqueue(
          encoder.encode(`event:${event.event}\ndata:${JSON.stringify(event.data)}\n\n`),
        )
      })
      controller.close()
    },
  })

  return {
    ok: true,
    status: 200,
    headers: new Headers({ 'Content-Type': 'text/event-stream' }),
    body,
  } as Response
}

function existingKnowledgeBaseEnvelope() {
  return apiEnvelope([
    {
      id: 'kb_java_backend',
      name: 'Java backend course materials',
      description: 'Student workbench course materials',
      visibility: 'PRIVATE',
      ownerUserId: 'stu_001',
      createdAt: '2026-06-05T00:00:00Z',
      updatedAt: '2026-06-05T00:00:00Z',
    },
  ])
}

function createdKnowledgeBaseEnvelope() {
  return apiEnvelope({
    id: 'kb_backend_created',
    name: 'Java backend course materials',
    description: 'Student workbench course materials',
    visibility: 'PRIVATE',
    ownerUserId: 'stu_001',
    createdAt: '2026-06-05T00:00:00Z',
    updatedAt: '2026-06-05T00:00:00Z',
  })
}

function knowledgeBaseDocumentsEnvelope() {
  return apiEnvelope([
    {
      documentId: 'doc_backend_existing',
      kbId: 'kb_java_backend',
      name: 'backend-existing-notes.md',
      parseStatus: 'READY',
      indexStatus: 'INDEXED',
      errorMessage: null,
      version: 3,
    },
    {
      documentId: 'doc_backend_pending',
      kbId: 'kb_java_backend',
      name: 'backend-pending-lab.pdf',
      parseStatus: 'PENDING',
      indexStatus: 'PARSING',
      errorMessage: null,
      version: 1,
    },
  ])
}

function emptyKnowledgeBaseDocumentsEnvelope() {
  return apiEnvelope([])
}

async function mountApp(path = '/') {
  const router = createAppRouter(createMemoryHistory())
  router.push(path)
  await router.isReady()
  return mount(App, {
    global: {
      plugins: [router],
    },
  })
}

describe('AI Learning OS workbench', () => {
  beforeEach(() => {
    window.localStorage.clear()
    vi.stubGlobal('fetch', vi.fn())
    MockEventSource.instances = []
  })

  afterEach(() => {
    window.localStorage.clear()
    vi.unstubAllGlobals()
    vi.unstubAllEnvs()
  })

  it('renders the new chat learning session by default', async () => {
    const wrapper = await mountApp()

    const text = wrapper.text()

    expect(text).toContain('新建对话')
    expect(text).toContain('开始一轮智能体学习会话')
    expect(text).toContain('RAG 问答区')
    expect(text).toContain('生成资源')
    expect(text).toContain('测评反馈')
    expect(text).toContain('新建对话学习会话')
    expect(text).not.toContain('AI 学习工作区')
    expect(text).not.toContain('学习画像')
    expect(text).not.toContain('Agent Trace')
    expect(text).not.toContain('学生学习')
    expect(text).not.toContain('Learning Loop')
  })

  it('shows the ChatGPT-style sidebar and interactive account menu', async () => {
    const wrapper = await mountApp()

    expect(wrapper.get('[data-test="new-chat"]').text()).toContain('新建对话')
    expect(wrapper.get('[data-test="new-chat"]').attributes('data-legacy-test')).toBe('new-learning-task')
    expect(wrapper.text()).toContain('搜索')
    expect(wrapper.text()).toContain('探索')
    expect(wrapper.text()).not.toContain('探索 GPT')
    expect(wrapper.find('[data-test="account-menu"]').exists()).toBe(false)

    await wrapper.get('[data-test="account-menu-trigger"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-test="account-menu"]').text()).toContain('个人账户')
    expect(wrapper.get('[data-test="account-menu"]').text()).toContain('设置')
    expect(wrapper.get('[data-test="account-menu"]').text()).toContain('帮助与反馈')
  })

  it('presents the new chat page without the removed learning workbench sections', async () => {
    const wrapper = await mountApp()
    await flushPromises()

    expect(wrapper.find('[data-test="student-primary-workspace"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="student-support-workspace"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="student-diagnostics"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="status-showcase"]').exists()).toBe(true)
    expect(wrapper.get('[data-test="student-primary-workspace"]').text()).toContain('RAG 问答区')
    expect(wrapper.get('[data-test="student-primary-workspace"]').text()).toContain('学习路径')
    expect(wrapper.get('[data-test="student-primary-workspace"]').text()).toContain('无可靠来源，系统暂不回答')
    expect(wrapper.get('[data-test="new-chat-thought-stream"]').text()).toContain('学习思考流')
    expect(wrapper.get('[data-test="new-chat-thought-stream"]').text()).toContain('理解问题')
    expect(wrapper.text()).not.toContain('AI 学习工作区')
    expect(wrapper.get('[data-test="status-showcase"]').text()).toContain('加载中')
    expect(wrapper.get('[data-test="status-showcase"]').text()).toContain('失败')
    expect(wrapper.get('[data-test="status-showcase"]').text()).toContain('无来源')
  })

  it('uses role-aware shell context and triage-oriented role pages', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(existingKnowledgeBaseEnvelope())
      .mockResolvedValueOnce(emptyKnowledgeBaseDocumentsEnvelope())
      .mockResolvedValueOnce(profileEnvelope())
      .mockResolvedValueOnce(pathEnvelope())
      .mockResolvedValueOnce(apiEnvelope([]))
      .mockResolvedValueOnce(healthEnvelope())
      .mockResolvedValueOnce(analyticsEnvelope())
      .mockResolvedValueOnce(apiEnvelope([]))

    window.localStorage.setItem('ai-learning-os-role', 'teacher')

    const wrapper = await mountApp()
    await flushPromises()

    expect(wrapper.get('[data-test="shell-context"]').text()).toContain('新建对话学习会话')

    await wrapper.get('[data-test="teacher-view"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-test="shell-context"]').text()).toContain('审核工作台')
    expect(wrapper.find('[data-test="teacher-review-workspace"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="teacher-evidence-checklist"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="reject-review"]').exists()).toBe(true)
    expect(wrapper.get('[data-test="reject-review"]').attributes('disabled')).toBeDefined()

    await wrapper.get('[data-test="admin-view"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-test="shell-context"]').text()).toContain('系统健康总览')
    expect(wrapper.find('[data-test="admin-triage"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="admin-dependency-matrix"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="admin-alert-table"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="admin-api-sources"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="status-showcase-admin"]').exists()).toBe(true)
  })

  it('exposes real routes for new chat, teacher, and admin pages', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(existingKnowledgeBaseEnvelope())
      .mockResolvedValueOnce(emptyKnowledgeBaseDocumentsEnvelope())
      .mockResolvedValueOnce(profileEnvelope())
      .mockResolvedValueOnce(pathEnvelope())
      .mockResolvedValueOnce(apiEnvelope([]))
      .mockResolvedValueOnce(healthEnvelope())
      .mockResolvedValueOnce(analyticsEnvelope())
      .mockResolvedValueOnce(apiEnvelope([]))

    window.localStorage.setItem('ai-learning-os-role', 'teacher')

    const wrapper = await mountApp()

    expect(wrapper.get('[data-test="new-chat"]').attributes('href')).toBe('/')
    expect(wrapper.find('[data-test="student-view"]').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('学生学习')
    expect(wrapper.get('[data-test="teacher-view"]').attributes('href')).toBe('/teacher/reviews')
    expect(wrapper.get('[data-test="admin-view"]').attributes('href')).toBe('/admin/operations')

    await wrapper.get('[data-test="teacher-view"]').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('教师审核队列')
    expect(wrapper.text()).toContain('当前决策')
    expect(wrapper.text()).toContain('暂无待审核资源')

    await wrapper.get('[data-test="admin-view"]').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('管理员运维')
    expect(wrapper.text()).toContain('运行时健康')
    expect(wrapper.text()).toContain('依赖健康')

    await wrapper.get('[data-test="new-chat"]').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('开始一轮智能体学习会话')
    expect(wrapper.text()).toContain('生成资源')
  })

  it('lets students log in without showing the teacher review module', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(existingKnowledgeBaseEnvelope())
      .mockResolvedValueOnce(emptyKnowledgeBaseDocumentsEnvelope())
      .mockResolvedValueOnce(profileEnvelope())
      .mockResolvedValueOnce(pathEnvelope())

    const wrapper = await mountApp('/login/student')

    expect(wrapper.get('[data-test="student-login-page"]').text()).toContain('学生登录')
    expect(wrapper.get('[data-test="switch-to-teacher-login"]').attributes('href')).toBe('/login/teacher')

    await wrapper.get('[data-test="student-login-submit"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('开始一轮智能体学习会话')
    expect(wrapper.find('[data-test="teacher-view"]').exists()).toBe(false)
    expect(wrapper.get('[data-test="admin-view"]').attributes('href')).toBe('/admin/operations')
    expect(wrapper.get('[data-test="admin-model-providers-view"]').attributes('href')).toBe('/admin/model-providers')
    expect(wrapper.get('[data-test="account-menu-trigger"]').text()).toContain('学生账户')
    expect(wrapper.text()).not.toContain('教师审核')
  })

  it('lets teachers log in and see every sidebar module', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(existingKnowledgeBaseEnvelope())
      .mockResolvedValueOnce(emptyKnowledgeBaseDocumentsEnvelope())
      .mockResolvedValueOnce(profileEnvelope())
      .mockResolvedValueOnce(pathEnvelope())

    const wrapper = await mountApp('/login/teacher')

    expect(wrapper.get('[data-test="teacher-login-page"]').text()).toContain('教师登录')
    expect(wrapper.get('[data-test="switch-to-student-login"]').attributes('href')).toBe('/login/student')

    await wrapper.get('[data-test="teacher-login-submit"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('开始一轮智能体学习会话')
    expect(wrapper.get('[data-test="teacher-view"]').attributes('href')).toBe('/teacher/reviews')
    expect(wrapper.get('[data-test="admin-view"]').attributes('href')).toBe('/admin/operations')
    expect(wrapper.get('[data-test="admin-model-providers-view"]').attributes('href')).toBe('/admin/model-providers')
    expect(wrapper.get('[data-test="account-menu-trigger"]').text()).toContain('教师账户')
  })

  it('loads existing knowledge-base documents from backend instead of static examples', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(existingKnowledgeBaseEnvelope())
      .mockResolvedValueOnce(knowledgeBaseDocumentsEnvelope())
      .mockResolvedValueOnce(profileEnvelope())
      .mockResolvedValueOnce(pathEnvelope())

    const wrapper = await mountApp()
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/knowledge-bases/kb_java_backend/documents',
      expect.objectContaining({ method: 'GET' }),
    )
    expect(wrapper.find('ul[aria-label="课程资料"]').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('database-course.md')
    expect(wrapper.text()).not.toContain('spring-boot-api-notes.pdf')
  })

  it('does not mark the documents workflow step complete when no documents are indexed', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(existingKnowledgeBaseEnvelope())
      .mockResolvedValueOnce(emptyKnowledgeBaseDocumentsEnvelope())
      .mockResolvedValueOnce(profileEnvelope())
      .mockResolvedValueOnce(pathEnvelope())

    const wrapper = await mountApp()
    await flushPromises()

    expect(wrapper.find('[data-test="workflow-documents"]').exists()).toBe(false)
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/knowledge-bases/kb_java_backend/documents',
      expect.objectContaining({ method: 'GET' }),
    )
  })

  it('prevents a second RAG stream while the first stream is still running', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(existingKnowledgeBaseEnvelope())
      .mockResolvedValueOnce(emptyKnowledgeBaseDocumentsEnvelope())
      .mockResolvedValueOnce(profileEnvelope())
      .mockResolvedValueOnce(pathEnvelope())
    vi.stubGlobal('EventSource', MockEventSource)

    const wrapper = await mountApp()
    await flushPromises()

    await wrapper.get('[data-test="rag-question-input"]').setValue('为什么 SQL JOIN 会产生重复行？')
    await wrapper.get('[data-test="ask-rag"]').trigger('click')
    await flushPromises()

    expect(MockEventSource.instances).toHaveLength(1)
    expect(wrapper.get('[data-test="ask-rag"]').attributes('disabled')).toBeDefined()

    await wrapper.get('[data-test="ask-rag"]').trigger('click')
    await flushPromises()

    expect(MockEventSource.instances).toHaveLength(1)
  })

  it('loads teacher review queue from backend review governance APIs', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock.mockResolvedValueOnce(
      apiEnvelope([
        {
          reviewId: 'rev_backend_1',
          resourceId: 'res_backend_1',
          generationTaskId: 'task_backend_1',
          status: 'PENDING_CRITIC',
          summary: 'Resource content awaits accuracy and fit review.',
          resourceTitle: 'Backend JOIN lecture',
          resourceType: 'LECTURE',
          resourceReviewStatus: 'PENDING_CRITIC',
        },
      ]),
    )

    const wrapper = await mountApp('/teacher/reviews')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/reviews/resources?status=PENDING_CRITIC',
      expect.objectContaining({ method: 'GET' }),
    )
    expect(wrapper.text()).toContain('Backend JOIN lecture')
    expect(wrapper.text()).toContain('Resource content awaits accuracy and fit review.')
  })

  it('shows teacher review details and sends custom revision feedback', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(
        apiEnvelope([
          {
            reviewId: 'rev_backend_1',
            resourceId: 'res_backend_1',
            generationTaskId: 'task_backend_1',
            status: 'PENDING_CRITIC',
            summary: 'Resource content awaits accuracy and fit review.',
            resourceTitle: 'Backend JOIN lecture',
            resourceType: 'LECTURE',
            resourceReviewStatus: 'PENDING_CRITIC',
          },
        ]),
      )
      .mockResolvedValueOnce(
        apiEnvelope({
          reviewId: 'rev_backend_1',
          resourceId: 'res_backend_1',
          generationTaskId: 'task_backend_1',
          status: 'REVISION_REQUESTED',
          summary: 'Citations need page numbers before release.',
          resourceTitle: 'Backend JOIN lecture',
          resourceType: 'LECTURE',
          resourceReviewStatus: 'REVISION_REQUESTED',
        }),
      )

    const wrapper = await mountApp('/teacher/reviews')
    await flushPromises()

    expect(wrapper.get('[data-test="review-detail"]').text()).toContain('Backend JOIN lecture')
    expect(wrapper.get('[data-test="review-detail"]').text()).toContain('task_backend_1')
    expect(wrapper.get('[data-test="review-detail"]').text()).toContain(
      'Resource content awaits accuracy and fit review.',
    )
    expect(
      wrapper.get('[data-test="teacher-review-workspace"]').element.compareDocumentPosition(
        wrapper.get('[data-test="review-detail"]').element,
      ),
    ).toBeTruthy()
    expect(
      wrapper
        .get('[data-test="review-detail"]')
        .element.compareDocumentPosition(wrapper.get('[data-test="teacher-evidence-checklist"]').element) &
        Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBe(Node.DOCUMENT_POSITION_FOLLOWING)

    await wrapper
      .get('[data-test="review-feedback-input"]')
      .setValue('Citations need page numbers before release.')
    await wrapper.get('[data-test="request-revision"]').trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/reviews/resources/rev_backend_1/decision',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          decision: 'REVISION_REQUESTED',
          summary: 'Citations need page numbers before release.',
        }),
      }),
    )
    expect(wrapper.text()).not.toContain('Resource content awaits accuracy and fit review.')
  })

  it('keeps review feedback scoped to the selected teacher review', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(
        apiEnvelope([
          {
            reviewId: 'rev_backend_1',
            resourceId: 'res_backend_1',
            generationTaskId: 'task_backend_1',
            status: 'PENDING_CRITIC',
            summary: 'First resource needs citation review.',
            resourceTitle: 'First JOIN lecture',
            resourceType: 'LECTURE',
            resourceReviewStatus: 'PENDING_CRITIC',
          },
          {
            reviewId: 'rev_backend_2',
            resourceId: 'res_backend_2',
            generationTaskId: 'task_backend_2',
            status: 'PENDING_CRITIC',
            summary: 'Second resource needs learner-fit review.',
            resourceTitle: 'Second JOIN exercise',
            resourceType: 'EXERCISE',
            resourceReviewStatus: 'PENDING_CRITIC',
          },
        ]),
      )
      .mockResolvedValueOnce(
        apiEnvelope({
          reviewId: 'rev_backend_2',
          resourceId: 'res_backend_2',
          generationTaskId: 'task_backend_2',
          status: 'APPROVED',
          summary: 'Second resource approved with its own feedback.',
          resourceTitle: 'Second JOIN exercise',
          resourceType: 'EXERCISE',
          resourceReviewStatus: 'APPROVED',
        }),
      )

    const wrapper = await mountApp('/teacher/reviews')
    await flushPromises()

    await wrapper
      .get('[data-test="review-feedback-input"]')
      .setValue('First resource feedback should not leak.')
    await wrapper.get('[data-test="select-review-rev_backend_2"]').trigger('click')
    await wrapper
      .get('[data-test="review-feedback-input"]')
      .setValue('Second resource approved with its own feedback.')
    await wrapper.get('[data-test="approve-selected-review"]').trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/reviews/resources/rev_backend_2/decision',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          decision: 'APPROVED',
          summary: 'Second resource approved with its own feedback.',
        }),
      }),
    )
    expect(
      fetchMock.mock.calls.some(([, init]) =>
        String(init?.body).includes('First resource feedback should not leak.'),
      ),
    ).toBe(false)
  })

  it('loads admin dependency health and analytics overview from the backend', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock.mockResolvedValueOnce(healthEnvelope()).mockResolvedValueOnce(analyticsEnvelope()).mockResolvedValueOnce(apiEnvelope([]))

    const wrapper = await mountApp('/admin/operations')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/health',
      expect.objectContaining({ method: 'GET' }),
    )
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/analytics/overview',
      expect.objectContaining({ method: 'GET' }),
    )
    expect(wrapper.text()).toContain('运行时健康')
    expect(wrapper.text()).toContain('application is running')
    expect(wrapper.text()).toContain('jdbc:h2:mem:health_test')
    expect(wrapper.text()).toContain('UNCONFIGURED')
    expect(wrapper.text()).toContain('Agent 任务')
    expect(wrapper.text()).toContain('18')
    expect(wrapper.text()).toContain('Token 用量')
    expect(wrapper.text()).toContain('200')
    expect(wrapper.text()).not.toContain('$0.42')
  })

  it('keeps admin copy aligned with available backend signals', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock.mockResolvedValueOnce(healthEnvelope()).mockResolvedValueOnce(analyticsEnvelope()).mockResolvedValueOnce(apiEnvelope([]))

    const wrapper = await mountApp('/admin/operations')
    await flushPromises()

    expect(wrapper.text()).toContain('可用信号')
    expect(wrapper.text()).toContain('依赖健康')
    expect(wrapper.text()).toContain('Token 活动')
    expect(wrapper.text()).toContain('审核 PENDING_CRITIC')
    expect(wrapper.text()).toContain('学习活动')
    expect(wrapper.text()).toContain('分析概览计数')
    expect(wrapper.text()).not.toContain('model cost')
    expect(wrapper.text()).not.toContain('trace coverage')
    expect(wrapper.text()).not.toContain('citation rate')
    expect(wrapper.text()).not.toContain('index health')
    expect(wrapper.text()).not.toContain('Closed-loop learning telemetry')
  })

  it('initializes student profile and learning path from backend learning APIs', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(existingKnowledgeBaseEnvelope())
      .mockResolvedValueOnce(emptyKnowledgeBaseDocumentsEnvelope())
      .mockResolvedValueOnce(
        apiEnvelope({
          profileDraft: {
            learnerId: 'stu_001',
            target: 'Build backend APIs from profile service',
            weakPoints: ['JOIN cardinality from profile service'],
            preferences: ['case studies', 'code labs'],
            dimensions: [
              {
                name: 'learning_goal',
                value: 'Backend API profile goal',
                confidence: 0.91,
                evidence: 'Profile service returned the goal.',
              },
              {
                name: 'knowledge_gap',
                value: 'JOIN cardinality',
                confidence: 0.88,
                evidence: 'Profile service returned the weak point.',
              },
            ],
            updatePolicy: 'LEARN_AS_YOU_GO',
          },
          followUpQuestions: ['Which API slice should come first?'],
          reasonSummary: 'Profile API inferred backend API goal and JOIN weakness.',
          traceId: 'trc_profile_backend',
        }),
      )
      .mockResolvedValueOnce(
        apiEnvelope({
          pathId: 'path_backend_init',
          learnerId: 'stu_001',
          goalId: 'goal_java_backend',
          reasonSummary: 'Knowledge DAG path from backend learning API.',
          traceId: 'trc_path_backend',
          nodes: [
            {
              nodeId: 'kp_backend_api',
              title: 'Backend API Slice',
              status: 'ACTIVE',
              mastery: 0.64,
              reasonSummary: 'Backend path planner selected this active node.',
            },
            {
              nodeId: 'kp_backend_join',
              title: 'JOIN Cardinality From Backend',
              status: 'LOCKED',
              mastery: 0.22,
              reasonSummary: 'Backend path planner locked this until prerequisites complete.',
            },
          ],
        }),
      )

    const wrapper = await mountApp()
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/profile/dialogue/extract',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          learnerId: 'stu_001',
          message:
            '我想掌握 Java 后端项目交付。SQL JOIN 诊断和带 Citation 的 RAG 服务是我的薄弱点；我更喜欢代码示例和项目实践。',
        }),
      }),
    )
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/learning-paths',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          learnerId: 'stu_001',
          goalId: 'goal_java_backend',
        }),
      }),
    )
    expect(wrapper.text()).toContain('Backend API Slice')
    expect(wrapper.text()).toContain('JOIN Cardinality From Backend')
  })

  it('renders profile follow-up questions and sends learner refinement to backend', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(existingKnowledgeBaseEnvelope())
      .mockResolvedValueOnce(emptyKnowledgeBaseDocumentsEnvelope())
      .mockResolvedValueOnce(profileEnvelope())
      .mockResolvedValueOnce(pathEnvelope())
      .mockResolvedValueOnce(
        apiEnvelope({
          profileDraft: {
            learnerId: 'stu_001',
            target: 'Prioritize API contracts before controllers',
            weakPoints: ['API boundary sequencing'],
            preferences: ['contract-first examples'],
            dimensions: [
              {
                name: 'learning_goal',
                value: 'API contract slice first',
                confidence: 0.93,
                evidence: 'Learner chose the follow-up and clarified sequencing.',
              },
            ],
            updatePolicy: 'LEARN_AS_YOU_GO',
          },
          followUpQuestions: ['Which controller should follow the contract slice?'],
          reasonSummary: 'Profile API refined the learner next step.',
          traceId: 'trc_profile_refined',
        }),
      )

    const wrapper = await mountApp()
    await flushPromises()

    expect(wrapper.text()).not.toContain('Which backend project do you want to build first?')
    expect(wrapper.find('[data-test="profile-follow-up-0"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="profile-prompt-input"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="refine-profile"]').exists()).toBe(false)
  })

  it('uploads course documents through backend knowledge-base document APIs', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(existingKnowledgeBaseEnvelope())
      .mockResolvedValueOnce(emptyKnowledgeBaseDocumentsEnvelope())
      .mockResolvedValueOnce(profileEnvelope())
      .mockResolvedValueOnce(pathEnvelope())
      .mockResolvedValueOnce(
        apiEnvelope({
          documentId: 'doc_backend_upload',
          indexTaskId: 'idx_backend_upload',
          status: 'PENDING',
        }),
      )
      .mockResolvedValueOnce(
        apiEnvelope({
          documentId: 'doc_backend_upload',
          kbId: 'kb_java_backend',
          name: 'joins-troubleshooting.md',
          parseStatus: 'READY',
          indexStatus: 'INDEXED',
          errorMessage: null,
          version: 1,
        }),
      )

    const wrapper = await mountApp()
    await flushPromises()

    const selectedFile = new File(['JOIN duplicates often come from one-to-many relationships.'], 'joins-troubleshooting.md', {
      type: 'text/markdown',
    })
    Object.defineProperty(wrapper.get('[data-test="document-file-input"]').element, 'files', {
      value: [selectedFile],
      configurable: true,
    })
    await wrapper.get('[data-test="document-file-input"]').trigger('change')
    await wrapper.get('[data-test="upload-document"]').trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/knowledge-bases/kb_java_backend/documents',
      expect.objectContaining({
        method: 'POST',
        body: expect.any(FormData),
      }),
    )
    const uploadCall = fetchMock.mock.calls.find(
      ([url, init]) =>
        url === 'http://localhost:8080/api/knowledge-bases/kb_java_backend/documents' &&
        init?.method === 'POST',
    )
    const uploadBody = uploadCall?.[1]?.body as FormData
    expect(uploadBody.get('courseId')).toBe('goal_java_backend')
    expect(uploadBody.get('chapterId')).toBe('kp_sql_join')
    expect(uploadBody.get('file')).toBe(selectedFile)
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/documents/doc_backend_upload',
      expect.objectContaining({ method: 'GET' }),
    )
  })

  it('uploads the file selected by the learner instead of a generated sample file', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(existingKnowledgeBaseEnvelope())
      .mockResolvedValueOnce(emptyKnowledgeBaseDocumentsEnvelope())
      .mockResolvedValueOnce(profileEnvelope())
      .mockResolvedValueOnce(pathEnvelope())
      .mockResolvedValueOnce(
        apiEnvelope({
          documentId: 'doc_selected_file',
          indexTaskId: 'idx_selected_file',
          status: 'PENDING',
        }),
      )
      .mockResolvedValueOnce(
        apiEnvelope({
          documentId: 'doc_selected_file',
          kbId: 'kb_java_backend',
          name: 'student-notes.md',
          parseStatus: 'READY',
          indexStatus: 'INDEXED',
          errorMessage: null,
          version: 1,
        }),
      )

    const wrapper = await mountApp()
    await flushPromises()

    const selectedFile = new File(['student-selected content'], 'student-notes.md', {
      type: 'text/markdown',
    })
    Object.defineProperty(wrapper.get('[data-test="document-file-input"]').element, 'files', {
      value: [selectedFile],
      configurable: true,
    })
    await wrapper.get('[data-test="document-file-input"]').trigger('change')
    await wrapper.get('[data-test="upload-document"]').trigger('click')
    await flushPromises()

    const uploadCall = fetchMock.mock.calls.find(
      ([url, init]) =>
        url === 'http://localhost:8080/api/knowledge-bases/kb_java_backend/documents' &&
        init?.method === 'POST',
    )
    const uploadBody = uploadCall?.[1]?.body as FormData
    expect(uploadBody.get('file')).toBe(selectedFile)
  })

  it('keeps failed document status visible instead of treating it as ready', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(existingKnowledgeBaseEnvelope())
      .mockResolvedValueOnce(
        apiEnvelope([
          {
            documentId: 'doc_backend_failed',
            kbId: 'kb_java_backend',
            name: 'broken-course.pdf',
            parseStatus: 'FAILED',
            indexStatus: 'FAILED',
            errorMessage: 'Parser could not extract text.',
            version: 1,
          },
        ]),
      )
      .mockResolvedValueOnce(profileEnvelope())
      .mockResolvedValueOnce(pathEnvelope())

    const wrapper = await mountApp()
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/knowledge-bases/kb_java_backend/documents',
      expect.objectContaining({ method: 'GET' }),
    )
    expect(wrapper.find('ul[aria-label="课程资料"]').exists()).toBe(false)
  })

  it('does not upload a generated sample when no learner file is selected', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(existingKnowledgeBaseEnvelope())
      .mockResolvedValueOnce(emptyKnowledgeBaseDocumentsEnvelope())
      .mockResolvedValueOnce(profileEnvelope())
      .mockResolvedValueOnce(pathEnvelope())

    const wrapper = await mountApp()
    await flushPromises()

    await wrapper.get('[data-test="upload-document"]').trigger('click')
    await flushPromises()

    expect(
      fetchMock.mock.calls.some(([url, init]) =>
        String(url).includes('/api/knowledge-bases/kb_java_backend/documents') &&
        init?.method === 'POST',
      ),
    ).toBe(false)
    expect(wrapper.text()).toContain('上传前请先选择文档。')
  })

  it('initializes the student knowledge base from backend APIs before upload', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(apiEnvelope([]))
      .mockResolvedValueOnce(createdKnowledgeBaseEnvelope())
      .mockResolvedValueOnce(emptyKnowledgeBaseDocumentsEnvelope())
      .mockResolvedValueOnce(profileEnvelope())
      .mockResolvedValueOnce(pathEnvelope())
      .mockResolvedValueOnce(
        apiEnvelope({
          documentId: 'doc_created_kb_upload',
          indexTaskId: 'idx_created_kb_upload',
          status: 'PENDING',
        }),
      )
      .mockResolvedValueOnce(
        apiEnvelope({
          documentId: 'doc_created_kb_upload',
          kbId: 'kb_backend_created',
          name: 'created-kb-notes.md',
          parseStatus: 'READY',
          indexStatus: 'INDEXED',
          errorMessage: null,
          version: 1,
        }),
      )

    const wrapper = await mountApp()
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/knowledge-bases',
      expect.objectContaining({ method: 'GET' }),
    )
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/knowledge-bases',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          name: 'Java backend course materials',
          description: 'Student workbench course materials',
          visibility: 'PRIVATE',
        }),
      }),
    )

    const selectedFile = new File(['created kb upload'], 'created-kb-notes.md', {
      type: 'text/markdown',
    })
    Object.defineProperty(wrapper.get('[data-test="document-file-input"]').element, 'files', {
      value: [selectedFile],
      configurable: true,
    })
    await wrapper.get('[data-test="document-file-input"]').trigger('change')
    await wrapper.get('[data-test="upload-document"]').trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/knowledge-bases/kb_backend_created/documents',
      expect.objectContaining({
        method: 'POST',
        body: expect.any(FormData),
      }),
    )
  })

  it('streams RAG chat status, tokens, and final citations over SSE', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(existingKnowledgeBaseEnvelope())
      .mockResolvedValueOnce(emptyKnowledgeBaseDocumentsEnvelope())
      .mockResolvedValueOnce(profileEnvelope())
      .mockResolvedValueOnce(pathEnvelope())
    vi.stubGlobal('EventSource', MockEventSource)

    const wrapper = await mountApp()
    await flushPromises()

    await wrapper.get('[data-test="rag-question-input"]').setValue('为什么 SQL JOIN 会产生重复行？')
    await wrapper.get('[data-test="ask-rag"]').trigger('click')
    await flushPromises()

    const stream = MockEventSource.instances[0]
    expect(stream.url).toContain('http://localhost:8080/api/chat/sessions/')
    expect(stream.url).toContain('/stream?')
    expect(stream.url).toContain('question=%E4%B8%BA%E4%BB%80%E4%B9%88+SQL+JOIN+%E4%BC%9A%E4%BA%A7%E7%94%9F%E9%87%8D%E5%A4%8D%E8%A1%8C%EF%BC%9F')
    expect(stream.url).toContain('kbIds=kb_java_backend')

    stream.emit('status', { stage: 'RETRIEVING' })
    await flushPromises()
    expect(wrapper.text()).toContain('检索中')

    stream.emit('token', { text: 'Streamed SQL JOIN answer from SSE.' })
    await flushPromises()
    expect(wrapper.text()).toContain('Streamed SQL JOIN answer from SSE.')

    stream.emit('done', {
      traceId: 'trc_sse_rag',
      sources: [
        {
          documentId: 'doc_sse',
          documentName: 'sse-course.md',
          pageNum: 7,
          sectionTitle: 'Join cardinality',
          excerpt: 'SSE citation explains one-to-many joins.',
          score: 0.91,
        },
      ],
    })
    await flushPromises()

    expect(wrapper.text()).toContain('完成')
    expect(wrapper.text()).toContain('trc_sse_rag')
    expect(wrapper.text()).toContain('sse-course.md')
    expect(wrapper.get('[data-test="new-chat-thought-stream"]').text()).toContain('检索课程资料')
    expect(wrapper.get('[data-test="new-chat-thought-stream"]').text()).toContain('生成回答')
    expect(stream.close).toHaveBeenCalled()
    expect(fetchMock).not.toHaveBeenCalledWith(
      'http://localhost:8080/api/rag/query',
      expect.anything(),
    )
  })

  it.each([
    { mode: 'production', prod: true },
    { mode: 'staging', prod: false },
  ])('uses POST AI QA stream in $mode without leaking question and kbIds through a URL', async ({ mode, prod }) => {
    vi.stubEnv('PROD', prod)
    vi.stubEnv('MODE', mode)
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(existingKnowledgeBaseEnvelope())
      .mockResolvedValueOnce(emptyKnowledgeBaseDocumentsEnvelope())
      .mockResolvedValueOnce(profileEnvelope())
      .mockResolvedValueOnce(pathEnvelope())
      .mockResolvedValueOnce(
        sseResponse([
          { event: 'status', data: { stage: 'INTENT_ROUTING' } },
          { event: 'status', data: { stage: 'VERIFYING' } },
          { event: 'token', data: { text: 'Production streamed QA answer without sensitive URL.' } },
          {
            event: 'done',
            data: {
              answer: 'Production streamed QA answer without sensitive URL.',
              answerMode: 'THINKING',
              reasoningEffort: 'medium',
              reasoningSummary: 'Safe summary.',
              sourceStatus: 'COURSE_GROUNDED',
              sourcePolicy: 'COURSE_RAG',
              traceId: 'trc_prod_stream_ai_qa',
              sources: [
                {
                  documentId: 'doc_prod_stream',
                  documentName: 'prod-ai-qa.md',
                  pageNum: 2,
                  sectionTitle: 'Secure QA transport',
                  excerpt: 'Production uses POST AI QA streaming to avoid query URL leakage.',
                  score: 0.9,
                },
              ],
              citations: [
                {
                  documentId: 'doc_prod_stream',
                  documentName: 'prod-ai-qa.md',
                  pageNum: 2,
                  sectionTitle: 'Secure QA transport',
                  excerpt: 'Production uses POST AI QA streaming to avoid query URL leakage.',
                  score: 0.9,
                },
              ],
              learnerFit: { summary: 'profile: backend learner', contextItems: 2, score: 0.82 },
              nextSteps: [{ title: '复盘 JOIN 基数', action: '打开练习' }],
              uncertainty: { level: 'LOW', reason: 'cited', factors: ['COURSE_RAG'] },
              qualityFlags: ['STRUCTURED_SCHEMA_V1', 'COURSE_GROUNDED'],
              requiresReview: false,
              verification: {
                verdict: 'PASS',
                gatePolicy: 'BASIC_QA_VERIFIER_V1',
                qualityFlags: ['QA_VERIFIED'],
                requiresReview: false,
                checks: [
                  { name: 'CITATION_CONSISTENCY', status: 'PASS', severity: 'INFO', message: 'Citation matched.' },
                ],
              },
              toolCalls: [
                { name: 'IntentRouter', status: 'SUCCESS', summary: 'mode=THINKING' },
                { name: 'AnswerVerifier', status: 'SUCCESS', summary: 'verdict=PASS' },
              ],
            },
          },
        ]),
      )
    vi.stubGlobal('EventSource', MockEventSource)

    const wrapper = await mountApp()
    await flushPromises()

    await wrapper.get('[data-test="rag-question-input"]').setValue('为什么 SQL JOIN 会产生重复行？')
    await wrapper.get('[data-test="ask-rag"]').trigger('click')
    await flushPromises()

    expect(MockEventSource.instances).toHaveLength(0)
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/ai/qa/stream',
      expect.objectContaining({
        method: 'POST',
        headers: expect.any(Headers),
        body: JSON.stringify({
          answerMode: 'THINKING',
          kbIds: ['kb_java_backend'],
          question: '为什么 SQL JOIN 会产生重复行？',
          courseId: 'goal_java_backend',
          topK: 5,
        }),
      }),
    )
    const streamCall = fetchMock.mock.calls.find(
      ([url]) => url === 'http://localhost:8080/api/ai/qa/stream',
    )
    expect(String(streamCall?.[0])).not.toContain('question=')
    expect(String(streamCall?.[0])).not.toContain('kbIds=')
    expect(wrapper.text()).toContain('Production streamed QA answer without sensitive URL.')
    expect(wrapper.text()).toContain('trc_prod_stream_ai_qa')
    expect(wrapper.text()).toContain('prod-ai-qa.md')
    expect(wrapper.get('[data-test="qa-quality-summary"]').text()).toContain('PASS')
    expect(wrapper.get('[data-test="qa-quality-summary"]').text()).toContain('BASIC_QA_VERIFIER_V1')
    expect(wrapper.get('[data-test="qa-quality-summary"]').text()).toContain('STRUCTURED_SCHEMA_V1')
  })

  it('does not retry the production AI QA POST stream through the legacy SSE fallback path', async () => {
    vi.stubEnv('PROD', true)
    vi.stubEnv('MODE', 'production')
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(existingKnowledgeBaseEnvelope())
      .mockResolvedValueOnce(emptyKnowledgeBaseDocumentsEnvelope())
      .mockResolvedValueOnce(profileEnvelope())
      .mockResolvedValueOnce(pathEnvelope())
      .mockResolvedValueOnce(apiErrorEnvelope('Production QA failed', 503))
    vi.stubGlobal('EventSource', MockEventSource)

    const wrapper = await mountApp()
    await flushPromises()

    await wrapper.get('[data-test="rag-question-input"]').setValue('为什么 SQL JOIN 会产生重复行？')
    await wrapper.get('[data-test="ask-rag"]').trigger('click')
    await flushPromises()

    const qaCalls = fetchMock.mock.calls.filter(([url]) => url === 'http://localhost:8080/api/ai/qa/stream')
    expect(MockEventSource.instances).toHaveLength(0)
    expect(qaCalls).toHaveLength(1)
    expect(wrapper.text()).toContain('Production QA failed')
  })

  it('falls back to REST RAG query when SSE streaming fails', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(existingKnowledgeBaseEnvelope())
      .mockResolvedValueOnce(emptyKnowledgeBaseDocumentsEnvelope())
      .mockResolvedValueOnce(profileEnvelope())
      .mockResolvedValueOnce(pathEnvelope())
      .mockResolvedValueOnce(
        apiEnvelope({
          answer: 'Fallback REST answer after SSE failed.',
          traceId: 'trc_rest_fallback',
          sources: [
            {
              documentId: 'doc_fallback',
              documentName: 'fallback-course.md',
              pageNum: 4,
              sectionTitle: 'Fallback retrieval',
              excerpt: 'REST fallback still returns grounded citations.',
              score: 0.89,
            },
          ],
        }),
      )
    vi.stubGlobal('EventSource', MockEventSource)

    const wrapper = await mountApp()
    await flushPromises()

    await wrapper.get('[data-test="rag-question-input"]').setValue('为什么 SQL JOIN 会产生重复行？')
    await wrapper.get('[data-test="ask-rag"]').trigger('click')
    await flushPromises()

    MockEventSource.instances[0].fail()
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/rag/query',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          kbIds: ['kb_java_backend'],
          question: '为什么 SQL JOIN 会产生重复行？',
          topK: 5,
        }),
      }),
    )
    expect(wrapper.text()).toContain('Fallback REST answer after SSE failed.')
    expect(wrapper.text()).toContain('trc_rest_fallback')
    expect(wrapper.text()).toContain('fallback-course.md')
  })

  it('surfaces malformed SSE events as RAG errors instead of hanging loading state', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(existingKnowledgeBaseEnvelope())
      .mockResolvedValueOnce(emptyKnowledgeBaseDocumentsEnvelope())
      .mockResolvedValueOnce(profileEnvelope())
      .mockResolvedValueOnce(pathEnvelope())
    vi.stubGlobal('EventSource', MockEventSource)

    const wrapper = await mountApp()
    await flushPromises()

    await wrapper.get('[data-test="rag-question-input"]').setValue('为什么 SQL JOIN 会产生重复行？')
    await wrapper.get('[data-test="ask-rag"]').trigger('click')
    await flushPromises()

    const stream = MockEventSource.instances[0]
    stream.emitRaw('status', '{not valid json')
    await flushPromises()

    expect(stream.close).toHaveBeenCalled()
    expect(wrapper.text()).toContain('SSE 事件数据无效')
    expect(wrapper.get('[data-test="ask-rag"]').text()).toContain('发送')
  })

  it('uses the learner-edited RAG question for backend retrieval', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(existingKnowledgeBaseEnvelope())
      .mockResolvedValueOnce(emptyKnowledgeBaseDocumentsEnvelope())
      .mockResolvedValueOnce(profileEnvelope())
      .mockResolvedValueOnce(pathEnvelope())
      .mockResolvedValueOnce(
        apiEnvelope({
          answer: 'Custom question answer from backend.',
          traceId: 'trc_custom_question',
          sources: [],
        }),
      )
    vi.stubGlobal('EventSource', MockEventSource)

    const wrapper = await mountApp()
    await flushPromises()

    await wrapper.get('[data-test="rag-question-input"]').setValue('How should I debug JOIN fanout?')
    await wrapper.get('[data-test="ask-rag"]').trigger('click')
    await flushPromises()

    MockEventSource.instances[0].fail()
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/rag/query',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          kbIds: ['kb_java_backend'],
          question: 'How should I debug JOIN fanout?',
          topK: 5,
        }),
      }),
    )
    expect(wrapper.text()).toContain('Custom question answer from backend.')
  })

  it('calls backend APIs for cited RAG, generated resources, and mastery update', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(existingKnowledgeBaseEnvelope())
      .mockResolvedValueOnce(emptyKnowledgeBaseDocumentsEnvelope())
      .mockResolvedValueOnce(profileEnvelope())
      .mockResolvedValueOnce(pathEnvelope())
      .mockResolvedValueOnce(
        apiEnvelope({
          answer: 'Grounded SQL JOIN answer from backend.',
          traceId: 'trc_backend_rag',
          sources: [
            {
              documentId: 'doc_backend',
              documentName: 'database-course.md',
              pageNum: 12,
              sectionTitle: 'Multi table joins',
              excerpt: 'Backend citation explains one-to-many row multiplication.',
              score: 0.94,
            },
          ],
        }),
      )
      .mockResolvedValueOnce(
        apiEnvelope({
          taskId: 'res_task_backend',
          agentTaskId: 'agent_task_backend',
          status: 'COMPLETED',
          reviewStatus: 'PENDING_CRITIC',
          progressPercent: 100,
          safetyStatus: 'PASS',
          traceId: 'trc_resource_backend',
          resources: [
            {
              resourceId: 'res_backend_1',
              type: 'LECTURE',
              modality: 'TEXT',
              title: 'Backend generated JOIN lecture',
              reviewStatus: 'PENDING_CRITIC',
              citationSummary: 'database-course.md p.12',
              markdownContent: 'Generated resource body',
              safetyStatus: 'PASS',
            },
          ],
        }),
      )
      .mockResolvedValueOnce(
        apiEnvelope({
          taskId: 'agent_task_backend',
          status: 'COMPLETED',
          traceId: 'trc_agent_backend',
          steps: [
            {
              stepId: 'step_backend_1',
              agentName: 'ResourceAgent',
              status: 'DONE',
              summary: 'Generated resource with citations.',
              latencyMs: 640,
              model: 'qwen-plus',
              promptVersion: 'resource-v1',
            },
          ],
        }),
      )
      .mockResolvedValueOnce(
        apiEnvelope({
          answerId: 'ans_backend',
          gradingResultId: 'grade_backend',
          score: 0.76,
          masteryUpdates: [
            {
              knowledgePointId: 'kp_sql_join',
              beforeMastery: 0.42,
              afterMastery: 0.76,
              reasonSummary: 'Backend grading detected improved JOIN reasoning.',
            },
          ],
          feedbackId: 'feedback_backend',
          replanTriggered: true,
          replanRecordId: 'path_replan_backend',
          wrongCauseAnalysis: 'Misread one-to-many cardinality.',
          resourcePushStrategy: 'Push targeted JOIN drill.',
          traceId: 'trc_assessment_backend',
        }),
      )

    const wrapper = await mountApp()
    await flushPromises()

    await wrapper.get('[data-test="rag-question-input"]').setValue('为什么 SQL JOIN 会产生重复行？')
    await wrapper.get('[data-test="ask-rag"]').trigger('click')
    await flushPromises()
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/rag/query',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          kbIds: ['kb_java_backend'],
          question: '为什么 SQL JOIN 会产生重复行？',
          topK: 5,
        }),
      }),
    )
    expect(wrapper.text()).toContain('Grounded SQL JOIN answer from backend.')
    expect(wrapper.text()).toContain('database-course.md')
    expect(wrapper.text()).toContain('trc_backend_rag')

    await wrapper.get('[data-test="generate-resources"]').trigger('click')
    await flushPromises()
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/resources/generation-tasks',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          learnerId: 'stu_001',
          goalId: 'goal_java_backend',
          pathNodeId: 'kp_sql_join',
          resourceTypes: ['LECTURE', 'MIND_MAP', 'EXERCISE', 'READING', 'CODE_LAB'],
        }),
      }),
    )
    expect(wrapper.text()).toContain('res_task_backend')
    expect(wrapper.text()).toContain('Backend generated JOIN lecture')
    expect(wrapper.text()).toContain('TEXT')
    expect(wrapper.text()).toContain('database-course.md p.12')
    expect(wrapper.text()).toContain('通过')

    await wrapper.get('[data-test="assessment-answer-input"]').setValue('当多条子表记录匹配同一条父表记录时，JOIN 会重复父表行；投影前应先聚合或约束子表行。')
    await wrapper.get('[data-test="submit-assessment"]').trigger('click')
    await flushPromises()
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/assessment/answers',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          learnerId: 'stu_001',
          questionId: 'q_sql_join_cardinality',
          answer: '当多条子表记录匹配同一条父表记录时，JOIN 会重复父表行；投影前应先聚合或约束子表行。',
        }),
      }),
    )
    expect(wrapper.text()).toContain('Misread one-to-many cardinality.')
    expect(wrapper.text()).toContain('path_replan_backend')
    expect(wrapper.text()).toContain('76%')
    expect(wrapper.find('[data-test="workflow-citations"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="workflow-resources"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="workflow-assessment"]').exists()).toBe(false)
  })

  it('groups student resources by review status and refreshes generation task state', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(existingKnowledgeBaseEnvelope())
      .mockResolvedValueOnce(emptyKnowledgeBaseDocumentsEnvelope())
      .mockResolvedValueOnce(profileEnvelope())
      .mockResolvedValueOnce(pathEnvelope())
      .mockResolvedValueOnce(
        apiEnvelope({
          taskId: 'res_task_review_states',
          agentTaskId: 'agent_task_review_states',
          status: 'COMPLETED',
          reviewStatus: 'PENDING_CRITIC',
          progressPercent: 100,
          safetyStatus: 'PASS',
          traceId: 'trc_review_states',
          resources: [
            {
              resourceId: 'res_pending',
              type: 'LECTURE',
              modality: 'TEXT',
              title: 'Pending review lecture',
              reviewStatus: 'PENDING_CRITIC',
              citationSummary: 'database-course.md p.12',
              markdownContent: 'Pending body',
              safetyStatus: 'PASS',
            },
            {
              resourceId: 'res_approved',
              type: 'EXERCISE',
              modality: 'TEXT',
              title: 'Approved drill',
              reviewStatus: 'APPROVED',
              citationSummary: 'database-course.md p.14',
              markdownContent: 'Approved body',
              safetyStatus: 'PASS',
            },
            {
              resourceId: 'res_revision',
              type: 'READING',
              modality: 'TEXT',
              title: 'Revision reading',
              reviewStatus: 'REVISION_REQUESTED',
              citationSummary: 'database-course.md p.16',
              markdownContent: 'Revision body',
              safetyStatus: 'PENDING',
            },
            {
              resourceId: 'res_safety_blocked',
              type: 'CODE_LAB',
              modality: 'TEXT',
              title: 'Safety blocked code lab',
              reviewStatus: 'SAFETY_BLOCKED',
              citationSummary: 'database-course.md p.18',
              markdownContent: 'Safety blocked body',
              safetyStatus: 'BLOCKED',
            },
          ],
        }),
      )
      .mockResolvedValueOnce(
        apiEnvelope({
          taskId: 'agent_task_review_states',
          status: 'COMPLETED',
          traceId: 'trc_agent_review_states',
          steps: [],
        }),
      )
      .mockResolvedValueOnce(
        apiEnvelope({
          taskId: 'res_task_review_states',
          agentTaskId: 'agent_task_review_states',
          status: 'COMPLETED',
          reviewStatus: 'APPROVED',
          progressPercent: 100,
          safetyStatus: 'PASS',
          traceId: 'trc_review_states_refreshed',
          resources: [
            {
              resourceId: 'res_pending',
              type: 'LECTURE',
              modality: 'TEXT',
              title: 'Pending review lecture now released',
              reviewStatus: 'APPROVED',
              citationSummary: 'database-course.md p.12',
              markdownContent: 'Released body',
              safetyStatus: 'PASS',
            },
          ],
        }),
      )

    const wrapper = await mountApp()
    await flushPromises()

    await wrapper.get('[data-test="generate-resources"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-test="resource-task-summary"]').text()).toContain('已完成')
    expect(wrapper.get('[data-test="resource-task-summary"]').text()).toContain('待审核')
    expect(wrapper.get('[data-test="resource-task-summary"]').text()).toContain('100%')
    expect(wrapper.get('[data-test="resource-task-summary"]').text()).toContain('通过')
    expect(wrapper.text()).toContain('Pending review lecture')
    expect(wrapper.text()).toContain('Approved drill')
    expect(wrapper.text()).toContain('Revision reading')
    expect(wrapper.text()).toContain('Safety blocked code lab')
    expect(wrapper.text()).toContain('Approved drill')
    expect(wrapper.text()).toContain('Safety blocked code lab')

    expect(wrapper.find('[data-test="refresh-resource-status"]').exists()).toBe(false)
  })

  it('uses default resource types when generating resources without visible type chips', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(existingKnowledgeBaseEnvelope())
      .mockResolvedValueOnce(emptyKnowledgeBaseDocumentsEnvelope())
      .mockResolvedValueOnce(profileEnvelope())
      .mockResolvedValueOnce(pathEnvelope())
      .mockResolvedValueOnce(
        apiEnvelope({
          taskId: 'res_task_selected_types',
          agentTaskId: 'agent_task_selected_types',
          status: 'COMPLETED',
          reviewStatus: 'PENDING_CRITIC',
          progressPercent: 100,
          safetyStatus: 'PASS',
          traceId: 'trc_selected_types',
          resources: [],
        }),
      )
      .mockResolvedValueOnce(
        apiEnvelope({
          taskId: 'agent_task_selected_types',
          status: 'COMPLETED',
          traceId: 'trc_agent_selected_types',
          steps: [],
        }),
      )

    const wrapper = await mountApp()
    await flushPromises()

    expect(wrapper.find('[data-test="composer-resource-type-MIND_MAP"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="composer-resource-type-READING"]').exists()).toBe(false)

    await wrapper.get('[data-test="generate-resources"]').trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/resources/generation-tasks',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          learnerId: 'stu_001',
          goalId: 'goal_java_backend',
          pathNodeId: 'kp_sql_join',
          resourceTypes: ['LECTURE', 'MIND_MAP', 'EXERCISE', 'READING', 'CODE_LAB'],
        }),
      }),
    )
  })

  it('handles an empty learning path without NaN mastery or resource generation crash', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(existingKnowledgeBaseEnvelope())
      .mockResolvedValueOnce(emptyKnowledgeBaseDocumentsEnvelope())
      .mockResolvedValueOnce(profileEnvelope())
      .mockResolvedValueOnce(
        apiEnvelope({
          pathId: 'path_empty',
          learnerId: 'stu_001',
          goalId: 'goal_java_backend',
          reasonSummary: 'No path nodes available yet.',
          traceId: 'trc_empty_path',
          nodes: [],
        }),
      )

    const wrapper = await mountApp()
    await flushPromises()

    expect(wrapper.text()).not.toContain('NaN%')

    await wrapper.get('[data-test="generate-resources"]').trigger('click')
    await flushPromises()

    expect(
      fetchMock.mock.calls.some(([url]) =>
        String(url).includes('/api/resources/generation-tasks'),
      ),
    ).toBe(false)
    expect(wrapper.text()).toContain('学习路径为空，请先创建路径再生成资源。')
  })

  it('uses the learner-edited assessment answer for grading', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(existingKnowledgeBaseEnvelope())
      .mockResolvedValueOnce(emptyKnowledgeBaseDocumentsEnvelope())
      .mockResolvedValueOnce(profileEnvelope())
      .mockResolvedValueOnce(pathEnvelope())
      .mockResolvedValueOnce(
        apiEnvelope({
          answerId: 'ans_custom',
          gradingResultId: 'grade_custom',
          score: 0.81,
          masteryUpdates: [
            {
              knowledgePointId: 'kp_sql_join',
              beforeMastery: 0.42,
              afterMastery: 0.81,
              reasonSummary: 'Custom answer explains JOIN fanout.',
            },
          ],
          feedbackId: 'feedback_custom',
          replanTriggered: true,
          replanRecordId: 'path_replan_custom',
          wrongCauseAnalysis: 'Custom answer is correct.',
          resourcePushStrategy: 'Advance to cited RAG practice.',
          traceId: 'trc_assessment_custom',
        }),
      )

    const wrapper = await mountApp()
    await flushPromises()

    await wrapper.get('[data-test="assessment-answer-input"]').setValue(
      'I would first group the child table by parent id, then join the aggregate.',
    )
    await wrapper.get('[data-test="submit-assessment"]').trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/assessment/answers',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          learnerId: 'stu_001',
          questionId: 'q_sql_join_cardinality',
          answer: 'I would first group the child table by parent id, then join the aggregate.',
        }),
      }),
    )
    expect(wrapper.text()).toContain('Custom answer is correct.')
    expect(wrapper.text()).toContain('81%')
  })
})

function healthEnvelope() {
  return apiEnvelope({
    application: {
      status: 'UP',
      detail: 'application is running',
      metadata: { environment: 'test', traceHeader: 'X-Trace-Id' },
    },
    database: {
      status: 'CONFIGURED',
      detail: 'database configuration loaded',
      metadata: { url: 'jdbc:h2:mem:health_test' },
    },
    redis: {
      status: 'CONFIGURED',
      detail: 'redis configuration loaded',
      metadata: { host: 'localhost', port: 6379 },
    },
    minio: {
      status: 'UNCONFIGURED',
      detail: 'minio configuration is incomplete',
      metadata: { endpoint: '', bucket: '' },
    },
    model: {
      status: 'UNCONFIGURED',
      detail: 'model provider disabled for local phase-1',
      metadata: { provider: 'none', chatModel: '', embeddingModel: '' },
    },
  })
}

function profileEnvelope() {
  return apiEnvelope({
    profileDraft: {
      learnerId: 'stu_001',
      target: 'Master Java backend project delivery',
      weakPoints: ['SQL JOIN diagnosis'],
      preferences: ['Code examples', 'project practice'],
      dimensions: [
        {
          name: 'learning_goal',
          value: 'Java backend delivery',
          confidence: 0.88,
          evidence: 'Backend profile extraction confirmed the goal.',
        },
        {
          name: 'knowledge_gap',
          value: 'SQL JOIN cardinality',
          confidence: 0.9,
          evidence: 'Backend profile extraction confirmed the weak point.',
        },
      ],
      updatePolicy: 'LEARN_AS_YOU_GO',
    },
    followUpQuestions: ['Which backend project do you want to build first?'],
    reasonSummary: 'Profile API inferred Java backend goal and SQL JOIN weakness.',
    traceId: 'trc_profile_default',
  })
}

function pathEnvelope() {
  return apiEnvelope({
    pathId: 'path_default',
    learnerId: 'stu_001',
    goalId: 'goal_java_backend',
    reasonSummary: 'Backend learning path keeps SQL JOIN diagnosis active.',
    traceId: 'trc_path_default',
    nodes: [
      {
        nodeId: 'kp_http_controller',
        title: 'HTTP Controllers',
        status: 'READY',
        mastery: 0.76,
        reasonSummary: 'Controller basics are stable enough for API workflow practice.',
      },
      {
        nodeId: 'kp_sql_join',
        title: 'SQL JOIN Diagnosis',
        status: 'ACTIVE',
        mastery: 0.42,
        reasonSummary: 'Weak point detected from profile extraction and prior answers.',
      },
      {
        nodeId: 'kp_cited_rag_service',
        title: 'Cited RAG Service',
        status: 'LOCKED',
        mastery: 0.18,
        reasonSummary: 'Unlock after retrieval, citation grounding, and JOIN correction.',
      },
    ],
  })
}

function analyticsEnvelope() {
  return apiEnvelope({
    agentTaskCount: 18,
    modelCallCount: 6,
    tokenUsage: {
      promptTokens: 120,
      completionTokens: 80,
      totalTokens: 200,
    },
    answerRecordCount: 4,
    wrongQuestionCount: 2,
    learningEventCount: 11,
    resourceReviewStatusCounts: {
      PENDING_CRITIC: 5,
      APPROVED: 3,
    },
  })
}
