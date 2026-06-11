# CONTEXT-20260609 P3-4-M Course API / CourseAccessService roles-first overload

## 1. Current Task Boundary

本切片只修复 Course API / Knowledge Catalog 主路径的 roles-first overload：

- Course list/detail/knowledge graph read scope。
- Course create / chapter / knowledge point / dependency manage scope。
- `CourseAccessService` role-aware overload 与旧签名兼容。

不处理 formal OAuth2/JWK/Spring Security、不做全仓库 RBAC、不修改前端/DB/RAG/model/vector。

## 2. Related Memory And Docs

- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/skills/project-specific/auth-context-boundary.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/harness/TEST_COMMANDS.md`
- `docs/subagents/runs/RUN-20260609-p3-4-m-course-access-roles-first-security.md`
- `docs/subagents/runs/RUN-20260609-p3-4-m-course-access-roles-first-backend.md`
- `docs/subagents/runs/RUN-20260609-p3-4-m-course-access-roles-first-test.md`

## 3. Selected Skills

- `feature-development-workflow`
- `security-review`
- `object-scope-authorization`
- `auth-context-boundary`
- `spring-boot-architecture`
- `api-contract-design`
- `test-driven-development`
- `verification-before-completion`

## 4. Subagent Plan

| Subagent | Role | Output |
|---|---|---|
| Halley | Security & Quality | `docs/subagents/runs/RUN-20260609-p3-4-m-course-access-roles-first-security.md` |
| Anscombe | Backend Expert | `docs/subagents/runs/RUN-20260609-p3-4-m-course-access-roles-first-backend.md` |
| Bohr | Test Engineer | `docs/subagents/runs/RUN-20260609-p3-4-m-course-access-roles-first-test.md` |

Implementation mode: Single Codex implementation。

## 5. Files Allowed To Modify

Production:

- `backend/src/main/java/com/learningos/knowledge/api/CourseController.java`
- `backend/src/main/java/com/learningos/knowledge/api/KnowledgePointController.java`
- `backend/src/main/java/com/learningos/knowledge/application/CourseAccessService.java`
- `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java`

Tests:

- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`

Docs:

- 本切片 PRD / REQ / SPEC / PLAN / TASK / CONTEXT / Evidence / Acceptance / Retro。
- `docs/subagents/runs/RUN-20260609-p3-4-m-course-access-roles-first-backend.md`
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
mvn --% -Dtest=CourseKnowledgeControllerTest test
mvn --% -Dtest=CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest,AnalyticsControllerTest test
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
Focused:  mvn --% -Dtest=CourseKnowledgeControllerTest test
Result:   20 run, 0 failures, 0 errors, 0 skipped

Adjacent: mvn --% -Dtest=CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest,AnalyticsControllerTest test
Result:   63 run, 0 failures, 0 errors, 0 skipped

Full:     mvn test
Result:   403 run, 0 failures, 0 errors, 1 skipped
```
