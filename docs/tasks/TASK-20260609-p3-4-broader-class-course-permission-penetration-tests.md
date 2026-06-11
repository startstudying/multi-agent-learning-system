# TASK-20260609 P3-4 子任务：broader class/course permission penetration tests

Status: Completed for this S Fast Lane slice.

## 目标

在不扩大认证架构和不改 API 合同的前提下，补齐 class/course 权限的高风险穿透测试，重点防止：

- `USER sub=teacher_1` 通过 subject-name 前缀获得教师权限。
- Bearer `TEACHER` 身份被 spoofed `X-User-Id` 改写。
- class analytics 从 learning path / wrong question / resource task 等历史业务信号反推班级成员。

## Task Type

Test / security regression。

## Skill Selection Report

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 继续推进 backend TODO 父计划。 |
| `subagent-driven-development` | 用户要求专家 subagent 并行开发。 |
| `security-review` | 本任务扩展权限渗透测试。 |
| `object-scope-authorization` | 课程、班级、答题记录都属于对象级授权。 |
| `auth-context-boundary` | 角色判断必须来自 trusted role facts，不能来自 subject/header。 |
| `test-driven-development` | 通过新增 MockMvc penetration tests 固化回归。 |
| `verification-before-completion` | 完成前需要 focused / adjacent / full 验证证据。 |

Missing Skills: 无。

GitHub Research Needed: No。

New Project-Specific Skill To Create: 暂不需要。

## Size Classification

- Size: S - Small Slice / Fast Lane
- Reason: 仅扩展现有测试类的权限穿透覆盖；不改 REST API、DTO、schema、依赖、前端合同或正式认证架构。
- Required Documents: 本 mini TASK，内嵌 Context Pack；完成后使用合并 Evidence/Acceptance。
- Can Skip: 独立 PRD/REQ/SPEC/PLAN/CONTEXT、独立 Retro。
- Upgrade Trigger: 如果新增测试暴露跨 Controller/Service、认证架构或多个 production files 的授权缺口，则升级为 M；本轮 RED 只需要 1 个 `AnalyticsController` HTTP 入口对齐 trusted role facts，保持 S 并在 Evidence 中记录。

## Subagent Plan

用户明确要求专家 subagent 并行开发，本 S 切片启用 L1 并行分析：

| Expert | Scope | Expected Output |
|---|---|---|
| Architect | 最小测试边界与升级条件 | 本切片边界和不触碰范围 |
| Security Reviewer | Broken Access Control 风险点 | 高风险渗透测试点 |
| Test Engineer | 最小 MockMvc 测试设计 | 测试类、方法名和命令 |

主 Codex 负责测试实现、集成和验证。

## Context Pack

### Related Memory and Docs

- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/skills/project-specific/auth-context-boundary.md`
- `docs/skills/project-specific/object-scope-authorization.md`

### Allowed Files

- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`
- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`
- `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java`
- `docs/tasks/TASK-20260609-p3-4-broader-class-course-permission-penetration-tests.md`
- `docs/evidence/EVIDENCE-20260609-p3-4-broader-class-course-permission-penetration-tests.md`
- `docs/subagents/runs/RUN-20260609-p3-4-broader-class-course-permission-penetration-tests-*.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

### Disallowed Files

- `frontend/**`
- `backend/pom.xml`
- database migration files
- other production Java files
- formal OAuth2/JWK/Spring Security configuration

## Planned Test Commands

Focused:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseKnowledgeControllerTest,AnalyticsControllerTest test
```

Adjacent:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseKnowledgeControllerTest,AnalyticsControllerTest,CourseAccessServiceTest,AnalyticsServiceTest,DevAuthFilterTest,CurrentUserServiceTest test
```

Full backend:

```powershell
cd D:\多元agent\backend
mvn test
```

## Acceptance Criteria

- [x] Bearer `USER sub=teacher_1` cannot create a course for self through subject-name role confusion.
- [x] Bearer `USER sub=teacher_1` cannot create a knowledge point in a course whose `teacherId` equals the token subject.
- [x] Bearer `USER sub=teacher_1` cannot read class summary for a subject-owned course.
- [x] Bearer `TEACHER` class summary path ignores spoofed `X-User-Id` and counts only active enrolled learners.
- [x] Never-enrolled and dropped learners with learning path / wrong question / resource task signals are not counted in class analytics.
- [x] No REST API, DTO, DB, dependency, frontend, or formal OAuth2/JWK/Spring Security change; one RED-driven `AnalyticsController` roles-facts fix was applied and documented.
- [x] Parent P3-4 remains not fully complete unless broader class/course and formal OAuth2/JWK/Spring Security items are completed separately.

## Implementation Notes

- Added `CourseKnowledgeControllerTest.bearerUserSubjectTeacherPrefixCannotCreateCourseForSelf`.
- Added `CourseKnowledgeControllerTest.bearerUserSubjectTeacherPrefixCannotCreateKnowledgePointInSubjectOwnedCourse`.
- Added `AnalyticsControllerTest.analyticsAdminOnlyEndpointsRejectBearerUserSubjectAdminRoleConfusion`.
- Added `AnalyticsControllerTest.bearerUserSubjectTeacherPrefixCannotReadClassSummaryForSubjectOwnedCourse`.
- Added `AnalyticsControllerTest.bearerTeacherClassSummaryIgnoresLegacySignalsForDroppedAndNeverEnrolledLearners`.
- RED exposed that `AnalyticsController` HTTP role checks still used `CurrentUserService.isAdmin()` / `isTeacherUser()` subject-name fallback. The controller now derives admin/teacher facts from `UserContext.roles()` for analytics HTTP entrypoints.

## Evidence / Acceptance

Combined Evidence/Acceptance: `docs/evidence/EVIDENCE-20260609-p3-4-broader-class-course-permission-penetration-tests.md`.

Verification summary:

- RED compile guard observed: missing `assertThat` import after new body assertions; fixed with static import.
- RED authorization gap observed: `bearerUserSubjectTeacherPrefixCannotReadClassSummaryForSubjectOwnedCourse` expected `403` but got `200`; fixed by role facts from `UserContext.roles()`.
- Focused: `mvn --% -Dtest=CourseKnowledgeControllerTest,AnalyticsControllerTest test` -> `59 run, 0 failures`.
- Adjacent: `mvn --% -Dtest=CourseKnowledgeControllerTest,AnalyticsControllerTest,CourseAccessServiceTest,AnalyticsServiceTest,DevAuthFilterTest,CurrentUserServiceTest test` -> `82 run, 0 failures`.
- Full backend: `mvn test` -> `487 run, 0 failures, 0 errors, 1 skipped`.

Subagent review:

- Architect: CONDITIONAL before closure; PASS for S slice boundary after Evidence/Acceptance is added.
- Security Reviewer: CONDITIONAL before closure; PASS for named high-risk penetration cases.
- Test Engineer: CONDITIONAL before closure; PASS for test design after Evidence/Acceptance is added.
