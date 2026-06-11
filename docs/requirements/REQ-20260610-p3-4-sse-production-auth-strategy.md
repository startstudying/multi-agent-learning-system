# REQ-20260610 P3-4 子任务：SSE production auth strategy

## 背景

P3-4 已完成 formal OAuth2/JWK/Spring Security 最小认证边界，以及 RAG query runtime roles-first RBAC。当前剩余风险是 SSE 生产认证策略：本地 test/dev 环境可通过 `X-User-Id` fallback 跑通 Chat/Tutor SSE，但 production-like 环境必须保持 Bearer/JWT fail-closed。

前端当前使用原生 `EventSource` 打开 SSE；原生 `EventSource` 不能携带 `Authorization` header。因此本轮目标不是引入不安全的 query token，而是先明确并验证后端生产策略：SSE endpoint 与普通 API 一样必须经过 Spring Security Bearer/JWT，不能 fallback 到开发身份。

## 目标

- 固化 production/staging 下 Chat/Tutor SSE 必须 Bearer/JWT 的策略。
- 证明 no Bearer / invalid Bearer / header-only spoofing 不会启动 SSE async，不会调用 `RagQueryService`。
- 证明 valid Bearer SSE 仍按 JWT subject 和 explicit role facts 进入 `RagQueryService`。
- 记录前端原生 `EventSource` 的生产限制，并把 fetch streaming / cookie / signed stream token 作为后续设计，而非本轮隐式引入。

## 功能需求

| ID | Requirement |
|---|---|
| FR-01 | `prod` / `production` / `staging` 下，`GET /api/chat/sessions/{sessionId}/stream` 无 Bearer 时必须返回 `UNAUTHORIZED`，且不启动 async。 |
| FR-02 | `prod` / `production` / `staging` 下，Chat SSE invalid Bearer 不得 fallback 到 `X-User-Id`。 |
| FR-03 | `prod` / `production` / `staging` 下，`GET /api/tutor/sessions/{sessionId}/stream` 无 Bearer或 invalid Bearer 时必须返回 `UNAUTHORIZED`，且不启动 async。 |
| FR-04 | valid Bearer 访问 Chat/Tutor SSE 时，`RagQueryService` 必须收到 JWT subject 和从 `UserContext.roles()` 派生的 role facts。 |
| FR-05 | Chat/Tutor SSE 中 Bearer `USER sub=admin` 不得获得 admin role facts。 |
| FR-06 | 本轮不得新增 query `access_token`、signed stream token、cookie session 或新依赖。 |

## 非目标

- 不实现前端 fetch-streaming 客户端。
- 不实现 signed stream token。
- 不修改登录/刷新 token 体系。
- 不新增数据库表或 migration。
- 不改 RAG answer/citation 语义。
- 不关闭 P3-4 父项。

## 验收标准

- 新增 production/staging Chat/Tutor SSE auth 回归测试并通过。
- adjacent auth/SSE/RAG runtime 测试通过。
- Evidence 明确区分已完成的后端 fail-closed 策略和未完成的生产前端 streaming 客户端策略。
