# TASK-20260610 P3-4 子任务：SSE production auth strategy

## Goal

固化 Chat/Tutor SSE 在 production-like 环境下的 Bearer/JWT fail-closed 策略，防止 SSE 为了兼容原生 `EventSource` 而退回 `X-User-Id`、`dev_user` 或 query token。

## Scope

In:

- 新增 production/staging SSE auth 回归测试。
- 更新相关工作流文档、证据、验收、记忆和 TODO。

Out:

- 不新增 signed stream token。
- 不改前端 `EventSource` 实现。
- 不新增 cookie/session auth。
- 不改 DB/schema/dependency。

## Allowed files

Production:

- `backend/src/main/java/com/learningos/common/auth/SecurityConfig.java`（仅当测试暴露缺口时）
- `backend/src/main/java/com/learningos/common/auth/DevAuthFilter.java`（仅当测试暴露缺口时）
- `backend/src/main/java/com/learningos/rag/api/ChatController.java`（仅当测试暴露缺口时）
- `backend/src/main/java/com/learningos/tutor/api/TutorController.java`（仅当测试暴露缺口时）

Tests:

- `backend/src/test/java/com/learningos/common/auth/SseProductionAuthStrategyTest.java`
- `backend/src/test/java/com/learningos/rag/api/ChatControllerTest.java`（仅必要时）
- `backend/src/test/java/com/learningos/tutor/api/TutorControllerTest.java`（仅必要时）

Docs:

- `docs/requirements/REQ-20260610-p3-4-sse-production-auth-strategy.md`
- `docs/specs/SPEC-20260610-p3-4-sse-production-auth-strategy.md`
- `docs/plans/PLAN-20260610-p3-4-sse-production-auth-strategy.md`
- `docs/tasks/TASK-20260610-p3-4-sse-production-auth-strategy.md`
- `docs/context/CONTEXT-20260610-p3-4-sse-production-auth-strategy.md`
- `docs/evidence/EVIDENCE-20260610-p3-4-sse-production-auth-strategy.md`
- `docs/acceptance/ACCEPT-20260610-p3-4-sse-production-auth-strategy.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## Disallowed files

- `frontend/**`
- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- secrets / env files

## Acceptance Criteria

- production Chat SSE no Bearer returns `UNAUTHORIZED`, does not start async, and does not call `RagQueryService`。
- production Chat SSE invalid Bearer has the same behavior。
- production Tutor SSE no Bearer returns `UNAUTHORIZED`, does not start async, and does not call `RagQueryService`。
- production Tutor SSE invalid Bearer has the same behavior。
- staging Chat/Tutor SSE header-only auth is rejected。
- production Chat/Tutor SSE valid Bearer uses JWT subject and role facts, ignoring spoofed `X-User-Id`。
- production Chat/Tutor SSE Bearer `USER sub=admin` does not get admin facts。
- focused / adjacent / full backend tests run or limitations are documented。
