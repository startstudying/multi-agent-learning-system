# CONTEXT-20260611 P3-4 子任务：formal production streaming design

## 1. Related Memory and Docs

- `AGENTS.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/subagents/SUBAGENT_REGISTRY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/harness/TEST_COMMANDS.md`
- `docs/specs/SPEC-20260610-p3-4-sse-production-auth-strategy.md`
- `docs/tasks/TASK-20260610-p3-4-frontend-sse-sensitive-url-cleanup.md`
- `docs/evidence/EVIDENCE-20260610-p3-4-frontend-sse-sensitive-url-cleanup.md`

## 2. Selected Skills

- `feature-development-workflow`
- `multi-agent-coder`
- `dispatching-parallel-agents`
- `spring-ai-agent-backend`
- `vue-edu-admin-frontend`
- `test-driven-development`
- `security-review`
- `agent-trace-governance`

## 3. Subagent Plan

Parallel analysis/design completed:

- Backend/Streaming Architect: `docs/subagents/runs/RUN-20260611-p3-4-formal-production-streaming-design-backend.md`
- Security & Quality: `docs/subagents/runs/RUN-20260611-p3-4-formal-production-streaming-design-security-quality.md`
- Integration Reviewer: `docs/subagents/runs/RUN-20260611-p3-4-formal-production-streaming-design-integration-precheck.md`
- Frontend Streaming Expert: completed in-thread;未落盘报告，关键结论纳入 REQ/PLAN。

Implementation mode: Single Codex integration after expert reports。

## 4. Files Allowed To Modify

Backend:

- `backend/src/main/java/com/learningos/rag/api/ChatController.java`
- `backend/src/test/java/com/learningos/rag/api/ChatControllerTest.java`
- `backend/src/test/java/com/learningos/common/auth/SseProductionAuthStrategyTest.java`

Frontend:

- `frontend/src/api/client.ts`
- `frontend/src/api/rag.ts`
- `frontend/src/pages/student/StudentDashboard.vue`
- `frontend/src/App.spec.ts`

Docs:

- `docs/requirements/REQ-20260611-p3-4-formal-production-streaming-design.md`
- `docs/specs/SPEC-20260611-p3-4-formal-production-streaming-design.md`
- `docs/plans/PLAN-20260611-p3-4-formal-production-streaming-design.md`
- `docs/tasks/TASK-20260611-p3-4-formal-production-streaming-design.md`
- `docs/context/CONTEXT-20260611-p3-4-formal-production-streaming-design.md`
- `docs/evidence/EVIDENCE-20260611-p3-4-formal-production-streaming-design.md`
- `docs/acceptance/ACCEPT-20260611-p3-4-formal-production-streaming-design.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 5. Files Not Allowed To Modify

- `backend/pom.xml`
- `frontend/package.json`
- `frontend/package-lock.json`
- `backend/src/main/resources/db/migration/**`
- `backend/src/main/java/com/learningos/common/auth/**`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
- `backend/src/main/java/com/learningos/tutor/api/TutorController.java`
- `backend/src/main/java/com/learningos/agent/**`
- `backend/src/main/java/com/learningos/assessment/**`
- VectorDB/parser/OCR/model gateway implementation files

## 6. Test Commands

RED/focused backend:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ChatControllerTest,SseProductionAuthStrategyTest test
```

Adjacent backend:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ChatControllerTest,TutorControllerTest,SseProductionAuthStrategyTest,SecurityFilterChainTest,SecurityJwtAuthenticationTest test
```

Full backend:

```powershell
cd D:\多元agent\backend
mvn test
```

Focused frontend:

```powershell
cd D:\多元agent\frontend
npm test -- --run src/App.spec.ts
```

Frontend type/build:

```powershell
cd D:\多元agent\frontend
npx vue-tsc -b --noEmit
npm run build
```

## 7. Current Task Boundary

- Only production/staging student RAG stream path is changed from REST fallback to POST stream.
- Dev/test native `EventSource` path remains for demo compatibility.
- Existing GET SSE endpoint remains unchanged.
- No token query, no signed stream token, no auth framework change.
- No DB/dependency/schema change.
