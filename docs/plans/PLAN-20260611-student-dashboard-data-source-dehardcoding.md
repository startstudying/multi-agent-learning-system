# Student Dashboard Data Source Dehardcoding Implementation Plan

## 状态

Completed on 2026-06-11。

- Evidence: `docs/evidence/EVIDENCE-20260611-student-dashboard-data-source-dehardcoding.md`
- Acceptance: `docs/acceptance/ACCEPT-20260611-student-dashboard-data-source-dehardcoding.md`
- Verification: `frontend pnpm test -- --run` 34 passed; `frontend pnpm build` passed; fixed-ID static search returned no matches in production frontend scope.

> **For agentic workers:** REQUIRED SUB-SKILL: Use `subagent-driven-development` for parallel analysis only if this grows into frontend + backend API work; otherwise implement task-by-task in one Codex session. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将学生端从固定 `stu_001 / kb_java_backend / goal_java_backend / kp_sql_join / q_sql_join_cardinality` 的演示状态，改为由当前用户、课程、知识库、学习目标和后端返回数据驱动。

**Architecture:** 参考 `jc-rag-kb-front` 的分层方式：入口/布局保持薄，API 模块集中，页面按业务面板拆分，跨面板状态进入学生工作台 composable/store。第一阶段不改后端 API 合同；若发现缺少“当前学生上下文/学习目标列表/题目列表”等接口，先以显式空状态和禁用动作呈现，不再伪造业务数据。

**Tech Stack:** Vue 3 + TypeScript + Vite；现有 `fetch` API wrapper；Vue Router；不新增依赖。

---

## Skill Selection Report

## Task Type

前端重构 + 数据源去硬编码 + 学生端工作台行为修正。

## Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 该任务属于可见行为调整和前端结构改造，必须按项目 M 级流程执行。 |
| `vue3-component-design` | 需要拆分 `StudentDashboard.vue`，建立清晰的 Vue 组件和 composable 边界。 |
| `ai-streaming-ui` | 学生端 RAG / AI QA 仍包含 SSE 或流式问答路径，拆分时必须保留流式状态。 |
| `rag-project-review` | 学生端知识库、文档、RAG 引用、no-source/fallback 状态需要保持 RAG 规则。 |
| `agent-trace-design` | 资源生成、画像抽取、学习路径、RAG/AI QA traceId 和步骤展示不能丢失。 |
| `test-generator` | 现有 `App.spec.ts` 覆盖大量学生端流程，重构需要回归测试保护。 |

## Missing Skills

无。

## GitHub Research Needed

No。已有本地参考 `D:/迅雷下载/jc-ai-master (1).zip`，其中 `jc-rag-kb-front` 已足够支撑结构参考；不需要继续查 GitHub。

## New Project-Specific Skill To Create

暂不创建。若执行中沉淀出通用“学生端工作台去硬编码”模式，再补 `docs/skills/project-specific/vue-student-workbench-data-source.md`。

## Size Classification

Size: M。

Reason:

- 主要影响前端学生端一个大页面及其 API/类型/测试，属于一个 substantial module。
- 会改变可见行为：固定演示数据改为空状态、选择态、后端数据态。
- 不计划改变 REST API 合同、DTO、数据库 schema、依赖或后端权限逻辑。

Required Documents:

- `REQ-20260611-student-dashboard-data-source-dehardcoding.md`
- `SPEC-20260611-student-dashboard-data-source-dehardcoding.md`
- 本文件 `PLAN-20260611-student-dashboard-data-source-dehardcoding.md`
- `TASK-20260611-student-dashboard-data-source-dehardcoding.md`
- `CONTEXT-20260611-student-dashboard-data-source-dehardcoding.md`

Can Skip:

- PRD：本任务是既有学生端的工程化修正，不新增产品能力。
- Subagent reports：先按单一前端模块处理。

Upgrade Trigger:

- 若必须新增后端接口，例如 `GET /api/me`、`GET /api/learning-goals`、`GET /api/questions`，则升级为 L 或拆成“后端上下文 API”独立 M 任务。

## Subagent Decision

Use Subagents: No。

Reason: 第一阶段限定为前端学生端去硬编码和组件拆分，不改后端 API / DB / Agent 编排。

Parallelism Level: Single Codex。

Implementation Mode: Single task execution with checkpoints。

---

