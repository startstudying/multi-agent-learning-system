# TASK-20260608 P3-4-I 真实认证上下文与 RBAC 兼容层

Status: Done

## Done Criteria

- [x] Spec-First 文档已创建。
- [x] 三个专家 subagent 报告已集成。
- [x] Bearer JWT 可以建立 `UserContext`。
- [x] invalid Bearer JWT 返回 `UNAUTHORIZED`，不 fallback 到 `X-User-Id`。
- [x] prod/staging/production 无 token 返回 `UNAUTHORIZED`。
- [x] valid token + spoofed `X-User-Id` 使用 token 身份。
- [x] dev/test 无 token 时继续兼容 `X-User-Id`。
- [x] `CurrentUserService.isAdmin()` / `isTeacherUser()` 优先基于 roles。
- [x] 不新增 Maven 依赖、不新增 DB migration、不修改 frontend。
- [x] P3-4-C..H 关键回归测试通过。
- [x] Evidence / Acceptance / Memory / Changelog / TODO / Skill 已更新。

## Allowed Files

- `backend/src/main/java/com/learningos/LearningOsApplication.java`
- `backend/src/main/java/com/learningos/config/AppProperties.java`
- `backend/src/main/java/com/learningos/config/AuthProperties.java`
- `backend/src/main/java/com/learningos/common/auth/**`
- `backend/src/main/resources/application.yml`
- `backend/src/test/java/com/learningos/common/auth/**`
- `backend/src/test/java/com/learningos/common/trace/StructuredRequestLoggingFilterTest.java`
- `docs/evidence/EVIDENCE-20260608-real-auth-rbac-context.md`
- `docs/acceptance/ACCEPT-20260608-real-auth-rbac-context.md`
- `docs/retrospectives/RETRO-20260608-real-auth-rbac-context.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/skills/project-specific/auth-context-boundary.md`
- `docs/skills/SKILL_REGISTRY.md`

## Files Not Allowed To Modify

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- RAG parser / model provider / assessment / resource / course business service implementation files

## Test Commands

```powershell
cd backend
mvn --% -Dtest=DevAuthFilterTest,CurrentUserServiceTest test
mvn --% -Dtest=DevAuthFilterTest,CurrentUserServiceTest,StructuredRequestLoggingFilterTest,CourseKnowledgeControllerTest,AssessmentControllerTest,DocumentControllerTest test
mvn test
```

## Verification Results

- RED observed: first focused run failed at compilation because `AuthProperties`, `CurrentUserService(AppProperties)`, and `currentUser()` did not exist.
- Focused: `13` tests, `0` failures, `0` errors, `0` skipped.
- Adjacent: `74` tests, `0` failures, `0` errors, `0` skipped.
- Full backend: `345` tests, `0` failures, `0` errors, `1` skipped.
