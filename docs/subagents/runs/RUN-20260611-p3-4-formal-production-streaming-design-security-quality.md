# RUN-20260611 P3-4 formal production streaming design Security & Quality Report

## 任务与边界

本报告只读分析 P3-4 子任务 `formal production streaming design` 的安全与质量边界。允许落盘的唯一文件为本报告；未修改生产代码，未启动 `node_repl`，未运行测试。

结论：该任务如果进入实现，应按 M 级设计切片处理。原因是它会同时涉及前端生产流式客户端、后端 streaming API 合同、Bearer/JWT 认证边界、RAG 权限复核、日志/trace 安全、幂等与测试矩阵。它不是单一测试补丁，也不应作为 S 级直接编码。

## 现有证据

- `docs/changelog/CHANGELOG.md:173-180`：`SSE production auth strategy` 已完成后端 production/staging Chat/Tutor SSE Bearer/JWT fail-closed 回归；无 Bearer、invalid Bearer、staging header-only 均拒绝，valid Bearer 使用 JWT subject/roles 并忽略 spoofed `X-User-Id`。
- `docs/changelog/CHANGELOG.md:66-72`：`frontend SSE sensitive URL cleanup` 已完成 production/staging 前端降级到 `POST /api/rag/query`，避免原生 `EventSource` 在 URL 中携带 `question` / `kbIds`；真正生产流式仍为后续 M slice。
- `docs/specs/SPEC-20260610-p3-4-sse-production-auth-strategy.md:9-12`：production-like 受保护请求要求 authenticated `JwtAuthenticationToken`；`EventSource` 不能携带 `Authorization` header。
- `docs/evidence/EVIDENCE-20260610-p3-4-sse-production-auth-strategy.md:103-106`：后续优先方案是 `fetch` / `ReadableStream` + Bearer；若继续原生 `EventSource`，需短 TTL、一次性、绑定 user/session/purpose 的 stream token，并处理日志脱敏；当前 SSE GET query 仍有敏感 URL 风险。
- `backend/src/main/java/com/learningos/common/auth/SecurityConfig.java:43-66`：CSRF 当前 disabled；production/staging 通过 Spring Security authorization manager 要求 `JwtAuthenticationToken`。
- `backend/src/main/java/com/learningos/rag/api/ChatController.java:78-96` 与 `backend/src/main/java/com/learningos/tutor/api/TutorController.java:52-70`：现有 SSE 入口是 GET + query params `question` / `kbIds`，内部调用 `RagQueryService` 并传 explicit role facts。
- `frontend/src/api/client.ts:24-25` 与 `frontend/src/api/rag.ts:17-19`：现有 `openSse` 基于原生 `EventSource`，`streamChat` 把 `question` / `kbIds` 拼到 URL。
- `frontend/src/pages/student/StudentDashboard.vue:257`、`docs/evidence/EVIDENCE-20260610-p3-4-frontend-sse-sensitive-url-cleanup.md:13-15`：production/staging 目前不创建 `EventSource`，改走 REST fallback。
- `backend/src/main/java/com/learningos/common/trace/StructuredRequestLoggingFilter.java:69-75`：结构化 HTTP 日志只记录 `traceId/userId/route/status/latencyMs/errorCode`；但若新 streaming API 使用 query URL，`safeRoute` fallback 包含 `request.getRequestURI()`，不含 query string，应用日志相对安全，代理/浏览器历史仍会记录完整 URL。
- `docs/architecture/ARCHITECTURE_BASELINE.md:38-42`：权限不能靠 Prompt，敏感数据不能进入 Prompt，API key 不能提交。

## 安全边界

1. 认证边界必须 fail-closed：
   - production/staging streaming 请求必须要求 Bearer JWT。
   - 无 Bearer、invalid Bearer、过期 token、issuer/audience 错误、缺少生产 JWT 配置均必须在启动 async/stream work 前失败。
   - 不得回退到 `X-User-Id`、`dev_user`、anonymous、cookie-only、query token 默认认证。

2. 授权边界必须 service-layer 复核：
   - stream 创建和实际 RAG 执行都必须使用 JWT subject 和 `UserContext.roles()` 推导出的 explicit admin/teacher facts。
   - `RagQueryService` 必须继续执行 KB permission filtering，mixed allowed + forbidden `kbIds` 不得部分放行。
   - replay / reconnect / resume 不得绕过 KB、course、learner scope。

3. 敏感数据边界：
   - `question`、`kbIds`、raw prompt、answer token、JWT、stream token、request body 不得出现在 URL、query string、browser history、access log、trace summary、metrics tag、error message。
   - Agent Trace / model logs / source citation 只能存必要业务证据，不得存 Authorization header、raw JWT、短期 stream token。

4. CSRF/CORS 边界：
   - 若使用 `Authorization: Bearer` header + same-origin API wrapper，CSRF 风险低于 cookie auth，但 CORS 必须限制 allowed origins，不得 `*` + credentials。
   - 若引入 cookie 或 stream token bootstrap，则必须重新评估 CSRF，并使用 SameSite、Origin 校验或 CSRF token。