## 现状证据

- `frontend/src/pages/student/StudentDashboard.vue` 直接定义 `LEARNER_ID = 'stu_001'`、`GOAL_ID = 'goal_java_backend'`、`JOIN_NODE_ID = 'kp_sql_join'`、`JOIN_QUESTION_ID = 'q_sql_join_cardinality'`。
- 同一文件初始 `state` 中固定 `knowledgeBase.id = 'kb_java_backend'`、本地 `resources`、本地 `traceSteps`、本地 profile dimensions。
- `frontend/src/api/documents.ts` 已有 `listKnowledgeBases()`、`createKnowledgeBase()`、`listKnowledgeBaseDocuments()`、`uploadKnowledgeBaseDocument()`。
- `frontend/src/api/learning.ts` 已有 `extractProfile()`、`createLearningPath()`，但没有学习目标列表接口。
- `frontend/src/api/rag.ts`、`frontend/src/api/aiQa.ts` 已支持 RAG / AI QA。
- 后端已有 `GET /api/courses`、`GET /api/courses/{courseId}/knowledge-graph`、`GET /api/knowledge-bases`、`POST /api/learning-paths`、`POST /api/assessment/answers`。
- 后端没有从已读证据中确认“当前登录用户详情 API”和“学生学习目标列表 API”。

## 参考结构

`jc-rag-kb-front` 的可借鉴点：

- `App.tsx` 只做路由和鉴权壳。
- `layout/MainLayout.tsx` 管菜单和用户展示。
- `api/request.ts` 管统一 token/baseURL/错误。
- `api/index.ts` 按领域分组 API。
- `store/useAuthStore.ts`、`store/useChatStore.ts` 管跨页面状态。
- `pages/Chat.tsx`、`pages/KnowledgeBase.tsx`、`pages/KbDocuments.tsx`、`pages/Dashboard.tsx` 分开承载业务，而不是一个页面全包。

迁移到本项目时不切 React/AntD，不新增依赖，只吸收“分层和数据流”。

---

## File Structure

### Create

- `frontend/src/api/courses.ts`
  - 封装 `GET /api/courses`、`GET /api/courses/{courseId}/knowledge-graph`。

- `frontend/src/composables/useStudentWorkbench.ts`
  - 学生端统一数据源、选择态、加载态、错误态和动作编排。

- `frontend/src/pages/student/components/StudentChatPanel.vue`
  - 课程问答、AI QA mode、来源状态、composer。

- `frontend/src/pages/student/components/StudentKnowledgeBasePanel.vue`
  - 知识库选择、文档列表、上传。

- `frontend/src/pages/student/components/StudentLearningPathPanel.vue`
  - 学习路径节点、掌握度、当前活跃节点选择。

- `frontend/src/pages/student/components/StudentResourcePanel.vue`
  - 资源生成状态、资源类型选择、资源任务刷新。

- `frontend/src/pages/student/components/StudentAssessmentPanel.vue`
  - 作答输入、提交、反馈与 mastery 更新。

- `frontend/src/pages/student/components/StudentTracePanel.vue`
  - traceId 和 trace steps 展示。

### Modify

- `frontend/src/pages/student/StudentDashboard.vue`
  - 收敛为页面组装层，不再保存固定业务实体。

- `frontend/src/types/api.ts`
  - 增加 Course / KnowledgeGraph / student workbench view model 类型。

- `frontend/src/api/client.ts`
  - 增加 `getDefaultUserId()` 或 `currentFrontendUserId()` 小函数，临时读取 `VITE_DEV_USER_ID`；不把 `stu_001` 散落页面。

- `frontend/src/App.spec.ts`
  - 将固定 ID 断言改为“使用后端返回 ID/选择态”的断言。

### Do Not Modify

- 后端 production code。
- REST API path 和 DTO。
- 数据库 migration。
- 新依赖配置。
- 教师和管理员页面，除非测试挂载需要微调。

---

## Data Source Rules

1. `learnerId`
   - 第一阶段来源：`getDefaultUserId()`，由 `VITE_DEV_USER_ID` 或当前 token 后续上下文提供。
   - 页面内禁止再出现裸字符串 `'stu_001'`。
   - 后续任务可替换为 `GET /api/me`。

