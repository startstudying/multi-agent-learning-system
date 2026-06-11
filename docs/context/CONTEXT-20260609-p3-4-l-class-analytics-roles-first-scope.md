# CONTEXT-20260609 P3-4-L class analytics roles-first course scope

## 1. Current Task Boundary

本切片只修复 `GET /api/analytics/classes/{courseId}/summary` roles-first 授权与 non-admin missing/foreign class course anti-enumeration。

不处理 formal OAuth2/JWK/Spring Security、不新增 class domain、不迁移完整 `CourseAccessService`、不修改前端/DB/RAG/model/vector。

## 2. Related Memory And Docs

- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/skills/project-specific/auth-context-boundary.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/harness/TEST_COMMANDS.md`

## 3. Selected Skills

- `feature-development-workflow`
- `security-review`
- `object-scope-authorization`
- `auth-context-boundary`
- `test-driven-development`
- `verification-before-completion`

## 4. Subagent Plan

| Subagent | Role | Output |
|---|---|---|
| Existing Security Agent | Security & Quality | `docs/subagents/runs/RUN-20260609-p3-4-l-class-analytics-roles-first-scope.md` |
| Existing Backend Agent | Backend Expert | same run report |
| Existing Test Agent | Test Engineer | same run report |

Implementation mode: Single Codex implementation.

## 5. Files Allowed To Modify

Production:

- `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java`
- `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java`

Tests:

- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`

Docs:

- `docs/product/PRD-20260609-p3-4-l-class-analytics-roles-first-scope.md`
- `docs/requirements/REQ-20260609-p3-4-l-class-analytics-roles-first-scope.md`
- `docs/specs/SPEC-20260609-p3-4-l-class-analytics-roles-first-scope.md`
- `docs/plans/PLAN-20260609-p3-4-l-class-analytics-roles-first-scope.md`
- `docs/tasks/TASK-20260609-p3-4-l-class-analytics-roles-first-scope.md`
- `docs/context/CONTEXT-20260609-p3-4-l-class-analytics-roles-first-scope.md`
- `docs/evidence/EVIDENCE-20260609-p3-4-l-class-analytics-roles-first-scope.md`
- `docs/acceptance/ACCEPT-20260609-p3-4-l-class-analytics-roles-first-scope.md`
- `docs/retrospectives/RETRO-20260609-p3-4-l-class-analytics-roles-first-scope.md`
- `docs/subagents/runs/RUN-20260609-p3-4-l-class-analytics-roles-first-scope.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 6. Files Not Allowed To Modify

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- RAG parser/OCR/vector/model provider files。

## 7. Test Commands

```powershell
cd backend
mvn --% -Dtest=AnalyticsControllerTest test
mvn --% -Dtest=AnalyticsControllerTest,CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
mvn test
```

## 8. Architecture Drift Checklist

| Check | Expected |
|---|---|
| Backend layering | PASS |
| Frontend rules | PASS |
| Agent / RAG rules | PASS |
| Security | PASS |
| API / Database | PASS |

## 9. Verification Evidence

```text
Focused:  mvn --% -Dtest=AnalyticsControllerTest test
Result:   29 run, 0 failures, 0 errors, 0 skipped

Adjacent: mvn --% -Dtest=AnalyticsControllerTest,CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
Result:   56 run, 0 failures, 0 errors, 0 skipped

Full:     mvn test
Result:   396 run, 0 failures, 0 errors, 1 skipped
```
