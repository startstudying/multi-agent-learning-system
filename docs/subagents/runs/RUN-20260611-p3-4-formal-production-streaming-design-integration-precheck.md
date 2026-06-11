# RUN-20260611 P3-4 子任务：formal production streaming design integration precheck

## 1. 预审结论

本报告只读预审 P3-4 子任务“formal production streaming design”的集成风险。除本报告外未修改生产代码、测试代码或项目文档。

结论：建议按 **M - Standard Feature Slice** 处理。该任务横跨后端 streaming API 合同、前端 streaming transport、认证传递、安全日志与回归测试，但如果限定为“正式生产流式合同设计 + 最小实现边界规划”，可以不改数据库、不新增依赖、不引入完整 signed token 体系，避免升级为 L。

最小推荐方向：以 **POST streaming + `fetch` / `ReadableStream` + Bearer header** 作为生产流式主合同，保留现有 `GET .../stream` + `EventSource` 作为 dev/test 演示或明确 deprecated 兼容面。不要通过 query token、完整 `question` query string 或生产 `X-User-Id` 解决可用性。

## 2. 证据摘要

### 2.1 已完成前置任务

- `docs/planning/backend-architecture-todolist.md` 记录：`SSE production auth strategy` 已完成，Chat/Tutor SSE 在 production/staging 下 Bearer/JWT fail-closed；`frontend SSE sensitive URL cleanup` 已完成，production/staging 学生端 RAG 问答不再创建原生 `EventSource`，改走 `POST /api/rag/query`；正式 production streaming 另列后续 M 级设计。
- `docs/specs/SPEC-20260610-p3-4-sse-production-auth-strategy.md` 明确当前任务未实现新的 SSE token 协议，也未修改前端 production streaming 客户端。
- `docs/evidence/EVIDENCE-20260610-p3-4-frontend-sse-sensitive-url-cleanup.md` 明确当前生产/预发只是降级为非流式 POST 查询；若后续要求生产保持流式输出，需要 M 级设计后端 POST streaming / `fetch` + `ReadableStream` 合同，或短 TTL signed stream token 方案。
- `docs/acceptance/ACCEPT-20260610-p3-4-sse-production-auth-strategy.md` 明确后续应避免把完整 `question` 放入 SSE GET URL。

### 2.2 当前后端合同

- `backend/src/main/java/com/learningos/rag/api/ChatController.java`：
  - `POST /api/rag/query` 接收 `RagQueryRequest`，走 `CurrentUserService.currentUser()`，向 `RagQueryService` 传 explicit role facts。
  - `GET /api/chat/sessions/{sessionId}/stream?question=...&kbIds=...` 返回 `SseEmitter`，在 query string 中接收 `question` / `kbIds`，事件类型为 `status` / `token` / `done`。
- `backend/src/main/java/com/learningos/tutor/api/TutorController.java`：
  - `POST /api/tutor/ask` 非流式。
  - `GET /api/tutor/sessions/{sessionId}/stream?question=...&kbIds=...` 与 Chat SSE 结构类似。

### 2.3 当前前端合同

- `frontend/src/api/client.ts`：
  - `apiRequest()` 使用 `fetch`，当前固定写入 `X-User-Id`，未体现 production Bearer 注入策略。
  - `openSse(path)` 直接 `new EventSource(...)`，无法携带 `Authorization` header。
- `frontend/src/api/rag.ts`：
  - `queryRag()` 使用 `POST /api/rag/query`。
  - `streamChat()` 使用 `openSse()`，把 `question` 和 `kbIds` 放入 GET URL。
- `frontend/src/pages/student/StudentDashboard.vue`：
  - 已有 `usesSensitiveUrlSafeRagTransport`，production/staging 使用 REST fallback，dev/test 保留 SSE 演示。
  - 现有 UI 消费事件语义：`status.stage`、`token.text`、`done.sources`、`done.traceId`、`done.retrieval`。