2. `knowledgeBaseId`
   - 来源：`listKnowledgeBases()` 返回的可访问 KB。
   - 默认选择第一条可访问 KB。
   - 若为空，显示空状态并允许创建 KB；RAG / upload / resource generation 相关按钮禁用或提示。
   - 页面内禁止再出现 `'kb_java_backend'`。

3. `courseId`
   - 来源：新增 `listCourses()`，使用 `GET /api/courses`。
   - 默认选择第一门可访问课程。
   - 上传文档时 `courseId` 只能来自当前选择课程。
   - 若无课程，上传仍可选择 KB，但 `courseId/chapterId` 不伪造。

4. `pathNodeId`
   - 来源：`createLearningPath()` 后端响应 nodes。
   - 默认活跃节点为第一个 `ACTIVE`，否则第一个非 `LOCKED`，否则空。
   - 页面内禁止再出现 `'kp_sql_join'`。

5. `goalId`
   - 第一阶段来源：用户输入或课程派生的可编辑字段，例如 `selectedGoalId`。
   - 默认值允许是空字符串，不再写死 `goal_java_backend`。
   - 若为空，创建学习路径、资源生成按钮禁用并提示“请先填写学习目标”。

6. `questionId`
   - 第一阶段不伪造题目 ID。
   - 若后端没有题目列表，测评提交面板进入禁用空状态，文案为“暂无可提交的课程题目；请先从后端题目列表接入”。
   - 禁止用 `q_sql_join_cardinality` 提交真实请求。

7. 初始 resources / traceSteps
   - 初始为空。
   - 只有后端动作返回后才展示 resource / trace。
   - 空状态展示“尚未生成资源 / 尚无 trace”。

---

## Task 1: 建立课程 API 和类型

**Files:**

- Create: `frontend/src/api/courses.ts`
- Modify: `frontend/src/types/api.ts`
- Test: `frontend/src/App.spec.ts`

- [ ] **Step 1: 写失败测试**

在 `App.spec.ts` 的学生端初始化测试中补充 mock：

```ts
fetchMock
  .mockResolvedValueOnce(apiEnvelope([
    {
      id: 'course_backend_java',
      title: 'Java 后端课程',
      description: '后端返回课程',
      ownerTeacherId: 'teacher_1',
      createdAt: '2026-06-11T00:00:00Z',
      updatedAt: '2026-06-11T00:00:00Z',
    },
  ]))
```

断言页面出现 `Java 后端课程`，且上传 FormData 的 `courseId` 来自 `course_backend_java`。

- [ ] **Step 2: 新增类型**

在 `types/api.ts` 增加：

```ts
export interface CourseResponse {
  id: string
  title: string
  description: string | null
  ownerTeacherId: string
  createdAt: string
  updatedAt: string
}

export interface KnowledgeGraphNodeResponse {
  id: string
  title: string
  type: string
  mastery?: number | null
}

export interface KnowledgeGraphResponse {
  courseId: string
  nodes: KnowledgeGraphNodeResponse[]
  edges: Array<{ from: string; to: string; type: string }>
}
```

- [ ] **Step 3: 新增 API 文件**

创建 `api/courses.ts`：

```ts
import { apiRequest } from './client'
import type { CourseResponse, KnowledgeGraphResponse } from '../types/api'

export function listCourses(): Promise<CourseResponse[]> {
  return apiRequest<CourseResponse[]>('/api/courses', { method: 'GET' })
}

export function fetchKnowledgeGraph(courseId: string): Promise<KnowledgeGraphResponse> {
  return apiRequest<KnowledgeGraphResponse>(`/api/courses/${courseId}/knowledge-graph`, {
    method: 'GET',
  })
}
```

- [ ] **Step 4: 运行测试**

Run: `cd frontend && pnpm test -- --run`

Expected: 初次可能仍失败，因为 `StudentDashboard.vue` 未消费课程 API；进入 Task 2。

---

## Task 2: 建立学生工作台数据 composable

**Files:**

- Create: `frontend/src/composables/useStudentWorkbench.ts`
- Modify: `frontend/src/api/client.ts`
- Modify: `frontend/src/pages/student/StudentDashboard.vue`
- Test: `frontend/src/App.spec.ts`

- [ ] **Step 1: 在 client 中集中当前用户 ID**

在 `api/client.ts` 导出：

```ts
export function getDefaultUserId(): string {
  return DEFAULT_USER_ID
}
```

