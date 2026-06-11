# TASK-20260610 P3-4 子任务：RAG query runtime roles-first RBAC

Status: Done.

## Goal

把 RAG query runtime 从 legacy `userId-only` KB read 授权迁移到 roles-first 授权事实传递，覆盖 `/api/rag/query`、chat/tutor runtime 和 Orchestrator `RAG_QA`。

## Checklist

- [x] Create RED tests for controller role-fact forwarding.
- [x] Create RED tests for service role-aware query and requestId replay.
- [x] Add role-aware overloads in `RagQueryService`.
- [x] Update `ChatController`.
- [x] Update `TutorController`.
- [x] Update Orchestrator `RAG_QA` precheck and execution.
- [x] Add Orchestrator `RAG_QA` Bearer admin / subject-name role-confusion regression tests.
- [x] Run focused tests.
- [x] Run adjacent tests.
- [x] Run full backend tests.
- [x] Create evidence.
- [x] Create acceptance report.
- [x] Update changelog and memory.

## Allowed Files

- `backend/src/main/java/com/learningos/rag/api/ChatController.java`
- `backend/src/main/java/com/learningos/tutor/api/TutorController.java`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
- `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java`
- `backend/src/test/java/com/learningos/rag/api/ChatControllerTest.java`
- `backend/src/test/java/com/learningos/tutor/api/TutorControllerTest.java`
- `backend/src/test/java/com/learningos/rag/application/RagQueryServiceTest.java`
- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`
- `docs/requirements/REQ-20260610-p3-4-rag-query-runtime-roles-first-rbac.md`
- `docs/specs/SPEC-20260610-p3-4-rag-query-runtime-roles-first-rbac.md`
- `docs/plans/PLAN-20260610-p3-4-rag-query-runtime-roles-first-rbac.md`
- `docs/tasks/TASK-20260610-p3-4-rag-query-runtime-roles-first-rbac.md`
- `docs/context/CONTEXT-20260610-p3-4-rag-query-runtime-roles-first-rbac.md`
- `docs/subagents/runs/RUN-20260610-p3-4-rag-query-runtime-roles-first-rbac.md`
- `docs/evidence/EVIDENCE-20260610-p3-4-rag-query-runtime-roles-first-rbac.md`
- `docs/acceptance/ACCEPT-20260610-p3-4-rag-query-runtime-roles-first-rbac.md`
- `docs/retrospectives/RETRO-20260610-p3-4-rag-query-runtime-roles-first-rbac.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/skills/project-specific/auth-context-boundary.md`
- `docs/skills/project-specific/object-scope-authorization.md`

## Disallowed Files

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `.env` / secrets

## Test Commands

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ChatControllerTest,TutorControllerTest,RagQueryServiceTest test
mvn --% -Dtest=OrchestratorWorkflowControllerTest,ChatControllerTest,TutorControllerTest,RagQueryServiceTest,PermissionServiceTest test
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest,CourseKnowledgeControllerTest,AnalyticsControllerTest,AssessmentControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest test
mvn test
```

## Acceptance Criteria

- Bearer `ADMIN` runtime RAG query can use explicit admin facts despite spoofed `X-User-Id`.
- Bearer `USER sub=admin` does not gain admin runtime RAG query semantics.
- `RagQueryService` role-aware query and requestId replay paths use `PermissionService.requireReadableKbIds(userId, admin, teacher, kbIds)`.
- Orchestrator RAG_QA replay precheck and execution use the same role facts.
- No API/DTO/schema/dependency/frontend changes.

## Verification Result

- `mvn --% -Dtest=OrchestratorWorkflowControllerTest test`：30 run, 0 failures, 0 errors.
- `mvn --% -Dtest=OrchestratorWorkflowControllerTest,ChatControllerTest,TutorControllerTest,RagQueryServiceTest,PermissionServiceTest test`：60 run, 0 failures, 0 errors.
- `mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest,CourseKnowledgeControllerTest,AnalyticsControllerTest,AssessmentControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest test`：161 run, 0 failures, 0 errors.
- `mvn test`：509 run, 0 failures, 0 errors, 1 skipped.