### 2.4 项目规则

- `docs/architecture/ARCHITECTURE_BASELINE.md` 要求：前端不能直接调用 LLM API；API calls must use shared request wrapper；AI streaming must use SSE wrapper；RAG answers must include sources；permission checks happen in backend code。
- `docs/skills/project-specific/auth-context-boundary.md` 要求：production/staging 不信任 `X-User-Id`，Bearer 由 Spring Security Resource Server 处理，runtime service calls 必须从 `UserContext.roles()` 派生 explicit role facts。
- `docs/skills/project-specific/vue-ai-learning-ui.md` 要求：SSE via shared composable，不在组件里直接 new raw EventSource；Stop button 取消 in-flight SSE stream。

## 3. 集成边界

### 3.1 应保持不变的边界

- RAG 业务执行仍由 `RagQueryService` 负责，Controller 只负责 HTTP/streaming 协议适配和传递 `UserContext` role facts。
- 前端仍通过统一 API/stream wrapper 调用后端，不直接调用 LLM，不在前端保存 API key。
- RAG 响应必须保留 `answer`、`sources`、`traceId` 语义；流式 `done` 事件应携带完整引用与 trace 元数据。
- production/staging 身份只来自 Bearer/JWT，不回退到 `X-User-Id`。
- 不新增依赖，不新增数据库 schema，除非 PLAN 明确升级和 dependency review。

### 3.2 建议新增/稳定的合同边界

建议定义一个生产流式主入口，例如：

```http
POST /api/rag/query/stream
Accept: text/event-stream
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "kbIds": ["kb_001"],
  "question": "...",
  "topK": 5,
  "requestId": "..."
}
```

可选同步定义 Tutor 包装入口：

```http
POST /api/tutor/ask/stream
Accept: text/event-stream
Authorization: Bearer <jwt>
Content-Type: application/json
```

最小合同应复用现有 `RagQueryRequest` / `RagQueryResponse` 字段，不新增新的业务 DTO 语义；只新增 streaming event envelope：

```json
{"event":"status","data":{"stage":"RETRIEVING"}}
{"event":"token","data":{"text":"..."}}
{"event":"done","data":{"answer":"...","sources":[],"traceId":"...","retrieval":null,"latencyMs":123}}
{"event":"error","data":{"code":"RAG_STREAM_FAILED","message":"...","traceId":"..."}}
```

说明：如果后端仍一次性生成完整 `RagQueryResponse` 再发送单个 `token`，可以作为 minimum integration，但 SPEC 必须明确这是“transport streaming”，不是模型 token 级真实增量输出。

## 4. 建议 Size 结论

建议 Size = **M**。

依据：

- 影响两个相关模块：backend Chat/Tutor/RAG streaming API 与 frontend shared API/学生端 streaming UI。
- 改变 production 用户可见行为：从 production REST fallback 恢复为生产可用流式输出。
- 需要 REQ / SPEC / PLAN / TASK / CONTEXT，以及独立 Evidence / Acceptance。
- 不建议做 PRD，除非本任务同时改变产品工作流或引入新的用户级功能语义；当前更像安全/工程生产化合同收敛。

升级 L 的触发条件：

- 引入 signed stream token、一次性 token、撤销/审计/TTL 存储。
- 新增数据库表或持久化 stream session。
- 引入 WebSocket 或新依赖。
- 同时改 Orchestrator workflow streaming、模型 gateway token 级 streaming、资源生成 streaming。

## 5. M 级文档清单

后续主线程应创建：

- `docs/requirements/REQ-20260611-p3-4-formal-production-streaming-design.md`
- `docs/specs/SPEC-20260611-p3-4-formal-production-streaming-design.md`
- `docs/plans/PLAN-20260611-p3-4-formal-production-streaming-design.md`
- `docs/tasks/TASK-20260611-p3-4-formal-production-streaming-design.md`
- `docs/context/CONTEXT-20260611-p3-4-formal-production-streaming-design.md`
- `docs/evidence/EVIDENCE-20260611-p3-4-formal-production-streaming-design.md`
- `docs/acceptance/ACCEPT-20260611-p3-4-formal-production-streaming-design.md`