页面和 composable 只能调用该函数，不直接写 `stu_001`。

- [ ] **Step 2: 创建 composable 初始结构**

`useStudentWorkbench.ts` 负责：

```ts
import { computed, onMounted, ref } from 'vue'
import { getDefaultUserId } from '../api/client'
import { listCourses } from '../api/courses'
import { listKnowledgeBaseDocuments, listKnowledgeBases } from '../api/documents'
import type {
  CourseResponse,
  DocumentRecord,
  DocumentStatusResponse,
  KnowledgeBaseResponse,
  PathNode,
  WorkbenchState,
} from '../types/api'

export function useStudentWorkbench() {
  const learnerId = ref(getDefaultUserId())
  const courses = ref<CourseResponse[]>([])
  const knowledgeBases = ref<KnowledgeBaseResponse[]>([])
  const selectedCourseId = ref('')
  const selectedKnowledgeBaseId = ref('')
  const selectedGoalId = ref('')
  const selectedPathNodeId = ref('')
  const selectedQuestionId = ref('')
  const documents = ref<DocumentRecord[]>([])
  const pathNodes = ref<PathNode[]>([])
  const isBootstrapping = ref(false)
  const errorMessage = ref('')

  const selectedKnowledgeBase = computed(() =>
    knowledgeBases.value.find((kb) => kb.id === selectedKnowledgeBaseId.value) ?? null,
  )

  async function bootstrap() {
    isBootstrapping.value = true
    errorMessage.value = ''
    try {
      const [courseList, kbList] = await Promise.all([listCourses(), listKnowledgeBases()])
      courses.value = courseList
      knowledgeBases.value = kbList
      selectedCourseId.value = courseList[0]?.id ?? ''
      selectedGoalId.value = courseList[0]?.id ?? ''
      selectedKnowledgeBaseId.value = kbList[0]?.id ?? ''
      if (selectedKnowledgeBaseId.value) {
        await refreshDocuments()
      }
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '学生工作台初始化失败'
    } finally {
      isBootstrapping.value = false
    }
  }

  async function refreshDocuments() {
    if (!selectedKnowledgeBaseId.value) {
      documents.value = []
      return
    }
    const rows = await listKnowledgeBaseDocuments(selectedKnowledgeBaseId.value)
    documents.value = rows.map(toDocumentRecord)
  }

  return {
    learnerId,
    courses,
    knowledgeBases,
    selectedCourseId,
    selectedKnowledgeBaseId,
    selectedKnowledgeBase,
    selectedGoalId,
    selectedPathNodeId,
    selectedQuestionId,
    documents,
    pathNodes,
    isBootstrapping,
    errorMessage,
    bootstrap,
    refreshDocuments,
  }
}

function toDocumentRecord(document: DocumentStatusResponse): DocumentRecord {
  return {
    id: document.documentId,
    name: document.name,
    type: document.name.includes('.') ? document.name.split('.').pop()?.toUpperCase() ?? 'DOC' : 'DOC',
    status: document.indexStatus === 'INDEXED' ? 'INDEXED' : document.indexStatus === 'FAILED' ? 'FAILED' : 'PENDING',
    version: document.version,
    errorMessage: document.errorMessage,
  }
}
```

- [ ] **Step 3: 接入 `StudentDashboard.vue`**

`StudentDashboard.vue` 顶部删除固定常量：

```ts
const LEARNER_ID = 'stu_001'
const GOAL_ID = 'goal_java_backend'
const JOIN_NODE_ID = 'kp_sql_join'
const JOIN_QUESTION_ID = 'q_sql_join_cardinality'
```

改为：

```ts
const workbench = useStudentWorkbench()

onMounted(() => {
  void workbench.bootstrap()
})
```

所有 API payload 使用 `workbench.learnerId.value`、`workbench.selectedKnowledgeBaseId.value`、`workbench.selectedGoalId.value`、`workbench.selectedPathNodeId.value`、`workbench.selectedQuestionId.value`。

- [ ] **Step 4: 将知识库状态从固定 state 迁出**

`state.value.knowledgeBase` 只作为显示兼容字段，来源改为 computed：

```ts
const activeKnowledgeBase = computed(() => workbench.selectedKnowledgeBase.value)
```

模板中展示 `activeKnowledgeBase?.name ?? '暂无知识库'`。

