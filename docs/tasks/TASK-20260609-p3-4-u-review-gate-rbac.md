# TASK - P3-4-U Review Gate ResourceReview roles-first RBAC

## 1. Traceability

- PLAN: `docs/plans/PLAN-20260609-p3-4-u-review-gate-rbac.md`
- SPEC: `docs/specs/SPEC-20260609-p3-4-u-review-gate-rbac.md`

## 2. Goal

关闭 `GET /api/reviews/resources` 与 `POST /api/reviews/resources/{reviewId}/decision` 的 legacy subject-name role-confusion，让 HTTP 主路径只信任 Bearer `UserContext.roles()`。

## 3. Files Allowed To Modify

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

## 4. Files Not Allowed To Modify

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- formal OAuth2/JWK/Spring Security config
- ResourceGeneration create/detail/trace/cancel behavior
- Agent Trace governance
- CourseAccessService legacy cleanup

## 5. Implementation Steps

1. 在 `ResourceReviewControllerTest` 加 JWT helper 和 Bearer role-confusion tests。
2. 运行 `mvn --% -Dtest=ResourceReviewControllerTest test` 观察 RED。
3. `ResourceReviewController` 读取 `currentUser()` 并传 `userId/admin/teacher` facts。
4. `ReviewGovernanceService` 新增 roles-first list/decision overload，HTTP path 不再回落 subject-name inference。
5. 运行 focused、adjacent、full tests。
6. 写 Evidence / Acceptance / Retro，更新 Changelog / Memory / TODO。

## 6. Test Commands

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ResourceReviewControllerTest test
mvn --% -Dtest=ResourceReviewControllerTest,ReviewGovernanceServiceTest,ResourceGenerationControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
mvn test
```

## 7. Done Criteria

- [x] PRD/REQ/SPEC/PLAN/TASK/CONTEXT 存在。
- [x] RED 失败已观察并记录。
- [x] Review controller 传 roles-first facts。
- [x] Review service roles-first overload 不从 subject 推断 admin/teacher。
- [x] Bearer admin spoofed header 成功。
- [x] Bearer teacher no-prefix own-course 成功。
- [x] Bearer `USER sub=admin` 和 `USER sub=teacher_1` 被拒绝。
- [x] focused/adjacent/full tests 已运行或限制已说明。
- [x] Evidence / Acceptance / Retro 已创建。
- [x] Changelog / Memory / TODO 已更新。

## 8. Status

Done。
