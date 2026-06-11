# SPEC-20260611 P3-4 子任务：formal production streaming design

## 1. Scope

本规格定义学生端 RAG 的正式生产流式传输协议。范围限制：

- 新增 `POST /api/rag/query/stream`。
- 前端 production/staging 改用 `fetch` / `ReadableStream`。
- 保留 dev/test legacy native `EventSource` 演示路径。
- 不新增依赖、DB schema、signed stream token、WebSocket、真实模型 token streaming。

## 2. Current Facts

- `ChatController` 已有：
  - `POST /api/rag/query`
  - `GET /api/rag/query`
  - `GET /api/chat/sessions/{sessionId}/stream?question=...&kbIds=...`
- `RagQueryService` 已支持：
  - explicit role facts: `currentUserAdmin` / `currentUserTeacher`
  - `requestId` replay / conflict
  - replay 前重新执行 KB read permission
- `StudentDashboard.vue` 已在 production/staging 使用非流式 `POST /api/rag/query` fallback，避免敏感 GET SSE URL。
- `openSse()` 当前是 native `EventSource`，不能携带 `Authorization` header。

## 3. API Contract

### 3.1 POST RAG stream

```http
POST /api/rag/query/stream
Accept: text/event-stream
Content-Type: application/json
Authorization: Bearer <jwt>
```

Request body:

```json
{
  "kbIds": ["kb_java_backend"],
  "question": "Why does SQL JOIN duplicate rows?",
  "topK": 5,
  "requestId": "optional-client-request-id"
}
```

Response content type:

```text
text/event-stream
```

Event examples:

```text
event:status
data:{"stage":"RETRIEVING"}

event:status
data:{"stage":"RERANKING"}

event:status
data:{"stage":"GENERATING"}

event:token
data:{"text":"Grounded answer..."}

event:done
data:{"answer":"Grounded answer...","sources":[],"retrieval":{},"traceId":"trc_x","latencyMs":42}
```

`error` event is allowed for stream-time failures after async work starts:

```text
event:error
data:{"code":"RAG_STREAM_FAILED","message":"RAG stream failed"}
```

Authentication failures remain normal JSON `ApiResponse` 401 before async starts.

### 3.2 Existing APIs

No contract change:

- `POST /api/rag/query`
- `GET /api/rag/query`
- `GET /api/chat/sessions/{sessionId}/stream`
- `POST /api/tutor/ask`
- `GET /api/tutor/sessions/{sessionId}/stream`

`GET .../stream` is treated as dev/test legacy demo path for frontend production purposes.

## 4. Backend Design

### 4.1 Controller

`ChatController` adds:

```java
@PostMapping(value = "/api/rag/query/stream", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter streamQuery(@Valid @RequestBody RagQueryRequest request)
```

Behavior:

1. Resolve `UserContext currentUser = currentUserService.currentUser()`.
2. Create `SseEmitter`.
3. Run virtual thread.
4. Execute `RagQueryService` using JWT-derived `userId` and role facts.
5. After service returns, emit `status` / `token` / `done`.
6. On exception after async starts, emit sanitized `error`, then complete.

Important: authentication failure is enforced by Spring Security before entering Controller in production/staging. The endpoint must not implement its own header fallback.

### 4.2 Service

Reuse existing methods:

- With `requestId`: `ragQueryService.queryWithRequestId(...)`
- Without `requestId`: `ragQueryService.query(...)`

No new service method is required for this M slice.

### 4.3 Events

`done` payload must include:

- `answer`
- `sources`
- `retrieval`
- `traceId`
- `latencyMs`

`token.text` may equal the complete answer for this minimum transport-streaming slice.

## 5. Frontend Design

### 5.1 Shared client

`frontend/src/api/client.ts` adds:

- in-memory Bearer token setter/getter for runtime auth integration.
- shared header builder used by normal API calls and stream calls.
- `streamRequest(path, init, handlers)` using `fetch` + `ReadableStream`.

No token is persisted to localStorage/sessionStorage in this task.

### 5.2 RAG API

`frontend/src/api/rag.ts` adds:

- `streamRagQuery(payload, handlers, signal?)`

It calls:

```http
POST /api/rag/query/stream
```

with JSON body, `Accept: text/event-stream`, and shared auth headers.

`streamChat(...)` remains as dev/test legacy native `EventSource`.

### 5.3 Student page

`StudentDashboard.vue` changes production/staging path:

- before: `queryRagRest()`
- after: `streamRagQueryResponse()`

Failure policy:

- production/staging stream error -> UI error state, no legacy GET SSE fallback.
- dev/test legacy SSE fail -> existing REST fallback remains.

## 6. Security Rules

- Production/staging must not use native `EventSource` for formal stream.
- Stream URL must not contain `question`, `kbIds`, JWT, token, prompt, or raw learner context.
- Missing/invalid Bearer fails before async work.
- `X-User-Id` must not override JWT identity in production/staging.
- `USER sub=admin` must not gain admin semantics.
- No new query token or signed URL in this task.
- Errors must not expose raw exception, JWT, Authorization header, or raw prompt.

## 7. Architecture Drift Check Before Implementation

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller adapts HTTP/SSE and delegates to `RagQueryService`. |
| Frontend rules | PASS | Frontend calls backend only, through shared client. |
| Agent / RAG rules | PASS | RAG permission/citations remain in backend service layer. |
| Security | PASS | Bearer/JWT fail-closed; no query token. |
| API / Database | PASS | New endpoint only; no DB change. |

## 8. Out of Scope

- Real model-token incremental streaming.
- signed stream token or stream session persistence.
- WebSocket.
- Orchestrator workflow streaming.
- Tutor production stream client integration.
- CORS policy redesign.
- New auth/login UI.