- [ ] **Step 5: 运行测试**

Run: `cd frontend && pnpm test -- --run`

Expected: 学生端初始化断言从固定 KB 改为后端返回 KB。

---

## Task 3: 去掉默认 path/resources/trace 假数据

**Files:**

- Modify: `frontend/src/pages/student/StudentDashboard.vue`
- Modify: `frontend/src/App.spec.ts`

- [ ] **Step 1: 写空状态断言**

在测试中断言初始页面：

```ts
expect(wrapper.text()).toContain('尚未生成资源')
expect(wrapper.text()).toContain('尚无 Agent Trace')
expect(wrapper.text()).not.toContain('res_local_001')
expect(wrapper.text()).not.toContain('trc_resource_local')
```

- [ ] **Step 2: 修改初始 state**

将 `resources` 和 `traceSteps` 初始值改为空数组：

```ts
resources: [],
traceSteps: [],
resourceTaskId: '',
resourceTaskStatus: 'IDLE',
resourceReviewStatus: 'IDLE',
resourceTraceId: '',
profileTraceId: '',
pathTraceId: '',
```

- [ ] **Step 3: 修改模板空状态**

资源区：

```vue
<p v-if="resources.length === 0" class="answer-text">尚未生成资源。请先选择学习目标和路径节点。</p>
```

Trace 区：

```vue
<p v-if="traceSteps.length === 0" class="answer-text">尚无 Agent Trace；完成画像、路径、问答或资源生成后会显示。</p>
```

- [ ] **Step 4: 运行测试**

Run: `cd frontend && pnpm test -- --run`

Expected: 固定资源/trace 不再出现；后端动作返回后仍可展示资源和 trace。

---

## Task 4: 用选择态驱动 RAG / AI QA / 上传 / 资源生成

**Files:**

- Modify: `frontend/src/pages/student/StudentDashboard.vue`
- Test: `frontend/src/App.spec.ts`

- [ ] **Step 1: RAG payload 改为选择态**

所有 RAG / AI QA payload：

```ts
const kbIds = workbench.selectedKnowledgeBaseId.value ? [workbench.selectedKnowledgeBaseId.value] : []
```

若 `kbIds.length === 0`，不请求后端，显示：

```ts
state.value.errorMessage = '请先选择知识库。'
```

- [ ] **Step 2: 上传 payload 改为选择态**

上传时：

```ts
await uploadKnowledgeBaseDocument(workbench.selectedKnowledgeBaseId.value, selectedDocumentFile.value, {
  courseId: workbench.selectedCourseId.value || undefined,
  chapterId: workbench.selectedPathNodeId.value || undefined,
})
```

如果没有 KB，显示“请先选择或创建知识库”。

- [ ] **Step 3: 资源生成 payload 改为选择态**

资源生成前校验：

```ts
if (!workbench.selectedGoalId.value || !workbench.selectedPathNodeId.value) {
  state.value.errorMessage = '请先生成学习路径并选择一个路径节点。'
  return
}
```

payload:

```ts
{
  learnerId: workbench.learnerId.value,
  goalId: workbench.selectedGoalId.value,
  pathNodeId: workbench.selectedPathNodeId.value,
  resourceTypes: selectedResourceTypes.value,
}
```

- [ ] **Step 4: 测评提交禁用无 questionId 状态**

若 `selectedQuestionId` 为空：

```ts
state.value.assessmentStatus = '暂无可提交题目'
state.value.errorMessage = '暂无可提交的课程题目；请先接入题目列表。'
return
```

不得向 `/api/assessment/answers` 发送固定 `q_sql_join_cardinality`。

- [ ] **Step 5: 回归测试**

断言：

```ts
expect(fetchMock).not.toHaveBeenCalledWith(
  'http://localhost:8080/api/assessment/answers',
  expect.anything(),
)
expect(wrapper.text()).toContain('暂无可提交的课程题目')
```

---

## Task 5: 组件拆分

**Files:**

- Create: `frontend/src/pages/student/components/StudentChatPanel.vue`
- Create: `frontend/src/pages/student/components/StudentKnowledgeBasePanel.vue`
- Create: `frontend/src/pages/student/components/StudentLearningPathPanel.vue`
- Create: `frontend/src/pages/student/components/StudentResourcePanel.vue`
- Create: `frontend/src/pages/student/components/StudentAssessmentPanel.vue`
- Create: `frontend/src/pages/student/components/StudentTracePanel.vue`
- Modify: `frontend/src/pages/student/StudentDashboard.vue`

