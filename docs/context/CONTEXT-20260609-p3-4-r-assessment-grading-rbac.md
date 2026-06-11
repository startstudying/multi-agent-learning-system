# CONTEXT - P3-4-R Assessment / GradingEvaluation roles-first RBAC

## 1. Current Task Boundary

本切片只处理 Assessment read paths 与 GradingEvaluation HTTP path 的 roles-first RBAC 迁移。

## 2. Related Memory and Docs

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/specs/SPEC-20260608-real-auth-rbac-context.md`
- `docs/specs/SPEC-20260608-assessment-record-rbac.md`
- `docs/specs/SPEC-20260608-assessment-record-list-rbac.md`
- `docs/specs/SPEC-20260608-grading-evaluation-course-scope.md`
- `docs/specs/SPEC-20260609-p3-4-m-course-access-roles-first-overload.md`
- `docs/specs/SPEC-20260609-p3-4-r-assessment-grading-rbac.md`

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
| Backend/Architect | `docs/subagents/runs/RUN-20260609-p3-4-r-assessment-grading-backend.md` |
| Security & Quality | `docs/subagents/runs/RUN-20260609-p3-4-r-assessment-grading-security.md` |
| Test Engineer | `docs/subagents/runs/RUN-20260609-p3-4-r-assessment-grading-test.md` |

Implementation mode：Main Codex 单线实现，避免多个 agent 修改同一文件。

## 5. Files Allowed To Modify

- `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java`
- `backend/src/main/java/com/learningos/assessment/api/AssessmentController.java`
- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java`
- `backend/src/main/java/com/learningos/assessment/application/GradingEvaluationService.java`
- `docs/subagents/runs/RUN-20260609-p3-4-r-assessment-grading-backend.md`
- `docs/subagents/runs/RUN-20260609-p3-4-r-assessment-grading-security.md`
- `docs/subagents/runs/RUN-20260609-p3-4-r-assessment-grading-test.md`
- `docs/product/PRD-20260609-p3-4-r-assessment-grading-rbac.md`
- `docs/requirements/REQ-20260609-p3-4-r-assessment-grading-rbac.md`
- `docs/specs/SPEC-20260609-p3-4-r-assessment-grading-rbac.md`
- `docs/plans/PLAN-20260609-p3-4-r-assessment-grading-rbac.md`
- `docs/tasks/TASK-20260609-p3-4-r-assessment-grading-rbac.md`
- `docs/context/CONTEXT-20260609-p3-4-r-assessment-grading-rbac.md`
- `docs/evidence/EVIDENCE-20260609-p3-4-r-assessment-grading-rbac.md`
- `docs/acceptance/ACCEPT-20260609-p3-4-r-assessment-grading-rbac.md`
- `docs/retrospectives/RETRO-20260609-p3-4-r-assessment-grading-rbac.md`
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

## 7. Test Commands

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AssessmentControllerTest test
mvn --% -Dtest=AssessmentControllerTest,GradingEvaluationServiceTest,CourseKnowledgeControllerTest,EvaluationSetControllerTest,EvaluationRunControllerTest,AnalyticsControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
mvn test
```

## 8. Boundary Notes

- 不改变 `POST /api/assessment/answers` 的 owner-only submit 语义。
- 不删除公共 legacy `CourseAccessService` overload。
- 不声明 P3-4 或 backend architecture TODO 整体完成。
