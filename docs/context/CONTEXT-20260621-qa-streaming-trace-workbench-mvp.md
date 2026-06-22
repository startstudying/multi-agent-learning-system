# CONTEXT-20260621 QA streaming and trace workbench MVP

## 1. 相关记忆与文档

- `docs/plans/PLAN-20260621-memory-answer-quality-execution-readable.md`
- `docs/specs/SPEC-20260621-qa-runtime-structured-answer-mvp.md`
- `docs/specs/SPEC-20260621-qa-verifier-eval-gate-mvp.md`
- `docs/evidence/EVIDENCE-20260621-qa-verifier-eval-gate-mvp.md`
- `docs/acceptance/ACCEPT-20260621-qa-verifier-eval-gate-mvp.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/FRONTEND_MEMORY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`

## 2. Selected Skills

- feature-development-workflow
- spring-ai-agent-backend
- agent-trace-governance
- vue-edu-admin-frontend
- frontend-design
- test-driven-development
- verification-before-completion

## 3. Subagent Plan

No subagents. 单 Codex 顺序执行。

## 4. Files Allowed To Modify

- `backend/src/main/java/com/learningos/aiqa/api/AiQaController.java`
- `backend/src/test/java/com/learningos/aiqa/api/AiQaControllerTest.java`
- `frontend/src/api/aiQa.ts`
- `frontend/src/types/api.ts`
- `frontend/src/pages/student/StudentDashboard.vue`
- `frontend/src/components/workspace/WorkspaceStream.vue`
- `frontend/src/components/learning/AiMessageBlock.vue`
- `frontend/src/App.spec.ts`
- `docs/product/PRD-20260621-qa-streaming-trace-workbench-mvp.md`
- `docs/requirements/REQ-20260621-qa-streaming-trace-workbench-mvp.md`
- `docs/specs/SPEC-20260621-qa-streaming-trace-workbench-mvp.md`
- `docs/plans/PLAN-20260621-qa-streaming-trace-workbench-mvp.md`
- `docs/tasks/TASK-20260621-qa-streaming-trace-workbench-mvp.md`
- `docs/context/CONTEXT-20260621-qa-streaming-trace-workbench-mvp.md`
- `docs/evidence/EVIDENCE-20260621-qa-streaming-trace-workbench-mvp.md`
- `docs/acceptance/ACCEPT-20260621-qa-streaming-trace-workbench-mvp.md`
- `docs/plans/PLAN-20260621-memory-answer-quality-execution-readable.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/FRONTEND_MEMORY.md`

## 5. Files Not Allowed To Modify

- `backend/pom.xml`
- `frontend/package.json`
- `backend/src/main/resources/db/**`
- provider credentials / env files
- RAG parser/vector/index worker
- ResourceGeneration / ReviewGate production code
- P2 memory lifecycle production modules

## 6. Test Commands

```powershell
cd backend
mvn --% -Dtest=AiQaControllerTest test
mvn --% -DskipTests -Dmaven.compiler.showWarnings=true -Dmaven.compiler.showDeprecation=true compile
```

```powershell
cd frontend
pnpm test -- --run App.spec.ts
pnpm build
```

## 7. Current Task Boundary

只实现 P1 MVP：AI QA transport streaming + student quality/trace visibility。不得实现 P2 memory lifecycle、DB schema、真实 provider token streaming 或批量 eval workbench。
