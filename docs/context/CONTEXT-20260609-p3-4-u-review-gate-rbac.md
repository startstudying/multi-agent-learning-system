# CONTEXT - P3-4-U Review Gate ResourceReview roles-first RBAC

## 1. Current Task Boundary

本切片只处理：

- `GET /api/reviews/resources`
- `POST /api/reviews/resources/{reviewId}/decision`
- 对应 controller integration tests。

## 2. Related Memory and Docs

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/specs/SPEC-20260607-review-gate-course-scope-hardening.md`
- `docs/specs/SPEC-20260608-real-auth-rbac-context.md`
- `docs/specs/SPEC-20260609-p3-4-k-permission-matrix.md`

## 3. Selected Skills

- `feature-development-workflow`
- `test-driven-development`
- `verification-before-completion`
- `spring-ai-agent-backend`
- `auth-context-boundary`
- `object-scope-authorization`

## 4. Subagent Plan

| Role | Report |
|---|---|
| Architect | `docs/subagents/runs/RUN-20260609-p3-4-u-review-gate-rbac-architecture.md` |
| Security Reviewer | `docs/subagents/runs/RUN-20260609-p3-4-u-review-gate-rbac-security.md` |
| Test Engineer | `docs/subagents/runs/RUN-20260609-p3-4-u-review-gate-rbac-test.md` |
| Integration Reviewer | `docs/subagents/runs/RUN-20260609-p3-4-u-review-gate-rbac-integration-review.md` |

Implementation mode：Main Codex 单线程实现，避免多 agent 修改同一文件。

## 5. Files Allowed To Modify

- `backend/src/test/java/com/learningos/agent/api/ResourceReviewControllerTest.java`
- `backend/src/main/java/com/learningos/agent/api/ResourceReviewController.java`
- `backend/src/main/java/com/learningos/agent/application/ReviewGovernanceService.java`
- `docs/subagents/runs/RUN-20260609-p3-4-u-review-gate-rbac-*.md`
- `docs/product/PRD-20260609-p3-4-u-review-gate-rbac.md`
- `docs/requirements/REQ-20260609-p3-4-u-review-gate-rbac.md`
- `docs/specs/SPEC-20260609-p3-4-u-review-gate-rbac.md`
- `docs/plans/PLAN-20260609-p3-4-u-review-gate-rbac.md`
- `docs/tasks/TASK-20260609-p3-4-u-review-gate-rbac.md`
- `docs/context/CONTEXT-20260609-p3-4-u-review-gate-rbac.md`
- `docs/evidence/EVIDENCE-20260609-p3-4-u-review-gate-rbac.md`
- `docs/acceptance/ACCEPT-20260609-p3-4-u-review-gate-rbac.md`
- `docs/retrospectives/RETRO-20260609-p3-4-u-review-gate-rbac.md`
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
- ResourceGeneration create/detail/trace/cancel
- Agent Trace governance
- CourseAccessService legacy cleanup

## 7. Test Commands

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ResourceReviewControllerTest test
mvn --% -Dtest=ResourceReviewControllerTest,ReviewGovernanceServiceTest,ResourceGenerationControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
mvn test
```

## 8. Boundary Notes

- HTTP path 必须 roles-first。
- Legacy service overload 保留兼容，不在本切片删除。
- 不声明 P3-4 或总 TODO 完成；broader class/course、formal OAuth2/JWK/Spring Security、broader penetration tests、P3-2 工业级解析仍未完成。
