# EVIDENCE-20260609 P3-4 子任务：broader class/course permission penetration tests

## Verdict

Accepted for this S Fast Lane child task.

P3-4 parent remains open: broader class/course authorization and formal OAuth2/JWK/Spring Security still need separate follow-up work.

## Scope

本轮只补齐 class/course 权限的高风险穿透测试，并修复测试暴露的一处 analytics HTTP 授权入口缺口。

In scope:

- Course / knowledge catalog subject-name role-confusion penetration tests.
- Analytics class summary subject-name role-confusion penetration tests.
- Bearer identity precedence over spoofed `X-User-Id`.
- Class analytics active enrollment boundary.

Out of scope:

- REST API contract change.
- Request/response DTO change.
- Database migration.
- Dependency change.
- Frontend change.
- Formal OAuth2/JWK/Spring Security migration.
- Declaring P3-4 fully complete.

## Files Changed

Production:

- `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java`

Tests:

- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`
- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`

Workflow and evidence:

- `docs/tasks/TASK-20260609-p3-4-broader-class-course-permission-penetration-tests.md`
- `docs/evidence/EVIDENCE-20260609-p3-4-broader-class-course-permission-penetration-tests.md`
- `docs/subagents/runs/RUN-20260609-p3-4-broader-class-course-permission-penetration-tests-architect.md`
- `docs/subagents/runs/RUN-20260609-p3-4-broader-class-course-permission-penetration-tests-security.md`
- `docs/subagents/runs/RUN-20260609-p3-4-broader-class-course-permission-penetration-tests-test.md`
- `docs/subagents/runs/RUN-20260609-p3-4-broader-class-course-permission-penetration-tests-integration-review.md`

Memory / planning:

- `docs/changelog/CHANGELOG.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## RED Evidence

1. Compile RED:
   - Command: `mvn --% -Dtest=CourseKnowledgeControllerTest,AnalyticsControllerTest test`
   - Result: compilation failed because new AssertJ body assertions in `AnalyticsControllerTest` needed `import static org.assertj.core.api.Assertions.assertThat;`.
   - Fix: added the missing static import.

2. Authorization RED:
   - Test: `AnalyticsControllerTest.bearerUserSubjectTeacherPrefixCannotReadClassSummaryForSubjectOwnedCourse`
   - Expected: `403 FORBIDDEN`
   - Actual: `200 OK`
   - Root cause: `AnalyticsController.teacherClassSummary` still derived teacher semantics through `CurrentUserService.isTeacherUser()`, which allows test/dev subject-name fallback such as `sub=teacher_1`.
   - Fix: `AnalyticsController` now derives admin/teacher facts from `UserContext.roles()` and passes those explicit facts to `AnalyticsService`.

## Implementation Summary

- Added `CourseKnowledgeControllerTest.bearerUserSubjectTeacherPrefixCannotCreateCourseForSelf`.
- Added `CourseKnowledgeControllerTest.bearerUserSubjectTeacherPrefixCannotCreateKnowledgePointInSubjectOwnedCourse`.
- Added `AnalyticsControllerTest.analyticsAdminOnlyEndpointsRejectBearerUserSubjectAdminRoleConfusion`.
- Added `AnalyticsControllerTest.bearerUserSubjectTeacherPrefixCannotReadClassSummaryForSubjectOwnedCourse`.
- Added `AnalyticsControllerTest.bearerTeacherClassSummaryIgnoresLegacySignalsForDroppedAndNeverEnrolledLearners`.
- Updated `AnalyticsController` analytics HTTP role checks to use `UserContext.roles()` instead of legacy subject-name helpers.

## Verification

Focused:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseKnowledgeControllerTest,AnalyticsControllerTest test
```

Result:

- `CourseKnowledgeControllerTest`: `22 run, 0 failures, 0 errors, 0 skipped`
- `AnalyticsControllerTest`: `37 run, 0 failures, 0 errors, 0 skipped`
- Combined focused total: `59 run, 0 failures`

Adjacent:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseKnowledgeControllerTest,AnalyticsControllerTest,CourseAccessServiceTest,AnalyticsServiceTest,DevAuthFilterTest,CurrentUserServiceTest test
```

Result:

- `82 run, 0 failures`

Full backend:

```powershell
cd D:\多元agent\backend
mvn test
```

Result:

- `487 run, 0 failures, 0 errors, 1 skipped`

## Subagent Review

| Expert | Result | Notes |
|---|---|---|
| Architect | PASS after closure | Implementation fits S Fast Lane; parent P3-4 must remain open. |
| Security Reviewer | PASS for named risks | Subject-name role-confusion, spoofed header, and enrollment-bound analytics risks are covered. |
| Test Engineer | PASS after closure | Test design supports this slice; evidence and TASK checklist now complete. |
| Integration Reviewer | PASS | No expert requested M/L upgrade or broader code changes. |

## Acceptance

- [x] Bearer `USER sub=teacher_1` cannot create a course for self through subject-name role confusion.
- [x] Bearer `USER sub=teacher_1` cannot create a knowledge point in a course whose `teacherId` equals the token subject.
- [x] Bearer `USER sub=teacher_1` cannot read class summary for a subject-owned course.
- [x] Bearer `TEACHER` class summary path ignores spoofed `X-User-Id` and counts only active enrolled learners.
- [x] Never-enrolled and dropped learners with learning path / wrong question / resource task signals are not counted in class analytics.
- [x] No REST API, DTO, DB, dependency, frontend, or formal OAuth2/JWK/Spring Security change.
- [x] Parent P3-4 remains not fully complete.

## Remaining Risks / Follow-ups

- Broader class/course authorization matrix still needs continued expansion.
- Formal OAuth2/JWK/Spring Security remains a separate P3-4 follow-up.
- After formal auth migration, rerun or port this subject-name/spoofing/enrollment penetration matrix.
