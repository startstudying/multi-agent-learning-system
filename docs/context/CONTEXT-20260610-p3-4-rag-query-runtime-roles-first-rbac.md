# CONTEXT-20260610 P3-4 子任务：RAG query runtime roles-first RBAC

## Current TASK

`docs/tasks/TASK-20260610-p3-4-rag-query-runtime-roles-first-rbac.md`

## Related Memory and Docs

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/acceptance/ACCEPT-20260609-p3-4-p-rag-kb-rbac.md`
- `docs/evidence/EVIDENCE-20260609-p3-4-p-rag-kb-rbac.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/skills/project-specific/auth-context-boundary.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`

## Selected Skills

- feature-development-workflow
- rag-project-review
- object-scope-authorization
- auth-context-boundary
- test-generator
- subagent-driven-development

## Subagent Plan

| Expert | Status | Output |
|---|---|---|
| Agent/RAG Architect | Completed | 建议本轮限定为 RAG query runtime roles-first RBAC；KB-course lifecycle schema 另列 L 级后续 |
| Test Engineer | Completed | 建议补 Orchestrator RAG_QA Bearer admin / subject-name role-confusion 回归 |
| Security Reviewer | Completed | PASS：未发现本轮 RAG query runtime roles-first RBAC 阻塞问题 |

## Files Allowed To Modify

- `backend/src/main/java/com/learningos/rag/api/ChatController.java`
- `backend/src/main/java/com/learningos/tutor/api/TutorController.java`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
- `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java`
- `backend/src/test/java/com/learningos/rag/api/ChatControllerTest.java`
- `backend/src/test/java/com/learningos/tutor/api/TutorControllerTest.java`
- `backend/src/test/java/com/learningos/rag/application/RagQueryServiceTest.java`
- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`
- related workflow docs for this task
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/skills/project-specific/auth-context-boundary.md`
- `docs/skills/project-specific/object-scope-authorization.md`

## Files Not Allowed To Modify

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- secrets / `.env`

## Test Commands

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ChatControllerTest,TutorControllerTest,RagQueryServiceTest test
mvn --% -Dtest=OrchestratorWorkflowControllerTest,ChatControllerTest,TutorControllerTest,RagQueryServiceTest,PermissionServiceTest test
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest,CourseKnowledgeControllerTest,AnalyticsControllerTest,AssessmentControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest test
mvn test
```

## Current Boundary

Do only runtime roles-first fact propagation. Do not implement KB-course lifecycle schema or SSE production auth strategy in this task.

## Closure

本 Context Pack 已完成。实际修改未触碰 `backend/pom.xml`、DB migration、frontend 或 secrets；KB-course binding schema/lifecycle governance、SSE production auth strategy 仍为后续任务。
