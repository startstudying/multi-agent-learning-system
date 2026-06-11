# REQ-20260611 P3-4 子任务：formal production streaming design

## 1. 背景

`P3-4 权限与安全加固` 中仍遗留“正式 production streaming 设计”。前置切片已经完成：

- 后端 Chat/Tutor GET SSE 在 production/staging 下 Bearer/JWT fail-closed。
- 前端 production/staging 学生 RAG 已从含 `question` / `kbIds` 的 GET SSE URL 降级为非流式 `POST /api/rag/query`。

本子任务目标是恢复 production/staging 的学生 RAG 流式体验，同时避免敏感业务参数进入 URL，并继续复用后端 Bearer/JWT 和 Service 层 RAG 权限检查。

## 2. Task Type

Frontend + Backend API linkage / Security hardening / RAG streaming transport。

## 3. Skill Selection Report

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 原始需求来自项目 TODO，必须走 S/M/L 工作流。 |
| `multi-agent-coder` / `dispatching-parallel-agents` | 用户明确要求专家 subagent 并行开发；本任务跨前端、后端、安全和集成。 |
| `spring-ai-agent-backend` | 后端涉及 Spring Boot SSE streaming、RAG 查询入口和 Controller 边界。 |
| `vue-edu-admin-frontend` | 前端涉及 Vue 学生端 RAG 流式交互和 API wrapper。 |
| `test-driven-development` | 行为变更必须先写 RED，再实现。 |
| `security-review` | 生产 streaming 涉及 Bearer/JWT、敏感 URL、header spoofing 和 side-effect 边界。 |
| `agent-trace-governance` | RAG stream 输出必须保持 traceId / citation 可追踪，不伪造成功证据。 |

Missing Skills: 无。

GitHub Research Needed: No。现有 Spring `SseEmitter`、前端 `fetch` / `ReadableStream` 能覆盖最小生产化协议，不新增依赖。

New Project-Specific Skill To Create: 暂不需要。

## 4. Size Classification

- Size: M - Standard Feature Slice
- Reason: 同时影响后端 RAG streaming API、前端 shared request/stream wrapper、学生端 RAG 调用和安全测试矩阵；不新增 DB schema、依赖、部署拓扑或 Agent/RAG 核心算法。
- Required Documents: REQ / SPEC / PLAN / TASK / CONTEXT；PRD 不需要。
- Can Skip: PRD、dependency review、数据库迁移。
- Upgrade Trigger: 若引入 signed stream token、stream session 表、WebSocket、新依赖、真实模型 token streaming、Orchestrator workflow streaming，则停止并升级为 L 或拆新任务。

## 5. Subagent Decision

- Use Subagents: Yes
- Reason: 用户明确要求专家 subagent 并行开发，且任务跨前端、后端、安全和集成。
- Parallelism Level: L1/L2 parallel analysis and design；实现由主 Codex 单线集成。
- Selected Subagents:
  - Backend/Streaming Architect
  - Frontend Streaming Expert
  - Security & Quality
  - Integration Reviewer
- Reports:
  - `docs/subagents/runs/RUN-20260611-p3-4-formal-production-streaming-design-backend.md`
  - `docs/subagents/runs/RUN-20260611-p3-4-formal-production-streaming-design-security-quality.md`
  - `docs/subagents/runs/RUN-20260611-p3-4-formal-production-streaming-design-integration-precheck.md`
  - Frontend expert completed in-thread;报告未落盘，关键结论记录在本任务上下文。

## 6. Functional Requirements

### FR-1 Production stream endpoint

后端必须提供 production 可用的 POST streaming 主入口：

```http
POST /api/rag/query/stream
Accept: text/event-stream
Content-Type: application/json
Authorization: Bearer <jwt>
```

请求体复用 `RagQueryRequest` 字段：

```json
{
  "kbIds": ["kb_java_backend"],
  "question": "Why does SQL JOIN duplicate rows?",
  "topK": 5,
  "requestId": "optional-client-request-id"
}
```

### FR-2 Sensitive URL cleanup

production/staging 前端不得创建包含 `question` / `kbIds` / token 的 stream URL。正式生产流式调用必须通过 POST body 传业务输入。

### FR-3 Bearer/JWT identity

production/staging streaming 请求必须复用 Spring Security Bearer/JWT：

- missing Bearer -> `401 UNAUTHORIZED`，不启动 async work。
- invalid Bearer -> `401 UNAUTHORIZED`，不启动 async work。
- valid Bearer + spoofed `X-User-Id` -> 使用 JWT subject / roles。
- `USER sub=admin` 不获得 admin role facts。

### FR-4 Stream events

最小事件合同：

- `status`: `{ "stage": "RETRIEVING" | "RERANKING" | "GENERATING" }`
- `token`: `{ "text": "..." }`
- `done`: `{ "answer": "...", "sources": [...], "retrieval": {...}, "traceId": "...", "latencyMs": 123 }`
- `error`: `{ "code": "...", "message": "..." }`

本切片只承诺 transport streaming；后端可先得到完整 `RagQueryResponse` 后分事件输出，不承诺真实模型 token 级增量。

### FR-5 Frontend production behavior

production/staging 学生端点击 RAG：

- 使用 `fetch` + `ReadableStream` 消费 `POST /api/rag/query/stream`。
- 不创建 native `EventSource`。
- 能解析 `status` / `token` / `done` / `error`。
- `done` 后更新 answer、sources、traceId。
- stream 失败时进入错误态；不通过敏感 GET SSE fallback 重试。

### FR-6 Dev/test compatibility

dev/test 可保留现有 native `EventSource` GET SSE 演示路径及 fallback 测试，但 production/staging 不得依赖它。

## 7. Non-Functional Requirements

- 不新增依赖。
- 不新增数据库迁移。
- 不改变现有 `POST /api/rag/query` 非流式合同。
- 不让前端直接调用 LLM API。
- 不把 API key、JWT、raw question、raw prompt、Authorization header 写入 docs/memory/log/metrics tag。
- 后端 Controller 只做 HTTP/streaming adapter，RAG 权限继续由 `RagQueryService` / `PermissionService` 执行。

## 8. Acceptance Criteria

- [x] 后端新增 POST stream focused RED/GREEN 覆盖。
- [x] 前端 production/staging 使用 POST stream，且不创建 `EventSource`。
- [x] production/staging stream URL 不含 `question` / `kbIds` / token。
- [x] valid Bearer + spoofed header 使用 JWT subject / roles。
- [x] missing/invalid Bearer 不启动 async work，不调用 `RagQueryService`。
- [x] RAG stream `done` 保留 sources / traceId / retrieval。
- [x] dev/test legacy SSE 测试继续通过。
- [x] focused + adjacent tests 通过；full backend/frontend 验证按证据记录。
