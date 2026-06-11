# EVIDENCE-20260610 P3-4 子任务：SSE production auth strategy

## Scope

本证据记录 P3-4 子任务 `SSE production auth strategy` 的专家审查、测试补强和验证结果。

目标：固化 Chat/Tutor SSE 在 production-like 环境下的 Bearer/JWT fail-closed 策略，防止为了兼容原生 `EventSource` 而回退到 `X-User-Id`、`dev_user` 或 query token。

## Implementation Evidence

| Area | Evidence |
|---|---|
| Production auth boundary | `backend/src/main/java/com/learningos/common/auth/SecurityConfig.java` 已在 `prod` / `production` / `staging` 要求受保护请求必须是 authenticated `JwtAuthenticationToken`。 |
| Dev/test fallback boundary | `backend/src/main/java/com/learningos/common/auth/DevAuthFilter.java` 仅在 dev/test 且无 Bearer 时允许 `X-User-Id` fallback；Bearer 存在时交给 Spring Security。 |
| Chat SSE role facts | `backend/src/main/java/com/learningos/rag/api/ChatController.java` stream 分支从 `CurrentUserService.currentUser()` 捕获 JWT subject 与 roles，并将 explicit admin/teacher facts 传给 `RagQueryService`。 |
| Tutor SSE role facts | `backend/src/main/java/com/learningos/tutor/api/TutorController.java` stream 分支同样传入 explicit role facts。 |
| Regression tests | 新增 `backend/src/test/java/com/learningos/common/auth/SseProductionAuthStrategyTest.java`，覆盖 production/staging Chat/Tutor SSE 认证矩阵。 |
| Production code changes | 本轮未修改生产代码；focused tests 首轮已证明现有生产策略满足 fail-closed，后续只补齐测试矩阵。 |

## Test Coverage

| Test Method | Coverage |
|---|---|
| `chatStreamInProductionRejectsMissingBearerBeforeStartingAsyncWork` | production Chat SSE 无 Bearer + spoofed `X-User-Id` 返回 `UNAUTHORIZED`，不启动 async，不调用 `RagQueryService`。 |
| `chatStreamInProductionRejectsInvalidBearerBeforeStartingAsyncWork` | production Chat SSE invalid Bearer 不 fallback，不启动 async，不调用服务。 |
| `chatStreamInProductionUsesBearerRolesAndIgnoresSpoofedUserIdHeader` | production Chat SSE valid Bearer 使用 JWT subject / role facts，忽略 spoofed `X-User-Id`。 |
| `chatStreamInProductionDoesNotInferAdminFromBearerSubjectName` | production Chat SSE Bearer `USER sub=admin` 不获得 admin facts。 |
| `tutorStreamInProductionRejectsMissingBearerBeforeStartingAsyncWork` | production Tutor SSE 无 Bearer + spoofed `X-User-Id` 返回 `UNAUTHORIZED`，不启动 async，不调用服务。 |
| `tutorStreamInProductionRejectsInvalidBearerBeforeStartingAsyncWork` | production Tutor SSE invalid Bearer 不 fallback，不启动 async，不调用服务。 |
| `tutorStreamInProductionUsesBearerRolesAndIgnoresSpoofedUserIdHeader` | production Tutor SSE valid Bearer 使用 JWT subject / role facts，忽略 spoofed `X-User-Id`。 |
| `tutorStreamInProductionDoesNotInferAdminFromBearerSubjectName` | production Tutor SSE Bearer `USER sub=admin` 不获得 admin facts。 |
| `chatStreamInStagingRejectsHeaderOnlyAuthBeforeStartingAsyncWork` | staging Chat SSE header-only auth 被拒绝，不启动 async，不调用服务。 |
| `tutorStreamInStagingRejectsHeaderOnlyAuthBeforeStartingAsyncWork` | staging Tutor SSE header-only auth 被拒绝，不启动 async，不调用服务。 |

## Verification Commands

### Focused - SSE production/staging auth strategy

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=SseProductionAuthStrategyTest test
```

Result:

```text
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-10T13:38:34+08:00
```

### Adjacent - auth / SSE / RAG runtime

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=SseProductionAuthStrategyTest,ChatControllerTest,TutorControllerTest,SecurityFilterChainTest,DevAuthFilterTest,SecurityJwtAuthenticationTest test
```

Result:

```text
Tests run: 38, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-10T13:40:02+08:00
```

### Full backend

```powershell
cd D:\多元agent\backend
mvn test
```

Result:

```text
Tests run: 530, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
Finished at: 2026-06-10T13:42:54+08:00
```

## Expert Review Evidence

| Expert | Report / Result |
|---|---|
| Backend Architect | `docs/subagents/runs/RUN-20260610-p3-4-sse-production-auth-strategy-architect.md`：建议本轮不引入 query token / signed stream token，后续用 fetch streaming + Bearer 或专门 token 协议处理生产前端流式能力。 |
| Test Engineer | `docs/subagents/runs/RUN-20260610-p3-4-sse-production-auth-strategy-test.md`：建议新增 production/staging SSE auth regression matrix。 |
| Security Reviewer | `docs/subagents/runs/RUN-20260610-p3-4-sse-production-auth-strategy-security.md`：指出生产 EventSource 无 Bearer header 风险、SSE GET query 敏感内容风险，以及缺少 SSE 生产认证回归测试。 |
| Code Reviewer | 初次复核为 FAIL，要求补齐 Tutor production 对称矩阵与 staging Tutor header-only；补齐后复核 PASS。 |

## Architecture Drift Check After

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 本轮只新增测试；Controller 身份读取和 Service 委托边界未改。 |
| Frontend rules | PASS | 未修改 frontend，未引入前端 LLM 调用、API key 或 token query。 |
| Agent / RAG | PASS | RAG retrieval 权限过滤仍由 `RagQueryService` / `PermissionService` 执行；未改 Agent loop。 |
| Security | PASS | production/staging 不信任 `X-User-Id`，不新增 query token，不记录 secret。 |
| API / Database | PASS | 未改 API path、DTO、schema、migration 或 dependency。 |

## Accepted Limitations / Follow-up

- 生产前端原生 `EventSource` 仍无法携带 `Authorization` header；生产可用流式客户端需要后续独立任务处理。
- 后续优先方案：`fetch` / `ReadableStream` + Bearer header。
- 若继续使用原生 `EventSource`，需要设计短 TTL、一次性、绑定 user/session/purpose 的 stream token，并处理日志脱敏；本轮未实现。
- 当前 SSE GET query 仍携带 `question` / `kbIds`，可能进入浏览器历史或代理日志；后续应考虑 POST 创建 stream session 后再打开不含敏感正文的 stream。
- P3-4 parent remains open.

## Acceptance Verdict

Verdict: PASS for `P3-4 子任务：SSE production auth strategy`.

P3-4 parent remains open.
