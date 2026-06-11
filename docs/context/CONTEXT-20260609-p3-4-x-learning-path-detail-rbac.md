# CONTEXT - P3-4-X LearningPath Detail Roles-First RBAC

## 1. Current Task Boundary

本切片只修复 `GET /api/learning-paths/{pathId}` detail 读取的 roles-first RBAC。

不处理：

- `POST /api/learning-paths` create 语义。
- `LearningWorkflowService.createPathForUser(String, CreateLearningPathRequest)` legacy cleanup。
- ResourceGeneration / Agent Trace / Review Gate。
- formal OAuth2/JWK/Spring Security。
- broader class/course authorization matrix。

## 2. Related Memory and Docs

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/skills/project-specific/auth-context-boundary.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/specs/SPEC-20260609-p3-4-s-learning-resource-create-rbac.md`
- `docs/specs/SPEC-20260609-p3-4-w-course-access-legacy-cleanup.md`

## 3. Selected Skills

- `feature-development-workflow`
- `auth-context-boundary`
- `object-scope-authorization`
- `test-driven-development`
- `security-review`
- `Confidence Check`

## 4. Subagent Plan

| Role | Report |
|---|---|
| Architect | `docs/subagents/runs/RUN-20260609-p3-4-x-learning-path-detail-rbac-architect.md` |
| Security Reviewer | `docs/subagents/runs/RUN-20260609-p3-4-x-learning-path-detail-rbac-security.md` |
| Test Engineer | `docs/subagents/runs/RUN-20260609-p3-4-x-learning-path-detail-rbac-test.md` |
| Integration Reviewer | `docs/subagents/runs/RUN-20260609-p3-4-x-learning-path-detail-rbac-integration-review.md` |

Implementation mode: Main Codex single implementation after L1 parallel analysis.

## 5. Files Allowed To Modify

- `backend/src/test/java/com/learningos/learning/api/LearningWorkflowControllerTest.java`
- `backend/src/main/java/com/learningos/learning/api/LearningPathController.java`
- `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java`
- P3-4-X workflow/evidence/acceptance/retro docs
- `docs/subagents/runs/RUN-20260609-p3-4-x-learning-path-detail-rbac-*.md`
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
- `CurrentUserService` / `DevAuthFilter`
- formal OAuth2/JWK/Spring Security config
- Agent/RAG/model provider runtime
- ResourceGeneration / Agent Trace / Review Gate code

## 7. Test Commands

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=LearningWorkflowControllerTest#learningPathDetailUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader+learningPathDetailRejectsBearerUserSubjectAdminRoleConfusion+learningPathDetailBearerAdminMissingPathReturnsNotFound+learningPathDetailRejectsBearerUserSubjectAdminMissingPathAsForbidden+learningPathDetailAllowsBearerOwnerDespiteSpoofedUserIdHeader+learningPathDetailRejectsBearerNonOwnerForeignPathAsSafeForbidden+learningPathDetailRejectsBearerNonOwnerMissingPathAsSafeForbidden test
mvn --% -Dtest=LearningWorkflowControllerTest test
mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest,AgentTraceControllerTest,LearningWorkflowServiceTest test
mvn test
```

## 8. Boundary Notes

- `GET` HTTP 主路径不得通过 `"admin".equals(currentUserId)` 获得 admin 语义。
- Bearer token 身份必须优先于 spoofed `X-User-Id`。
- P3-4-X 完成后，P3-4 仍保留 broader class/course authorization、formal OAuth2/JWK/Spring Security、broader penetration tests。

