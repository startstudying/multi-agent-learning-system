# CONTEXT - P3-4-V ResourceGeneration / Agent Trace detail roles-first RBAC

## 1. Current Task Boundary

本切片只处理：

- `GET /api/resources/generation-tasks/{taskId}`
- `GET /api/agent/resource-generation/tasks/{taskId}/learner-resources`
- `GET /api/agent/tasks/{taskId}/trace`
- `GET /api/agent/traces`

`POST /api/agent/tasks/{taskId}/cancel` 不开放 admin cancel；仅通过 adjacent tests 防止 owner-only 行为回归。

## 2. Related Memory and Docs

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/specs/SPEC-20260608-real-auth-rbac-context.md`
- `docs/specs/SPEC-20260609-p3-4-k-permission-matrix.md`
- `docs/specs/SPEC-20260609-p3-4-s-learning-resource-create-rbac.md`
- `docs/specs/SPEC-20260609-p3-4-t-orchestrator-resource-create-rbac.md`
- `docs/specs/SPEC-20260609-p3-4-u-review-gate-rbac.md`

## 3. Selected Skills

- `feature-development-workflow`
- `test-driven-development`
- `spring-ai-agent-backend`
- `auth-context-boundary`
- `object-scope-authorization`
- `agent-trace-governance`

## 4. Subagent Plan

| Role | Report |
|---|---|
| Architect | `docs/subagents/runs/RUN-20260609-p3-4-v-resource-trace-detail-rbac-architecture.md` |
| Security Reviewer | `docs/subagents/runs/RUN-20260609-p3-4-v-resource-trace-detail-rbac-security.md` |
| Test Engineer | `docs/subagents/runs/RUN-20260609-p3-4-v-resource-trace-detail-rbac-test.md` |
| Integration Reviewer | `docs/subagents/runs/RUN-20260609-p3-4-v-resource-trace-detail-rbac-integration-review.md` |

Implementation mode: Main Codex 单线程实现，避免多个 agent 修改同一文件。

## 5. Files Allowed To Modify

- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`
- `backend/src/test/java/com/learningos/agent/api/AgentTraceControllerTest.java`
- `backend/src/main/java/com/learningos/agent/api/ResourceGenerationController.java`
- `backend/src/main/java/com/learningos/agent/api/AgentTraceController.java`
- `backend/src/main/java/com/learningos/agent/api/AgentTraceGovernanceController.java`
- `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java`
- `backend/src/main/java/com/learningos/agent/application/AgentTraceGovernanceService.java`
- P3-4-V workflow/evidence/acceptance/retro docs
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
- Agent/RAG/model provider runtime
- Review Gate completed paths
- CourseAccessService legacy cleanup

## 7. Test Commands

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ResourceGenerationControllerTest#resourceGenerationDetailUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader+resourceGenerationDetailRejectsBearerUserSubjectAdminRoleConfusion+resourceGenerationDetailBearerAdminMissingTaskReturnsNotFound+learnerResourcesRejectsBearerUserSubjectAdminMissingTaskAsForbidden,AgentTraceControllerTest#traceGovernanceSearchUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader+traceGovernanceSearchRejectsBearerUserSubjectAdminRoleConfusion+traceDetailUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader+traceDetailRejectsBearerUserSubjectAdminRoleConfusion+traceDetailBearerAdminMissingTaskReturnsNotFound test
mvn --% -Dtest=ResourceGenerationControllerTest,AgentTraceControllerTest,ResourceReviewControllerTest,OrchestratorWorkflowControllerTest,AnalyticsControllerTest test
mvn test
```

## 8. Boundary Notes

- HTTP path 必须 roles-first。
- Legacy service overload 保留兼容，不在本切片删除。
- 不声称 P3-4 或总 TODO 完成；P3-2 工业级 layout、CourseAccessService legacy cleanup、broader class/course、formal OAuth2/JWK/Spring Security 和 broader penetration tests 仍待继续。
