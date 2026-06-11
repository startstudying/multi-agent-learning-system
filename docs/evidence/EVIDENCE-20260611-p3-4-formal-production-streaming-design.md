# EVIDENCE-20260611 P3-4 子任务：formal production streaming design

## 1. 结论

本子任务已完成并通过验证。

production/staging 学生端 RAG 正式流式传输已从敏感 GET SSE URL 切到 `POST /api/rag/query/stream` + `fetch` / `ReadableStream`。`question` / `kbIds` 通过 JSON body 传输，不进入 stream URL；后端 streaming endpoint 复用 JWT-derived `userId` 和 `roles` facts，production/staging Bearer/JWT fail-closed 矩阵已覆盖。

## 2. 实现证据

### 2.1 后端 POST stream endpoint

- `D:\多元agent\backend\src\main\java\com\learningos\rag\api\ChatController.java:78-83`
  - 新增 `POST /api/rag/query/stream`
  - `consumes = application/json`
  - `produces = text/event-stream`
- `D:\多元agent\backend\src\main\java\com\learningos\rag\api\ChatController.java:83-107`
  - 从 `CurrentUserService` 读取当前 JWT 上下文。
  - 有 `requestId` 时调用 `ragQueryService.queryWithRequestId(...)`。
  - 无 `requestId` 时调用 `ragQueryService.query(...)`。
  - admin/teacher facts 来自 `UserContext.roles()`。
- `D:\多元agent\backend\src\main\java\com\learningos\rag\api\ChatController.java:108-118`
  - 输出 `status` / `token` / `done` 事件。
  - `done` 包含 `answer`、`sources`、`retrieval`、`traceId`、`latencyMs`。
- `D:\多元agent\backend\src\main\java\com\learningos\rag\api\ChatController.java:119-121`
  - stream-time exception 走 safe `error` event。

### 2.2 前端正式 production/staging stream path

- `D:\多元agent\frontend\src\api\client.ts:37-68`
  - 新增 `streamRequest(...)`，使用 `fetch`、`Accept: text/event-stream` 和 `ReadableStream.getReader()` 解析 SSE blocks。
- `D:\多元agent\frontend\src\api\client.ts:71-80`
  - shared header builder 可把 in-memory Bearer 写入 `Authorization` header。
- `D:\多元agent\frontend\src\api\rag.ts:31-44`
  - 新增 `streamRagQuery(...)`，固定调用 `POST /api/rag/query/stream`，业务输入放 body。
- `D:\多元agent\frontend\src\pages\student\StudentDashboard.vue:256-258`
  - production/staging 判定为敏感 URL 安全传输路径。
- `D:\多元agent\frontend\src\pages\student\StudentDashboard.vue:391-404`
  - production/staging 走 `streamRagQueryResponse()`。
  - dev/test 保留 legacy `streamRagResponse()`。
  - production/staging stream failure 不回退 legacy GET SSE。
- `D:\多元agent\frontend\src\pages\student\StudentDashboard.vue:457-484`
  - `question` / `kbIds` / `topK` 在 POST body。
  - 解析 `status` / `token` / `done` / `error`。

### 2.3 测试覆盖

- `D:\多元agent\backend\src\test\java\com\learningos\rag\api\ChatControllerTest.java:188-235`
  - 覆盖 `POST /api/rag/query/stream` 使用 request body。
  - 覆盖 `requestId` 进入 `queryWithRequestId(...)`。
  - 覆盖 `event:done`、`traceId` 等 stream 输出。
- `D:\多元agent\backend\src\test\java\com\learningos\common\auth\SseProductionAuthStrategyTest.java`
  - `postRagStreamInProductionRejectsMissingBearerBeforeStartingAsyncWork`
  - `postRagStreamInProductionRejectsInvalidBearerBeforeStartingAsyncWork`
  - `postRagStreamInProductionUsesBearerRolesAndIgnoresSpoofedUserIdHeader`
  - `postRagStreamInProductionDoesNotInferAdminFromBearerSubjectName`
- `D:\多元agent\frontend\src\App.spec.ts:973-1037`
  - production/staging 使用 `POST /api/rag/query/stream`。
  - 不创建 native `EventSource`。
  - URL 不含 `question=` / `kbIds=`。
  - `done` 后展示 answer、traceId、source。
- `D:\多元agent\frontend\src\App.spec.ts:1039-1061`
  - production POST stream failure 不回退 legacy SSE。
