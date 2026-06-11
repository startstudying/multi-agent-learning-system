# EVIDENCE-20260610-p3-4-frontend-sse-sensitive-url-cleanup

## 1. 任务

P3-4 子任务：frontend SSE sensitive URL cleanup

## 2. 变更摘要

完成最小前端安全收口：production/staging 环境下，学生端 RAG 问答不再使用原生 `EventSource` 拼接包含 `question` / `kbIds` 的 GET SSE URL，而是直接复用已有 `POST /api/rag/query`。

实现点：

- `StudentDashboard.vue` 增加 `usesSensitiveUrlSafeRagTransport`，当 `import.meta.env.PROD` 或 `import.meta.env.MODE === 'staging'` 时启用。
- production/staging 点击 RAG 直接调用 `queryRagRest()`，不进入 `streamRagResponse()`，因此不会创建 `EventSource`。
- production/staging POST 查询失败时直接进入错误态，不再通过 SSE fallback 路径重复调用一次 POST。
- dev/test 保留现有 SSE 流式演示、SSE 失败 fallback、malformed SSE error 语义。
- 该切片是 S 方案：生产降级为 POST fallback，不是正式 POST streaming。

## 3. 修改文件

- `frontend/src/pages/student/StudentDashboard.vue`
- `frontend/src/App.spec.ts`
- `docs/tasks/TASK-20260610-p3-4-frontend-sse-sensitive-url-cleanup.md`
- `docs/evidence/EVIDENCE-20260610-p3-4-frontend-sse-sensitive-url-cleanup.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 4. Verification

### RED 1：production 仍创建 EventSource

```powershell
cd D:\多元agent\frontend
npm test -- --run src/App.spec.ts
```

结果：

- `Tests: 1 failed | 28 passed`
- 失败点：production 模式测试期望 `MockEventSource.instances` 长度为 0，当前代码实际创建了 1 个 `EventSource`。

### RED 2：production REST 失败会重复 POST

```powershell
cd D:\多元agent\frontend
npm test -- --run src/App.spec.ts
```

结果：

- `Tests: 1 failed | 30 passed`
- 失败点：production REST 失败后，`POST /api/rag/query` 被调用 2 次；期望只调用 1 次并直接进入错误态。

### Focused frontend

```powershell
cd D:\多元agent\frontend
npm test -- --run src/App.spec.ts
```

结果：

- `Test Files 1 passed`
- `Tests 31 passed`

### Type check

```powershell
cd D:\多元agent\frontend
npx vue-tsc -b --noEmit
```

结果：

- 通过，无错误输出。

### Build

```powershell
cd D:\多元agent\frontend
npm run build
```

结果：

- `vue-tsc -b && vite build`
- `✓ built`
- `dist/index.html`
- `dist/assets/index-*.css`
- `dist/assets/index-*.js`

## 5. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 未改后端。 |
| Frontend rules | PASS | 未直接调用 LLM API，未存储 API key；production/staging 使用现有 API wrapper `queryRag` / `apiRequest`。 |
| Agent / RAG rules | PASS | RAG 仍通过后端 `/api/rag/query`，引用来源仍由后端返回。 |
| Security | PASS | production/staging 不再把 `question` / `kbIds` 放入 SSE GET URL。未新增依赖或 secrets。 |
| API / Database | PASS | 未改 API contract 或 DB schema。 |

## 6. Acceptance

| Criteria | Verdict |
|---|---|
| 新增测试先 RED，证明 production 当前会创建 EventSource | PASS |
| production 下点击 RAG 不创建 `EventSource` | PASS |
| staging 下点击 RAG 不创建 `EventSource` | PASS |
| production/staging 下不会产生包含 `question` / `kbIds` 的 SSE URL | PASS |
| production/staging 下调用 `POST /api/rag/query` 且请求体包含 `kbIds`、`question`、`topK` | PASS |
| production/staging REST 查询失败时不重复 POST | PASS |
| dev/test 现有 SSE 流式、fallback、malformed event 测试继续通过 | PASS |
| frontend focused tests、type check 和 build 通过 | PASS |

最终结论：PASS。P3-4 的 frontend sensitive URL cleanup 最小切片完成。

## 7. Accepted Limitations / Follow-up

- 本切片接受 production/staging 暂时降级为非流式 POST 查询。
- 若后续要求生产仍保持流式输出，并同时支持 Bearer header 且不泄露 query URL，需要升级 M：设计后端 POST streaming / `fetch` + `ReadableStream` 合同，或短 TTL signed stream token 方案。