5. 幂等与副作用边界：
   - stream 创建必须有 `requestId` 或等价 idempotency key。
   - 同 user + same `requestId` + same payload replay/resume 应返回同一 stream/job/query 结果或可恢复状态。
   - same `requestId` + different payload 必须 `409 CONFLICT`，不得产生第二份 query log/citation/model/token side effects。
   - 权限失败必须无 RAG query log、source citation、model call、token usage、AgentTask/Trace 伪成功证据；如保留失败 trace，必须是安全失败证据且不含敏感输入。

## 必须禁止的协议形态

- 禁止 production/staging 使用原生 `EventSource` 直接请求：
  - `GET /api/chat/sessions/{sessionId}/stream?question=...&kbIds=...`
  - `GET /api/tutor/sessions/{sessionId}/stream?question=...&kbIds=...`
- 禁止 query 参数承载：
  - `access_token`
  - `jwt`
  - `Authorization`
  - `question`
  - `prompt`
  - `kbIds`
  - learner/profile/resource/answer raw payload
- 禁止为了兼容 EventSource 在 production/staging 信任 `X-User-Id`、`X-Role`、`dev_user` 或 subject-name role inference。
- 禁止 CORS `Access-Control-Allow-Origin: *` 与 credentials 或 Bearer/cookie 混用。
- 禁止 stream reconnect 用未绑定 user/session/purpose/payload hash 的 bearer-like token。
- 禁止把 stream token、JWT、raw question、raw answer token 写入日志、trace、metrics tag、error response 或 URL。
- 禁止前端直接调用 LLM provider API 或保存 provider API key。

## 推荐协议方向

优先方案：`fetch` / `ReadableStream` + Bearer header。

- 前端使用现有 API client 的 Bearer 传递能力，发起 `POST` streaming 请求。
- 请求 body 承载 `question`、`kbIds`、`topK`、`requestId`。
- 响应使用 `text/event-stream`、NDJSON、或分块 JSON 事件均可，但必须明确事件 schema。
- 服务器在写出第一个事件前完成 JWT、scope、payload validation、requestId conflict 检查。
- 断线重连优先通过 `requestId` 查询状态或重新发起同 payload replay，不依赖 URL token。

备选方案：POST bootstrap + 短 TTL 一次性 stream token。

- 仅在必须兼容原生 `EventSource` 时考虑。
- bootstrap 必须 Bearer JWT 认证，返回一次性 opaque `streamId` 或 token。
- token 必须短 TTL、一次性、绑定 userId/sessionId/purpose/payloadHash/origin，可撤销或消费后失效。
- stream URL 不得包含 `question` / `kbIds` / JWT；最多包含 opaque id。
- 需要专门日志脱敏、token hash 存储、replay 防护和并发消费策略。

## 建议验收项

- production/staging 无 Bearer streaming 请求返回 `UNAUTHORIZED`，且没有 async work、RAG service call、query log、citation、model/token side effect。
- invalid / expired / wrong issuer / wrong audience Bearer 均 fail-closed。
- valid Bearer + spoofed `X-User-Id: admin` 只使用 JWT subject/roles。
- Bearer `USER sub=admin` 不获得 admin 权限。
- mixed allowed + forbidden `kbIds` 返回 `FORBIDDEN`，不产生部分流式 token。
- production/staging 前端不会创建包含 `question` / `kbIds` / token 的 URL。
- streaming 请求体中的 `question` / `kbIds` 不出现在 application log、access log、trace summary、metrics tag、error response。
- stream 首事件前完成权限复核；失败时不发送 `token` / `done` 伪成功事件。
- same `requestId` + same payload 支持 replay/resume；same `requestId` + different payload 返回 `CONFLICT`。
- 断线/取消不会留下 RUNNING 长挂任务；有明确 timeout、cleanup、client abort handling。
- CORS 只允许配置的前端 origin；preflight 对 `Authorization` header 可用但不放宽为 wildcard。
- CSRF 风险说明落入 SPEC/PLAN；若使用 cookie 或 bootstrap token，必须有 Origin/CSRF 防护验收。

## 允许修改文件候选

文档：

- `docs/requirements/REQ-20260611-p3-4-formal-production-streaming-design.md`
- `docs/specs/SPEC-20260611-p3-4-formal-production-streaming-design.md`
- `docs/plans/PLAN-20260611-p3-4-formal-production-streaming-design.md`
- `docs/tasks/TASK-20260611-p3-4-formal-production-streaming-design.md`
- `docs/context/CONTEXT-20260611-p3-4-formal-production-streaming-design.md`
- `docs/evidence/EVIDENCE-20260611-p3-4-formal-production-streaming-design.md`
- `docs/acceptance/ACCEPT-20260611-p3-4-formal-production-streaming-design.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

后端候选：

- `backend/src/main/java/com/learningos/rag/api/ChatController.java`
- `backend/src/main/java/com/learningos/tutor/api/TutorController.java`
- `backend/src/main/java/com/learningos/rag/api/dto/RagQueryDtos.java`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`，仅当 requestId/replay/status 语义需要扩展
- `backend/src/main/java/com/learningos/common/auth/SecurityConfig.java`，仅当 CORS/preflight 明确需要配置
- `backend/src/main/java/com/learningos/common/trace/StructuredRequestLoggingFilter.java`，仅当新增 route/log redaction 测试发现缺口

