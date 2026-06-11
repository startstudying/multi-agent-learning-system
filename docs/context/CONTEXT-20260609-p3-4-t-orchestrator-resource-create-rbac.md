# CONTEXT - P3-4-T Orchestrator RESOURCE_GENERATION create roles-first RBAC

## 1. Current Task Boundary

本切片只处理：

- `POST /api/orchestrator/workflows` 的 `RESOURCE_GENERATION` create roles-first RBAC。
- `POST /api/orchestrator/workflows/{workflowId}/retry` 的 roles-first retry。
- 对应 controller integration tests。

## 2. Related Memory and Docs

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/specs/SPEC-20260608-real-auth-rbac-context.md`
- `docs/specs/SPEC-20260608-course-enrollment-scope.md`
- `docs/specs/SPEC-20260609-p3-4-s-learning-resource-create-rbac.md`
- `docs/specs/SPEC-20260606-orchestrator-resource-generation-context.md`
- `docs/specs/SPEC-20260606-orchestrator-failure-retry-policy.md`

## 3. Selected Skills

- `feature-development-workflow`
- `test-driven-development`
- `systematic-debugging`
- `verification-before-completion`
- `dispatching-parallel-agents`
- `spring-ai-agent-backend`
- `agent-trace-governance`
- `auth-context-boundary`
- `object-scope-authorization`

## 4. Subagent Plan

| Role | Report |
|---|---|
| Architect | `docs/subagents/runs/RUN-20260609-p3-4-t-orchestrator-resource-backend.md` |
| Security Reviewer | `docs/subagents/runs/RUN-20260609-p3-4-t-orchestrator-resource-security.md` |
| Test Engineer | `docs/subagents/runs/RUN-20260609-p3-4-t-orchestrator-resource-test.md` |
| Integration Reviewer | `docs/subagents/runs/RUN-20260609-p3-4-t-orchestrator-resource-integration-review.md` |

Implementation mode：Main Codex 单线程实现，避免多 agent 修改同一文件。

## 5. Files Allowed To Modify

- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`
- `backend/src/main/java/com/learningos/orchestrator/api/OrchestratorWorkflowController.java`
- `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java`
- `docs/subagents/runs/RUN-20260609-p3-4-t-orchestrator-resource-*.md`
- `docs/product/PRD-20260609-p3-4-t-orchestrator-resource-create-rbac.md`
- `docs/requirements/REQ-20260609-p3-4-t-orchestrator-resource-create-rbac.md`
- `docs/specs/SPEC-20260609-p3-4-t-orchestrator-resource-create-rbac.md`
- `docs/plans/PLAN-20260609-p3-4-t-orchestrator-resource-create-rbac.md`
- `docs/tasks/TASK-20260609-p3-4-t-orchestrator-resource-create-rbac.md`
- `docs/context/CONTEXT-20260609-p3-4-t-orchestrator-resource-create-rbac.md`
- `docs/evidence/EVIDENCE-20260609-p3-4-t-orchestrator-resource-create-rbac.md`
- `docs/acceptance/ACCEPT-20260609-p3-4-t-orchestrator-resource-create-rbac.md`
- `docs/retrospectives/RETRO-20260609-p3-4-t-orchestrator-resource-create-rbac.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 6. Files Not Allowed To Modify

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- formal OAuth2/JWK/Spring Security config
- broader class/course authorization matrix
- ResourceGeneration direct create behavior
- ResourceGeneration detail/trace/cancel/review RBAC

## 7. Test Commands

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=OrchestratorWorkflowControllerTest test
mvn --% -Dtest=OrchestratorWorkflowControllerTest,ResourceGenerationControllerTest,LearningWorkflowControllerTest,CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
mvn test
```

## 8. Boundary Notes

- ResourceGeneration 保持 owner-only，不开放 admin/teacher 代创建。
- 对 course enrollment denied，允许保留 Orchestrator failed workflow evidence；不允许 ResourceGeneration/model/token/citation 业务副作用。
- 不声明 P3-4 或 backend TODO 整体完成。