- `D:\多元agent\frontend\src\App.spec.ts:919-971`
  - dev/test legacy SSE 行为继续保留。

## 3. 专家复核证据

已使用专家 subagent 并行分析/复核：

- `D:\多元agent\docs\subagents\runs\RUN-20260611-p3-4-formal-production-streaming-design-backend.md`
- `D:\多元agent\docs\subagents\runs\RUN-20260611-p3-4-formal-production-streaming-design-security-quality.md`
- `D:\多元agent\docs\subagents\runs\RUN-20260611-p3-4-formal-production-streaming-design-integration-precheck.md`

最终复核补充结论：

- 后端专家：POST stream endpoint、`requestId` 分支、最小事件合同和 production Bearer fail-closed 测试成立；`citation` 独立事件和 GET Chat/Tutor error 合同统一可后续补强。
- 前端专家：production/staging 已使用 `fetch` / `ReadableStream`，不创建 native `EventSource`，stream URL 不携带 `question` / `kbIds`；Bearer token 来源接入属于外部登录态闭环，当前只提供 wrapper 能力。
- 安全专家：Bearer/JWT fail-closed、禁止 query token、禁止 `X-User-Id` production fallback、禁止 subject-name role inference 的关键矩阵成立；legacy GET SSE 仍需继续标注为 dev/test/demo-only。

## 4. 验证命令

### 4.1 Backend adjacent

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ChatControllerTest,TutorControllerTest,SseProductionAuthStrategyTest,SecurityFilterChainTest,SecurityJwtAuthenticationTest test
```

Result:

```text
Tests run: 36, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-11T02:58:35+08:00
```

### 4.2 Frontend focused

```powershell
cd D:\多元agent\frontend
npm test -- --run src/App.spec.ts
```

Result:

```text
Test Files  1 passed (1)
Tests  31 passed (31)
```

### 4.3 Frontend typecheck

```powershell
cd D:\多元agent\frontend
npx vue-tsc -b --noEmit
```

Result:

```text
Exit code: 0
```

### 4.4 Backend full

```powershell
cd D:\多元agent\backend
mvn test
```

Result:

```text
Tests run: 601, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
Finished at: 2026-06-11T03:01:28+08:00
```

### 4.5 Frontend build

```powershell
cd D:\多元agent\frontend
npm run build
```

Result:

```text
vue-tsc -b && vite build
✓ 1751 modules transformed.
✓ built in 1.32s
```

## 5. Architecture Drift Check After Implementation

| Check | Verdict | Evidence |
|---|---|---|
| Backend owns AI/RAG API calls | PASS | Frontend only calls backend `/api/rag/query/stream`。 |
| Controller only adapts protocol | PASS | `ChatController` delegates RAG logic to `RagQueryService`。 |
| Permission in backend service/security | PASS | production auth by Spring Security; RAG permission remains in service layer。 |
| No frontend LLM call | PASS | No LLM API call added to frontend。 |
| No query token / signed URL | PASS | Stream uses POST body and Bearer header capability。 |
| No DB/dependency/schema change | PASS | No migration/dependency files changed。 |
| RAG answer keeps sources/trace | PASS | `done` includes `sources` and `traceId`。 |

## 6. Residual Risks / Follow-up

- Legacy `GET /api/chat/sessions/{sessionId}/stream?question=...&kbIds=...` and Tutor GET stream still exist for dev/test/demo compatibility. Production client must continue using only `POST /api/rag/query/stream`。
- Frontend `setApiBearerToken(...)` is a runtime wrapper capability；真实登录态 token 注入点未在本任务内实现。
- `streamRequest` 当前测试断言 URL 不含 `question` / `kbIds`，后续可补充断言 URL 不含 `token` / `access_token` / `Authorization` 字样。
- `StudentDashboard.vue` 局部变量名 `useRestOnlyTransport` 已不准确，后续小清理可改名为 `useSafeStreamTransport`，本次不在验证后再引入代码改动。
- `POST /api/rag/query/stream` 当前是 transport streaming，`token.text` 可为完整答案；真实模型 token 增量 streaming 不在本任务范围。

## 7. Evidence Verdict

PASS。

本 M 级子任务满足 TASK / REQ / SPEC 中定义的最小 production streaming 目标；P3-4 父项仍保持打开，剩余重点是更完整业务权限矩阵抽样复核和其他教师端数据扩展。
