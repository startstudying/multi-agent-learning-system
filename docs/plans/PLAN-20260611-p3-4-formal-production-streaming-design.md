# PLAN-20260611 P3-4 子任务：formal production streaming design

## 1. Goal

完成学生端 RAG production/staging 正式流式传输：POST body 承载 `question` / `kbIds`，`fetch` stream 可携带 Bearer header，避免敏感 GET URL，并复用后端 RAG 权限检查。

## 2. Integrated Expert Result

| Expert | Result |
|---|---|
| Backend/Streaming Architect | 建议 M 级；复用 `RagQueryService`，新增/收敛 streaming API 合同；不要重做认证。 |
| Frontend Streaming Expert | production/staging 应使用 `fetch` + `ReadableStream`；不要用 native `EventSource`；修改范围限于 shared client、RAG API、学生页和测试。 |
| Security & Quality | 条件通过；必须禁止 query token / sensitive URL / header fallback；missing/invalid Bearer 不得启动 async work。 |
| Integration Reviewer | 建议 M 级；POST streaming + fetch stream 是最小集成路径；若引入 token/session/DB/WebSocket 则升级。 |

## 3. Implementation Strategy

TDD 顺序：

1. 后端 RED：新增 `POST /api/rag/query/stream` 测试，验证 endpoint 不存在或未流式。
2. 后端 GREEN：在 `ChatController` 加 POST stream endpoint。
3. 后端安全 RED/GREEN：production missing/invalid Bearer 对 POST stream 401 且 no service call；valid Bearer 使用 JWT subject/roles。
4. 前端 RED：production/staging 点击 RAG 应调用 `POST /api/rag/query/stream`、不创建 `EventSource`、URL 不含敏感参数。
5. 前端 GREEN：新增 shared stream wrapper 和 `streamRagQuery`，学生页 production/staging 改用 stream。
6. 验证 focused、adjacent、build/full。

## 4. Allowed Files

Backend:

- `backend/src/main/java/com/learningos/rag/api/ChatController.java`
- `backend/src/test/java/com/learningos/rag/api/ChatControllerTest.java`
- `backend/src/test/java/com/learningos/common/auth/SseProductionAuthStrategyTest.java`

Frontend:

- `frontend/src/api/client.ts`
- `frontend/src/api/rag.ts`
- `frontend/src/pages/student/StudentDashboard.vue`
- `frontend/src/App.spec.ts`

Docs:

- `docs/requirements/REQ-20260611-p3-4-formal-production-streaming-design.md`
- `docs/specs/SPEC-20260611-p3-4-formal-production-streaming-design.md`
- `docs/plans/PLAN-20260611-p3-4-formal-production-streaming-design.md`
- `docs/tasks/TASK-20260611-p3-4-formal-production-streaming-design.md`
- `docs/context/CONTEXT-20260611-p3-4-formal-production-streaming-design.md`
- `docs/evidence/EVIDENCE-20260611-p3-4-formal-production-streaming-design.md`
- `docs/acceptance/ACCEPT-20260611-p3-4-formal-production-streaming-design.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 5. Disallowed Files

- `backend/pom.xml`
- `frontend/package.json`
- `frontend/package-lock.json`
- `backend/src/main/resources/db/migration/**`
- `backend/src/main/java/com/learningos/common/auth/**`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
- `backend/src/main/java/com/learningos/tutor/api/TutorController.java`
- VectorDB/parser/OCR/model gateway files

If implementation needs any disallowed file, stop and reclassify.

## 6. Test Plan

Focused backend:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ChatControllerTest,SseProductionAuthStrategyTest test
```

Adjacent backend:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ChatControllerTest,TutorControllerTest,SseProductionAuthStrategyTest,SecurityFilterChainTest,SecurityJwtAuthenticationTest test
```

Full backend:

```powershell
cd D:\多元agent\backend
mvn test
```

Focused frontend:

```powershell
cd D:\多元agent\frontend
npm test -- --run src/App.spec.ts
```

Frontend type/build:

```powershell
cd D:\多元agent\frontend
npx vue-tsc -b --noEmit
npm run build
```

## 7. Risks and Controls

| Risk | Control |
|---|---|
| 把 token 放 URL | 只采用 POST fetch stream；测试断言 URL 不含 `question` / `kbIds`。 |
| production 回退 `X-User-Id` | 后端 production tests 覆盖 missing/invalid Bearer no service call。 |
| 前端绕过 shared client | 在 `client.ts` 提供 shared stream helper。 |
| 误称真实 token streaming | 文档和 evidence 明确是 transport streaming。 |
| stream error 泄露 raw exception | 后端 `error` event 使用固定安全消息。 |

## 8. Architecture Drift Check Plan

实现后复查：

- Controller 是否只做协议适配。
- 前端是否仍只调用后端 API。
- RAG sources/traceId 是否保持。
- 无新增依赖/DB。
- 无敏感 URL/token 文档或日志输出。
