# CONTEXT - P3-4-Q Analytics student summary roles-first RBAC

## 1. 当前任务边界

只修复 `GET /api/analytics/students/{learnerId}/summary?courseId=...` 的 course read legacy caller。目标是保留 Controller 传入的 role facts，并让 `AnalyticsService` 调用 role-aware `CourseAccessService.requireCourseRead(...)`。

## 2. Related Memory and Docs

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/specs/SPEC-20260608-real-auth-rbac-context.md`
- `docs/specs/SPEC-20260608-analytics-student-summary-course-scope.md`
- `docs/specs/SPEC-20260609-p3-4-m-course-access-roles-first-overload.md`
- `docs/specs/SPEC-20260609-p3-4-p-rag-kb-rbac.md`

## 3. Selected Skills

- `feature-development-workflow`
- `auth-context-boundary`
- `object-scope-authorization`
- `test-driven-development`
- `security-review`
- `test-generator`
- `architecture-drift-check`
- `verification-before-completion`

## 4. Subagent Plan

| Role | Report |
|---|---|
| Backend/Architect | `docs/subagents/runs/RUN-20260609-p3-4-q-analytics-student-summary-backend.md` |
| Security & Quality | `docs/subagents/runs/RUN-20260609-p3-4-q-analytics-student-summary-security.md` |
| Test Engineer | `docs/subagents/runs/RUN-20260609-p3-4-q-analytics-student-summary-test.md` |

执行策略：L1 parallel analysis 已完成；实现由 Main Codex 单线程完成。

## 5. Files Allowed To Modify

- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`
- `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java`
- `docs/product/PRD-20260609-p3-4-q-analytics-student-summary-rbac.md`
- `docs/requirements/REQ-20260609-p3-4-q-analytics-student-summary-rbac.md`
- `docs/specs/SPEC-20260609-p3-4-q-analytics-student-summary-rbac.md`
- `docs/plans/PLAN-20260609-p3-4-q-analytics-student-summary-rbac.md`
- `docs/tasks/TASK-20260609-p3-4-q-analytics-student-summary-rbac.md`
- `docs/context/CONTEXT-20260609-p3-4-q-analytics-student-summary-rbac.md`
- `docs/evidence/EVIDENCE-20260609-p3-4-q-analytics-student-summary-rbac.md`
- `docs/acceptance/ACCEPT-20260609-p3-4-q-analytics-student-summary-rbac.md`
- `docs/retrospectives/RETRO-20260609-p3-4-q-analytics-student-summary-rbac.md`
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
- Assessment / GradingEvaluation / LearningPath / ResourceGeneration code。
- Formal OAuth2/JWK/Spring Security config。

## 7. Test Commands

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AnalyticsControllerTest test
mvn --% -Dtest=AnalyticsControllerTest,CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
mvn test
```

## 8. Current Task Boundary

本任务不处理 P3-4 剩余全部事项。完成后仍需保留：

- Assessment / GradingEvaluation legacy role inference follow-up。
- LearningPath / ResourceGeneration course-bound create roles-first follow-up。
- Broader class/course permission matrix。
- Formal OAuth2/JWK/Spring Security。