PRD：默认不需要。若产品要求“正式生产聊天体验必须流式、支持取消、断线恢复、会话历史”，则 PRD 需要补齐。

Changelog / Memory：完成后必须更新：

- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- 相关 domain memory，例如 `docs/memory/BACKEND_MEMORY.md`、`docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 6. Context Pack 文件边界

### 6.1 建议允许修改

后端：

- `backend/src/main/java/com/learningos/rag/api/ChatController.java`
- `backend/src/main/java/com/learningos/tutor/api/TutorController.java`
- `backend/src/test/java/com/learningos/rag/api/ChatControllerTest.java`
- `backend/src/test/java/com/learningos/tutor/api/TutorControllerTest.java`
- `backend/src/test/java/com/learningos/common/auth/SseProductionAuthStrategyTest.java`

前端：

- `frontend/src/api/client.ts`
- `frontend/src/api/rag.ts`
- `frontend/src/types/api.ts`
- `frontend/src/pages/student/StudentDashboard.vue`
- `frontend/src/App.spec.ts`

文档：

- 上述 REQ / SPEC / PLAN / TASK / CONTEXT / EVIDENCE / ACCEPT
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

### 6.2 建议只读参考

- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/harness/TEST_COMMANDS.md`
- `docs/specs/SPEC-20260610-p3-4-sse-production-auth-strategy.md`
- `docs/evidence/EVIDENCE-20260610-p3-4-frontend-sse-sensitive-url-cleanup.md`
- `docs/acceptance/ACCEPT-20260610-p3-4-sse-production-auth-strategy.md`
- `docs/skills/project-specific/auth-context-boundary.md`
- `docs/skills/project-specific/vue-ai-learning-ui.md`

### 6.3 建议禁止修改

