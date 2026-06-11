# CONTEXT-20260608 学生分析摘要课程范围权限收口

## 1. Current Task Boundary

只实现 `GET /api/analytics/students/{learnerId}/summary` 的 student / teacher / admin 课程范围权限与 course-scoped 聚合。不得改前端、DB migration、模型接入、RAG parser、assessment list/detail 行为。

## 2. Related Memory And Docs

- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/skills/project-specific/auth-context-boundary.md`
- `docs/specs/SPEC-20260608-analytics-student-summary-course-scope.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`

## 3. Selected Skills

- `feature-development-workflow`
- `object-scope-authorization`
- `auth-context-boundary`
- `spring-boot-architecture`
- `test-driven-development`
- `verification-before-completion`

## 4. Subagent Plan

| Subagent | Scope | Output |
|---|---|---|
| P3-2 RAG Parser/OCR Expert | Remaining parser/OCR gap analysis | `RUN-20260608-p3-remaining-rag-parser-ocr.md` |
| P3-3 Model Provider Expert | Real model provider slice analysis | `RUN-20260608-p3-remaining-model-provider.md` |
| P3-4 Security Matrix Expert | Remaining class/course RBAC analysis | `RUN-20260608-p3-remaining-security-matrix.md` |

本切片采用主 Codex 单线程实现，避免多个 agent 同时修改 `AnalyticsControllerTest` / `AnalyticsService`。

## 5. Allowed Files

- `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java`
- `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java`
- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`
- `docs/product/PRD-20260608-analytics-student-summary-course-scope.md`
- `docs/requirements/REQ-20260608-analytics-student-summary-course-scope.md`
- `docs/specs/SPEC-20260608-analytics-student-summary-course-scope.md`
- `docs/plans/PLAN-20260608-analytics-student-summary-course-scope.md`
- `docs/tasks/TASK-20260608-analytics-student-summary-course-scope.md`
- `docs/context/CONTEXT-20260608-analytics-student-summary-course-scope.md`
- `docs/evidence/EVIDENCE-20260608-analytics-student-summary-course-scope.md`
- `docs/acceptance/ACCEPT-20260608-analytics-student-summary-course-scope.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 6. Files Not Allowed

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- unrelated RAG parser / model provider / auth / assessment / resource implementation files

## 7. Test Commands

```powershell
cd backend
mvn --% -Dtest=AnalyticsControllerTest test
mvn --% -Dtest=AnalyticsControllerTest,CourseKnowledgeControllerTest,AssessmentControllerTest test
mvn test
```

## 8. Architecture Drift Checklist

| Check | Expected |
|---|---|
| Controller only delegates | PASS |
| Service owns authorization and aggregation scope | PASS |
| No new dependency | PASS |
| No schema drift | PASS |
| No frontend LLM/API key change | PASS |
