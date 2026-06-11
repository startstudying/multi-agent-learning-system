# TASK-20260610 P3-4 子任务：frontend SSE sensitive URL cleanup

## 目标

在不改后端 API/DTO/schema/dependency 的前提下，先完成最小前端安全收口：production/staging 环境不再使用原生 `EventSource` 拼接包含 `question` / `kbIds` 的 GET SSE URL，改走已有 `POST /api/rag/query` REST fallback。dev/test 环境保留当前 SSE 演示和回归测试。

## Task Type

Frontend security cleanup / RAG client hardening。

## Skill Selection Report

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 原始 P3-4 TODO 推进，必须走项目 S/M/L 工作流。 |
| `vue-edu-admin-frontend` | 修改 Vue 3 学生端 RAG 交互和 Vitest 回归。 |
| `test-driven-development` | 行为变更必须先写失败测试，再做最小实现。 |
| `security-review` | 清理敏感业务参数出现在生产 SSE URL 的风险。 |

Missing Skills: 无。

GitHub Research Needed: No。该切片使用现有前端 API 和测试模式，不新增协议或依赖。

New Project-Specific Skill To Create: 不需要。

## Size Classification

- Size: S - Small Slice / Fast Lane
- Reason: 前端-only 行为收口；不改后端合同、数据库、依赖、部署拓扑；生产环境从 SSE 降级为现有 POST 查询。
- Required Documents: 本 mini TASK，内嵌 Context Pack。
- Can Skip: 独立 PRD/REQ/SPEC/PLAN/CONTEXT、独立 Acceptance、独立 Retrospective。
- Upgrade Trigger: 若要求 production 仍保持流式输出 + Bearer + 不把敏感参数放 URL，则升级 M，并需要后端 POST streaming / fetch `ReadableStream` 合同。

## Subagent Decision

- Use Subagents: Yes
- Reason: 用户要求专家 subagent 并行开发；frontend expert 已完成只读分析并建议 S 方案。
- Parallelism Level: L1 Parallel Analysis
- Selected Subagents: Frontend SSE/security expert（只读分析）
- Implementation Mode: Single Codex implementation

## Context Pack

### Related Memory and Docs

- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/subagents/runs/` 中 frontend SSE 只读分析结论

### Allowed Files

- `frontend/src/pages/student/StudentDashboard.vue`
- `frontend/src/App.spec.ts`
- `docs/tasks/TASK-20260610-p3-4-frontend-sse-sensitive-url-cleanup.md`
- `docs/evidence/EVIDENCE-20260610-p3-4-frontend-sse-sensitive-url-cleanup.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

### Disallowed Files

- `backend/**`
- `frontend/package.json`
- `frontend/package-lock.json`
- `frontend/src/api/client.ts`
- `frontend/src/api/rag.ts`
- database migration files
- dependency/security review files

### Current Boundary

- 生产/预发：点击 RAG 直接调用 `queryRag(...)` 的 POST `/api/rag/query`，不创建 `EventSource`。
- dev/test：继续使用现有 SSE 流式演示和 fallback 测试。
- 不新增 token、signed URL、cookie/session、POST streaming 协议。

## Planned Test Commands

RED / focused:

```powershell
cd D:\多元agent\frontend
npm test -- --run src/App.spec.ts
```

Type check:

```powershell
cd D:\多元agent\frontend
npx vue-tsc -b --noEmit
```

Optional build:

```powershell
cd D:\多元agent\frontend
npm run build
```

## Acceptance Criteria

- [x] 新增测试先 RED：production 环境点击 RAG 时当前代码仍创建 `EventSource` 或未走 POST fallback。
- [x] production/staging 下点击 RAG 不创建 `EventSource`。
- [x] production/staging 下不会产生包含 `question` / `kbIds` 的 SSE URL。
- [x] production/staging 下调用 `POST /api/rag/query`，请求体包含 `kbIds`、`question`、`topK`。
- [x] production/staging REST 查询失败时不重复 POST。
- [x] dev/test 现有 SSE 流式测试继续通过。
- [x] Focused frontend tests、type check 与 build 通过。
