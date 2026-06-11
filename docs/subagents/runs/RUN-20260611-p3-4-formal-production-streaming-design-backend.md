# P3-4 formal production streaming design 后端只读分析报告

## 结论

当前后端已经具备可落地的流式骨架，但“正式生产流式设计”还没完全收敛成统一合同。现状是：

- `ChatController` / `TutorController` 已提供 SSE 入口，且都通过 `CurrentUserService` 传递显式角色事实。
- `RagQueryService` 已支持 `query` / `queryWithRequestId` / `queryWithTraceIdAndRequestId`，可承接 POST 查询与 replay。
- 生产/测试认证边界已经比较清晰，`prod` / `production` / `staging` 走 Bearer JWT，`dev` / `test` 才保留 `X-User-Id` 兼容。

我的判断：此切片更适合定为 **M**，不是 L。原因是它主要是流式合同、认证上下文和测试矩阵收敛，不涉及新依赖、DB schema、前端合同大改或新的 Agent/RAG 核心算法。

## 现有后端流式入口

### 1) RAG POST 查询入口

- `POST /api/rag/query`
  - `backend/src/main/java/com/learningos/rag/api/ChatController.java`
  - 支持 `requestId` 时走 `ragQueryService.queryWithRequestId(...)`
  - 不带 `requestId` 时走 `ragQueryService.query(...)`

- `GET /api/rag/query`
  - 同样走 `ragQueryService.query(...)`
  - 适合作为非流式 fallback / 调试入口

### 2) Chat SSE

- `GET /api/chat/sessions/{sessionId}/stream`
  - `backend/src/main/java/com/learningos/rag/api/ChatController.java`
  - 当前事件序列：`status` -> `status` -> `status` -> `status` -> `token` -> `done`
  - `done` 里返回 `sources`、`retrieval`、`traceId`、`latencyMs`、`sessionId`

### 3) Tutor SSE / POST

- `POST /api/tutor/ask`
  - `backend/src/main/java/com/learningos/tutor/api/TutorController.java`

- `GET /api/tutor/sessions/{sessionId}/stream`
  - 同样是 `SseEmitter`
  - 事件与 Chat 基本一致

### 4) RAG service 侧流式/重放支撑

- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
  - `queryWithTraceIdAndRequestId(...)` 已支持 request replay
  - `replayQueryIfPresent(...)` 里会重新走 `replayContext(...)`
  - `replayContext(...)` 会再次调用 `permissionService.requireReadableKbIds(...)`
  - 这说明 RAG replay 语义已经是“重放前重新过授权”

## 认证上下文

### 证据

- `backend/src/main/java/com/learningos/common/auth/CurrentUserService.java`
  - 优先读 `SecurityContextHolder` 中的 `JwtAuthenticationToken`
  - 没有安全上下文时才回退 `UserContextHolder`
  - `isAdmin()` / `isTeacherUser()` 优先看 roles
  - 仅 dev/test 才允许旧的 `admin` / `teacher_*` 字符串推断

- `backend/src/main/java/com/learningos/common/auth/DevAuthFilter.java`
  - `X-User-Id` 是开发兼容头，不应作为生产身份来源

- `backend/src/main/java/com/learningos/common/auth/SecurityConfig.java`
  - `prod` / `production` / `staging` 只能接受 `JwtAuthenticationToken`
  - Bearer 缺失或无效时应失败，不回退 header 身份

### 结论

- Chat/Tutor/RAG 的流式与 POST 查询入口，身份都应来自 `CurrentUserService.currentUser()`
- `X-User-Id` 只应作为 dev/test fallback
- `Bearer USER sub=admin` 不应自动变成 admin 语义

## 建议 API 合同

### 推荐保持的主合同

1. `POST /api/rag/query`
   - 请求体：`question`、`kbIds`、`topK?`、`requestId?`
   - 响应：`ApiResponse<RagQueryResponse>`
   - 适合幂等 replay / 非流式调用

2. `GET /api/chat/sessions/{sessionId}/stream`
   - 查询参数：`question`、`kbIds`、`topK?`
   - SSE 事件建议保留：
     - `status`
     - `token`
     - `citation`
     - `done`
     - `error`

