# RUN-20260610 P3-4 SSE production auth strategy - Backend Architect

## 角色

Backend Architect。

## 结论

当前后端 production-like 环境已经通过 Spring Security 要求受保护请求必须是已认证 `JwtAuthenticationToken`，不会信任 `X-User-Id` fallback。SSE 的核心生产风险不是后端已开放，而是前端仍使用原生 `EventSource`，无法携带 `Authorization` header，所以生产 Bearer-only 策略下 SSE 会在进入 Controller 前被 401 拦截。

## 关键证据

- `backend/src/main/java/com/learningos/common/auth/SecurityConfig.java`：`prod` / `production` / `staging` 下除 health/info 以外必须是 authenticated `JwtAuthenticationToken`。
- `backend/src/main/java/com/learningos/common/auth/DevAuthFilter.java`：Bearer 存在时交给 Spring Security；只有 dev/test 且无 Bearer 时才允许 `X-User-Id` fallback。
- `backend/src/main/java/com/learningos/rag/api/ChatController.java`：Chat SSE 捕获 `UserContext` 并把 explicit role facts 传入 `RagQueryService`。
- `backend/src/main/java/com/learningos/tutor/api/TutorController.java`：Tutor SSE 同样传递 explicit role facts。
- `frontend/src/api/client.ts`：`openSse()` 使用 `new EventSource(...)`，不能设置 `Authorization` header。
- `docs/architecture/rag-architecture.md`：已记录浏览器 `EventSource` 不能发送自定义 header，生产需要 cookie/session auth 或 signed stream token。

## 设计建议

本子任务采用最小生产安全策略收口：

1. 后端继续 fail-closed：production-like SSE 无 Bearer 或 invalid Bearer 必须在进入 Controller 前返回 `UNAUTHORIZED`，不得 fallback 到 `X-User-Id` / `dev_user`。
2. 不引入 query `access_token` 或 signed stream token；URL token 有浏览器历史、代理日志、应用日志泄露风险。
3. 通过专项回归测试证明 Chat/Tutor SSE 与普通 API 一样走 Spring Security Bearer/JWT。
4. 将完整“生产可用 SSE 客户端”留作后续前端/协议切片：推荐 `fetch` streaming + `Authorization: Bearer ...`，而不是原生 `EventSource`。

## 边界

- 不新增依赖。
- 不改数据库 schema。
- 不改变现有 REST DTO。
- 不关闭 P3-4 父项。
- 本轮不实现 signed stream token / cookie session / fetch-streaming 前端。