- [ ] **Step 1: 先抽只展示组件**

每个组件先只接收 props / emits，不直接调用 API。

例：`StudentKnowledgeBasePanel.vue`

```vue
<script setup lang="ts">
import type { DocumentRecord, KnowledgeBaseResponse } from '../../../types/api'

defineProps<{
  knowledgeBases: KnowledgeBaseResponse[]
  selectedKnowledgeBaseId: string
  documents: DocumentRecord[]
  errorMessage: string
}>()

defineEmits<{
  (event: 'selectKnowledgeBase', id: string): void
  (event: 'uploadSelectedFile', file: File): void
}>()
</script>
```

- [ ] **Step 2: 保持原 data-test**

迁移模板时保留现有 `data-test`，例如：

- `student-primary-workspace`
- `student-chat-composer`
- `upload-document`
- `submit-assessment`
- `student-diagnostics`

- [ ] **Step 3: 页面变成组装层**

`StudentDashboard.vue` 最终职责：

- 调用 `useStudentWorkbench()`
- 组合各 panel
- 处理跨 panel action，如“画像抽取后创建学习路径”

- [ ] **Step 4: 运行测试**

Run: `cd frontend && pnpm test -- --run`

Expected: 既有学生端流程仍通过；失败时优先修 props/emit，而不是把业务逻辑塞回子组件。

---

## Task 6: Evidence / Acceptance / Memory

**Files:**

- Create: `docs/evidence/EVIDENCE-20260611-student-dashboard-data-source-dehardcoding.md`
- Create: `docs/acceptance/ACCEPT-20260611-student-dashboard-data-source-dehardcoding.md`
- Modify: `docs/changelog/CHANGELOG.md`
- Modify: `docs/memory/PROJECT_MEMORY.md`
- Modify: `docs/memory/FRONTEND_MEMORY.md`

- [ ] **Step 1: 运行验证**

Run:

```bash
cd frontend && pnpm test -- --run
cd frontend && pnpm build
```

Expected:

- 所有前端测试通过。
- 构建通过。

- [ ] **Step 2: Evidence 记录**

Evidence 必须写明：

- 已去除哪些固定 ID。
- 哪些数据源来自 API。
- 哪些后端缺口仍保留为空状态。
- 测试命令与结果。

- [ ] **Step 3: Acceptance 记录**

Acceptance 必须覆盖：

- 页面内无裸 `stu_001` / `kb_java_backend` / `goal_java_backend` / `kp_sql_join` / `q_sql_join_cardinality`。
- 初始资源和 trace 不再伪造。
- 无 KB、无课程、无路径、无题目时行为明确。
- RAG / AI QA / 上传 / 资源生成使用选择态。

---

## Verification Commands

```bash
cd frontend && pnpm test -- --run
cd frontend && pnpm build
```

可选静态搜索：

```bash
rg -n "stu_001|kb_java_backend|goal_java_backend|kp_sql_join|q_sql_join_cardinality|res_local|trc_resource_local" frontend/src/pages/student frontend/src/api frontend/src/types
```

预期：除测试 mock 或文档说明外，生产前端学生端不再出现这些固定演示 ID。

---

## Risks

- 当前缺少已确认的“当前用户详情 API”，第一阶段只能把 `learnerId` 集中到 `api/client.ts`，不能彻底消除 dev identity fallback。
- 当前缺少已确认的“学习目标列表 API”和“课程题目列表 API”，所以资源生成和测评提交需要进入选择/空状态，而不是继续伪造默认目标或题目。
- `StudentDashboard.vue` 当前测试覆盖很厚，组件拆分时要保留 `data-test`，否则会制造大量无意义测试 churn。
- 不要把权限判断放到前端；后端仍负责 RBAC 和对象范围校验。

## Done Definition

- M 级文档齐全。
- 学生端生产代码不再写死演示 learner/kb/goal/node/question/resource/trace ID。
- 页面从真实 API 初始化课程、知识库和文档。
- 无数据时显示明确空状态，关键动作禁用或提示。
- 前端测试与构建通过。
- Evidence、Acceptance、Changelog、Project Memory、Frontend Memory 更新完成。
