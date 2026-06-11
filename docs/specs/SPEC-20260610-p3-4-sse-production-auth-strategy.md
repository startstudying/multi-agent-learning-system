# SPEC-20260610 P3-4 子任务：SSE production auth strategy

## 范围

本规格覆盖 Chat/Tutor SSE 在 production-like 环境下的认证策略和回归测试。核心是证明后端 SSE 入口 fail-closed，而不是实现新的 SSE token 协议。

## 当前事实

- `SecurityConfig` 在 `prod` / `production` / `staging` 下要求除 health/info 外的所有请求必须为 authenticated `JwtAuthenticationToken`。
- `DevAuthFilter` 仅在 dev/test 且无 Bearer 时允许 `X-User-Id` fallback。
- Chat/Tutor SSE Controller 已从 `CurrentUserService.currentUser()` 派生 explicit role facts 并传入 `RagQueryService`。
- 前端 `openSse()` 使用原生 `EventSource`，生产 Bearer-only 下不能自动携带 `Authorization` header。

## 设计决策

| Decision | Result |
|---|---|
| 不引入 query token | 避免 token 出现在 URL、浏览器历史、代理日志和监控日志。 |
| 不新增 signed stream token | 该方案需要 TTL、一次性使用、撤销、日志脱敏和发行接口，超出本 M 级子任务。 |
| 不修改前端生产 streaming 客户端 | 前端 fetch-streaming 是后续前后端 L/M 子任务。 |
| 通过测试固化后端 fail-closed | 防止未来为了让 EventSource 生产可用而回退到 header spoofing 或 query token。 |

## 目标行为

### Chat SSE

```http
GET /api/chat/sessions/{sessionId}/stream?question=...&kbIds=...
```

- production-like no Bearer：`401 UNAUTHORIZED` JSON envelope；`SseEmitter` 不启动；`RagQueryService` 不调用。
- production-like invalid Bearer：`401 UNAUTHORIZED` JSON envelope；不 fallback。
- production-like valid Bearer：允许进入 SSE；service 使用 JWT subject 和 explicit role facts。

### Tutor SSE

```http
GET /api/tutor/sessions/{sessionId}/stream?question=...&kbIds=...
```

- 认证行为与 Chat SSE 一致。

## 安全约束

- 不接受 `access_token` query parameter。
- 不使用 prompt 控制权限。
- 不让 Controller 自行绕过 Spring Security。
- 不把 `X-User-Id` 作为 production identity。
- 认证失败响应不得包含 token、secret、subject、spoofed user id 或 raw exception。

## 测试规格

新增：

- `backend/src/test/java/com/learningos/common/auth/SseProductionAuthStrategyTest.java`

覆盖：

- production Chat SSE no Bearer -> 401 / async not started / no service call。
- production Chat SSE invalid Bearer -> 401 / no fallback / no service call。
- production Chat SSE valid Bearer ADMIN + spoofed `X-User-Id` -> service receives token subject and `admin=true`。
- production Chat SSE `USER sub=admin` -> service receives `admin=false`。
- production Tutor SSE no Bearer -> 401 / async not started / no service call。
- production Tutor SSE invalid Bearer -> 401 / no fallback / no service call。
- production Tutor SSE valid Bearer ADMIN + spoofed `X-User-Id` -> service receives token subject and `admin=true`。
- production Tutor SSE `USER sub=admin` -> service receives `admin=false`。
- staging Chat SSE header-only -> 401 / async not started / no service call。
- staging Tutor SSE header-only -> 401 / async not started / no service call。

## Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 本轮主要新增测试；若改代码，Controller 仍只读取身份并委托 Service。 |
| Frontend rules | PASS | 不让前端直连 LLM；不新增前端 API key。 |
| Agent / RAG rules | PASS | RAG 权限仍在 `RagQueryService` / `PermissionService` 前置。 |
| Security | PASS | production-like 不信任 `X-User-Id`，不新增 query token。 |
| API / Database | PASS | 不改 API path / DTO / schema。 |
