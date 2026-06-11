# CONTEXT-20260610 P3-4 子任务：SSE production auth strategy

## Related memory/docs

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/skills/project-specific/auth-context-boundary.md`
- `docs/specs/SPEC-20260609-p3-4-formal-oauth2-jwk-spring-security.md`
- `docs/specs/SPEC-20260610-p3-4-rag-query-runtime-roles-first-rbac.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`

## Subagent reports

- `docs/subagents/runs/RUN-20260610-p3-4-sse-production-auth-strategy-architect.md`
- `docs/subagents/runs/RUN-20260610-p3-4-sse-production-auth-strategy-test.md`
- `docs/subagents/runs/RUN-20260610-p3-4-sse-production-auth-strategy-security.md`

## Current evidence

- `SecurityConfig` 已要求 production-like 受保护请求必须为 authenticated `JwtAuthenticationToken`。
- `DevAuthFilter` 仅 dev/test 无 Bearer 时启用 header fallback。
- Chat/Tutor SSE 已向 `RagQueryService` 传 explicit role facts。
- 已新增 production/staging Chat/Tutor SSE auth 专项测试，覆盖 no Bearer、invalid Bearer、valid Bearer role facts、subject-name role-confusion 与 header-only 拒绝。

## Allowed files

见 `docs/tasks/TASK-20260610-p3-4-sse-production-auth-strategy.md`。

## Test commands

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=SseProductionAuthStrategyTest test
mvn --% -Dtest=SseProductionAuthStrategyTest,ChatControllerTest,TutorControllerTest,SecurityFilterChainTest,DevAuthFilterTest,SecurityJwtAuthenticationTest test
mvn test
```

## Boundary

当前子任务只关闭“后端 production-like SSE Bearer fail-closed 策略已验证”这一语义子项；P3-4 父项仍 open，前端生产 streaming 客户端策略仍为后续任务。