- `backend/pom.xml`
- `frontend/package.json`
- `frontend/package-lock.json`
- `backend/src/main/resources/db/migration/**`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`，除非 SPEC 明确需要 service-level streaming abstraction
- `backend/src/main/java/com/learningos/common/auth/**`，除非只补充测试或明确发现 Bearer 上下文传递缺口
- 所有 `docs/security/DEPENDENCY-REVIEW-*.md`，因为本方案不应新增依赖

## 7. 冲突点

### 7.1 EventSource 与 Bearer header 冲突

证据：`frontend/src/api/client.ts` 的 `openSse()` 使用原生 `EventSource`；前置文档已确认 production Bearer-only 下不能自动携带 `Authorization` header。

建议：生产主方案不要继续依赖原生 `EventSource`。前端应通过 `fetch` + `ReadableStream` 设置 `Authorization` header。若项目尚无 token 获取机制，SPEC 必须先定义“沿用现有 shared request wrapper 的认证注入点”，不要临时把 token 放 query。

### 7.2 敏感参数在 GET URL 中

证据：`ChatController.stream()` / `TutorController.stream()` 从 query string 接收 `question` / `kbIds`；`streamChat()` 用 `URLSearchParams` 拼接这些参数。

建议：生产 streaming 使用 POST body，避免问题、KB 范围、可能的学习上下文进入浏览器历史、代理日志、访问日志。

### 7.3 当前“流式”不等于模型 token streaming

证据：后端当前先调用 `ragQueryService.query(...)` 得到完整 `RagQueryResponse`，再发送单个 `token` 事件。

建议：M 级最小切片可只做 production transport streaming，但 SPEC 必须命名清楚；不要承诺真实模型增量 token。真实模型 streaming 应另起 P3-3 / model gateway 子任务。

### 7.4 shared wrapper 规则与现有前端实现不完全一致

证据：架构基线要求 AI streaming 使用 shared wrapper；现有 `openSse()` 是 shared helper，但不是可携带 Bearer 的 production wrapper；项目技能建议 composable，而当前学生页直接消费 API 函数。

建议：最小修改可在 `frontend/src/api/client.ts` 增加 `openStream()` / `streamRequest()`；更完整但仍 M 的方案可新增 composable，但 Context Pack 必须列入对应文件。

### 7.5 Chat 与 Tutor 合同重复

证据：`ChatController` 和 `TutorController` 的 stream 方法事件序列高度重复。

建议：本 M 切片不做大重构。可以在 SPEC 中规定事件合同一致，先分别补测试；若抽公共 helper，必须控制在 Controller/API 层，不改变 `RagQueryService` 权限语义。

## 8. 测试建议

### 8.1 后端 focused tests

建议新增/扩展：

- production `POST /api/rag/query/stream` missing Bearer -> `401`，不启动 async，不调用 `RagQueryService`。
- production invalid Bearer -> `401`，不 fallback。
- production valid Bearer + spoofed `X-User-Id` -> 使用 JWT subject / roles。
- POST streaming 不接受 query `question` / `kbIds` 作为业务输入。
- stream event 顺序：`status` -> `token` -> `done`。
- service exception -> `error` event 或受控 async error，错误不包含 token、raw question、raw exception。
- `done.sources` / `traceId` 保持与 `POST /api/rag/query` 语义一致。

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ChatControllerTest,TutorControllerTest,SseProductionAuthStrategyTest test
```

### 8.2 前端 focused tests

建议新增/扩展：

- production/staging 点击 RAG 使用 `fetch` streaming，不创建 `EventSource`。
- streaming request 使用 POST body 包含 `question` / `kbIds` / `topK`，URL 不包含敏感参数。
- request headers 能携带认证头，且不把 token 放 URL。
- 能解析 `status` / `token` / `done` / `error` 事件。
- stop/cancel 能 abort in-flight stream，loading 状态清理。
- malformed stream event 进入错误状态，不挂起。
- dev/test legacy SSE 行为如需保留，原有 EventSource 测试继续通过。

命令：

```powershell
cd D:\多元agent\frontend
npm test -- --run src/App.spec.ts
npx vue-tsc -b --noEmit
npm run build
```

### 8.3 Adjacent / full verification

后端：

```powershell
cd D:\多元agent\backend
mvn test
```

前端：

```powershell
cd D:\多元agent\frontend
npm run build
```

Evidence 应记录 focused、adjacent/full 的实际输出；如果 full 无法运行，必须说明环境限制。

## 9. 验收组织

Acceptance 建议至少包含：

- production/staging 不再通过原生 `EventSource` 传生产流式认证。
- production streaming 支持 Bearer header，拒绝 missing/invalid Bearer。
- `question` / `kbIds` 不进入 streaming URL。
- `X-User-Id` spoofing 不影响 production 身份。
- RAG stream 最终输出包含 `sources` 与 `traceId`。
- 错误事件与日志不泄露 token、secret、raw exception、大段原始 question。
- dev/test 兼容行为已明确：保留、废弃或迁移，不能含糊。
- 未新增依赖、未改 DB schema；若新增依赖则必须有 `docs/security` dependency review。

## 10. 集成建议

1. 先写 M 级 REQ/SPEC/PLAN/TASK/CONTEXT，再实现。
2. 生产主合同优先选择 POST streaming + `fetch` / `ReadableStream`，不要用 query token 修补 EventSource。
3. 保持 `RagQueryService` 作为权限和 RAG 执行边界；Controller 只做 transport。
4. 将旧 GET SSE 合同标为 dev/test 或 deprecated，避免 production 前端继续依赖。
5. 若实现中发现需要 token 存储、stream session 表、真实模型 token streaming 或跨 Orchestrator streaming，立即停止并升级为 L 或拆新任务。
