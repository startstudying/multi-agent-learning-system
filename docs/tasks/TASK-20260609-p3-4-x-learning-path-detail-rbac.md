# TASK - P3-4-X LearningPath Detail Roles-First RBAC

## 1. Traceability

- PLAN: `docs/plans/PLAN-20260609-p3-4-x-learning-path-detail-rbac.md`
- SPEC: `docs/specs/SPEC-20260609-p3-4-x-learning-path-detail-rbac.md`

## 2. Goal

修复 `GET /api/learning-paths/{pathId}` 的 subject-name admin role-confusion，使 LearningPath detail 读取使用 explicit `ADMIN` role fact。

## 3. Files Allowed To Modify

- `backend/src/test/java/com/learningos/learning/api/LearningWorkflowControllerTest.java`
- `backend/src/main/java/com/learningos/learning/api/LearningPathController.java`
- `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java`
- `docs/subagents/runs/RUN-20260609-p3-4-x-learning-path-detail-rbac-*.md`
- `docs/product/PRD-20260609-p3-4-x-learning-path-detail-rbac.md`
- `docs/requirements/REQ-20260609-p3-4-x-learning-path-detail-rbac.md`
- `docs/specs/SPEC-20260609-p3-4-x-learning-path-detail-rbac.md`
- `docs/plans/PLAN-20260609-p3-4-x-learning-path-detail-rbac.md`
- `docs/tasks/TASK-20260609-p3-4-x-learning-path-detail-rbac.md`
- `docs/context/CONTEXT-20260609-p3-4-x-learning-path-detail-rbac.md`
- `docs/evidence/EVIDENCE-20260609-p3-4-x-learning-path-detail-rbac.md`
- `docs/acceptance/ACCEPT-20260609-p3-4-x-learning-path-detail-rbac.md`
- `docs/retrospectives/RETRO-20260609-p3-4-x-learning-path-detail-rbac.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 4. Files Not Allowed To Modify

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- `CurrentUserService` / `DevAuthFilter`
- formal OAuth2/JWK/Spring Security config
- Agent/RAG/model provider runtime
- ResourceGeneration / Agent Trace / Review Gate code
- `POST /api/learning-paths` business semantics

## 5. Implementation Steps

1. 在 `LearningWorkflowControllerTest` 新增 RED tests。
2. 运行 focused RED，确认测试因现有 subject-name admin 推断失败。
3. 修改 `LearningPathController.get(...)`，读取 `UserContext` 并传 explicit admin fact。
4. 修改 `LearningWorkflowService`，新增 roles-first `getPathForUser(currentUserId, currentUserAdmin, pathId)`。
5. 确保 HTTP GET 主路径不再调用 subject-name admin 判断。
6. 运行 focused、adjacent、full backend tests。
7. 写 Evidence / Acceptance / Retro，更新 Changelog / Memory / TODO。

## 6. Test Commands

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=LearningWorkflowControllerTest#learningPathDetailUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader+learningPathDetailRejectsBearerUserSubjectAdminRoleConfusion+learningPathDetailBearerAdminMissingPathReturnsNotFound+learningPathDetailRejectsBearerUserSubjectAdminMissingPathAsForbidden+learningPathDetailAllowsBearerOwnerDespiteSpoofedUserIdHeader+learningPathDetailRejectsBearerNonOwnerForeignPathAsSafeForbidden+learningPathDetailRejectsBearerNonOwnerMissingPathAsSafeForbidden test
mvn --% -Dtest=LearningWorkflowControllerTest test
mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest,AgentTraceControllerTest,LearningWorkflowServiceTest test
mvn test
```

## 7. Done Criteria

- [x] PRD/REQ/SPEC/PLAN/TASK/CONTEXT 存在。
- [x] RED 失败已观察并记录。
- [x] `GET /api/learning-paths/{pathId}` 使用 explicit admin role fact。
- [x] Bearer `ADMIN sub=ops_admin` foreign detail 与 missing 语义正确。
- [x] Bearer `USER sub=admin` foreign/missing 均按 non-admin 语义拒绝。
- [x] owner 与 non-owner anti-enumeration 回归通过。
- [x] focused/adjacent/full tests 已运行或限制已说明。
- [x] Evidence / Acceptance / Retro 已创建。
- [x] Changelog / Memory / TODO 已更新。

## 8. Status

Done。P3-4-X 已完成；P3-4 总项仍未全部完成。