3. `GET /api/tutor/sessions/{sessionId}/stream`
   - 与 Chat SSE 保持同样的事件协议

### 我建议的收敛点

- 生产环境只接受 Bearer JWT，不接受 `X-User-Id` 作为身份来源
- SSE 事件中的 `done` 可以继续返回 `sources/retrieval/traceId/latencyMs/sessionId`
- 如果后续要做断线重连或幂等流式恢复，再额外引入 `requestId` / `Last-Event-ID` 语义；当前不建议强行把所有 SSE 改成可 replay

## 允许修改文件候选

### 代码文件候选

- `backend/src/main/java/com/learningos/rag/api/ChatController.java`
- `backend/src/main/java/com/learningos/tutor/api/TutorController.java`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
- `backend/src/main/java/com/learningos/common/auth/CurrentUserService.java`
- `backend/src/main/java/com/learningos/common/auth/SecurityConfig.java`
- `backend/src/main/java/com/learningos/common/auth/DevAuthFilter.java`

### 测试文件候选

- `backend/src/test/java/com/learningos/common/auth/SseProductionAuthStrategyTest.java`
- `backend/src/test/java/com/learningos/rag/api/ChatControllerTest.java`
- `backend/src/test/java/com/learningos/tutor/api/TutorControllerTest.java`
- `backend/src/test/java/com/learningos/rag/application/RagQueryServiceTest.java`

### 配置文件候选

- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-prod.yml`（如后续需要新增生产专用覆盖）
- `backend/src/main/resources/application-staging.yml`（如后续需要新增预发专用覆盖）

## 风险

1. **SSE 只覆盖 `status/token/done`**  
   当前没有显式 `error` / `citation` 事件，正式生产流式体验和可观测性还不够完整。

2. **`sessionId` 不是认证边界**  
   现在它只是路径参数和响应字段，不能承担授权作用。

3. **流式入口与 replay 语义尚未统一**  
   `RagQueryService` 已具备 replay 前重新授权的能力，但 SSE 入口本身仍是 live-stream 语义，未来如果要断线恢复，需要单独设计幂等和恢复协议。

4. **生产与测试 profile 行为差异明显**  
   `test/dev` 允许 header fallback，`prod/staging` 不允许；如果新流式入口绕开 `CurrentUserService`，很容易引入环境不一致。

5. **RAG citations 只在 done payload 返回**  
   如果产品要求“流中可见引用”，目前还需要补事件级输出。

## 测试建议

### 现有已覆盖

- `backend/src/test/java/com/learningos/common/auth/SseProductionAuthStrategyTest.java`
  - 生产/预发下无 Bearer、invalid Bearer、Bearer + spoofed header 的行为已覆盖

- `backend/src/test/java/com/learningos/rag/api/ChatControllerTest.java`
  - GET/POST 查询与 SSE 事件序列已覆盖

- `backend/src/test/java/com/learningos/tutor/api/TutorControllerTest.java`
  - Tutor POST / SSE 已覆盖

### 建议补的最小测试

1. `ChatControllerTest`
   - Bearer `ADMIN` + spoofed `X-User-Id`
   - Bearer `USER sub=admin`
   - SSE `done` 中包含 `traceId/sessionId/sources`

2. `TutorControllerTest`
   - 同样补 Bearer / spoofed header / subject-confusion 矩阵

3. `SseProductionAuthStrategyTest`
   - 明确覆盖 `POST /api/rag/query` 在 production-like profile 下也不接受 header-only 身份
   - 确认 invalid Bearer 不 fallback 到 `X-User-Id`

4. `RagQueryServiceTest`
   - 保持 `replayQueryIfPresent(...)` 在 replay 前重新过 allowed KB 校验
   - 确认 no-source、citation、response snapshot 的安全边界不变

## 设计判断

### 证据

- SSE 与 POST 查询入口都已存在
- 认证根边界已经在 `SecurityConfig` / `CurrentUserService` 中明确区分 prod-like 和 dev/test
- RAG replay 语义已经是“先重算授权、再重放结果”

### 推断

- 这个切片最合理的目标，不是再发明新的流式框架，而是把现有 SSE / POST / auth / replay 合同写成一致的生产设计，并补足生产化测试矩阵