前端候选：

- `frontend/src/api/client.ts`
- `frontend/src/api/rag.ts`
- `frontend/src/pages/student/StudentDashboard.vue`
- 相关 frontend test 文件，按现有测试结构收窄

测试候选：

- `backend/src/test/java/com/learningos/common/auth/SseProductionAuthStrategyTest.java`
- `backend/src/test/java/com/learningos/rag/api/ChatControllerTest.java`
- `backend/src/test/java/com/learningos/tutor/api/TutorControllerTest.java`
- `backend/src/test/java/com/learningos/rag/application/RagQueryServiceTest.java`
- frontend 对 `streamChat` / production transport 的现有测试文件

## 禁止修改文件候选

- `backend/pom.xml`，除非 PLAN 明确需要并完成 dependency review。
- `backend/src/main/resources/db/migration/**`，除非设计引入持久 stream session；若引入则任务升级并补 schema spec。
- `backend/src/main/java/com/learningos/agent/**`，除非明确设计 AgentTask/Trace 作为 stream job 证据。
- `backend/src/main/java/com/learningos/assessment/**`
- `backend/src/main/java/com/learningos/knowledge/**`，除非只为测试 fixture 间接使用。
- `frontend` 中与学生 RAG streaming 无关的页面。
- OAuth/JWK 基础认证实现重构；本任务应复用既有 formal auth，不重做认证架构。
- VectorDB、parser/OCR、embedding、reranker 实现文件。

## 测试建议

Backend focused：

```powershell
cd backend
mvn --% -Dtest=SseProductionAuthStrategyTest,ChatControllerTest,TutorControllerTest test
```

Backend adjacent：

```powershell
cd backend
mvn --% -Dtest=SseProductionAuthStrategyTest,ChatControllerTest,TutorControllerTest,RagQueryServiceTest,SecurityFilterChainTest,SecurityJwtAuthenticationTest test
```

Backend full：

```powershell
cd backend
mvn test
```

Frontend focused/build：

```powershell
cd frontend
pnpm test
pnpm build
```

必须新增或保留的矩阵：

- production Chat stream：missing Bearer / invalid Bearer / valid Bearer + spoofed header / `USER sub=admin` role-confusion。
- production Tutor stream：同 Chat 对称矩阵。
- staging header-only：Chat/Tutor 均拒绝。
- sensitive URL：production/staging 不创建含 `question` / `kbIds` / token 的 URL。
- permission：foreign KB、mixed KB、PUBLIC 但 course-bound foreign KB 均不得流出 token。
- side effect：authz/authn 失败不调用 `RagQueryService`，不写 query/citation/model/token。
- idempotency：same request replay、payload conflict、client abort/retry。
- logging/trace：日志、trace、metrics 不含 raw question、Authorization、stream token。
- CORS/preflight：允许配置 origin + `Authorization` header；拒绝非配置 origin。

## 架构漂移风险

风险等级：中到高，取决于协议选择。

主要漂移点：

- 为兼容原生 `EventSource` 而引入 query token 或 header fallback，会破坏 formal OAuth2/JWT fail-closed 边界。
- 在前端绕过 shared API wrapper 或直接调用 LLM provider，违反 frontend/backend AI API 边界。
- 把权限判断放在 prompt 或前端状态中，违反 service-layer permission rule。
- 新增 stream session 持久化但不补 schema/REQ/SPEC/PLAN，会造成文档与架构漂移。
- 事件流失败后写入成功 trace/query/citation，会破坏 Agent Trace 和 RAG citation 可信度。
- CORS 为了调通流式请求放宽到 wildcard，会扩大跨站数据暴露面。

控制建议：

- 先完成 M 级 REQ/SPEC/PLAN/Context Pack，再实现。
- 默认采用 `fetch` / `ReadableStream` + Bearer，不新增 query token。
- 如选择 token bootstrap，单独做 security design，并把 token 存储、TTL、绑定、日志脱敏、replay 防护写入验收。
- 保持 P3-4 父 epic open；本子任务只关闭 formal production streaming design/implementation 对应语义项。

## Verdict

Security & Quality verdict：CONDITIONAL PASS for design readiness。

当前项目已有后端 production SSE auth fail-closed 和前端 sensitive URL fallback 的良好基础；formal production streaming design 可以继续推进，但必须作为 M 级跨前后端安全设计切片，禁止直接恢复 production 原生 `EventSource` + query params 的协议形态。
