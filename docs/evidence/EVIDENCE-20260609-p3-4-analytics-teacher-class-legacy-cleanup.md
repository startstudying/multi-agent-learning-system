# EVIDENCE-20260609 P3-4 子任务：Analytics teacherClassSummary legacy subject-name cleanup

## 结果

通过。

## 变更摘要

- 删除 `AnalyticsService.teacherClassSummary(String courseId, String currentUserId)` 两参 legacy overload。
- 删除 `AnalyticsService.isLegacyTeacherUser(String)`。
- `requireTeacherClassAccess(...)` 不再在 `courseAccessService == null` 时通过 subject-name 或 `Course.teacherId` fallback 授权。
- `classLearnerIds(...)` 在 `courseAccessService == null` 时 fail-closed 返回 `Set.of()`，不再从 `LearningPath` 反推班级成员。
- 新增 `AnalyticsServiceTest` 作为 reflection guard 与 service-level behavior regression。

## 专家 Subagent 证据

- `docs/subagents/runs/RUN-20260609-p3-4-analytics-teacher-class-legacy-cleanup-architect.md`
- `docs/subagents/runs/RUN-20260609-p3-4-analytics-teacher-class-legacy-cleanup-security.md`
- `docs/subagents/runs/RUN-20260609-p3-4-analytics-teacher-class-legacy-cleanup-test.md`
- `docs/subagents/runs/RUN-20260609-p3-4-analytics-teacher-class-legacy-cleanup-integration-review.md`

## 静态核对

```powershell
rg -n "teacherClassSummary\(String courseId, String currentUserId\)|isLegacyTeacherUser|courseAccessService == null &&|LearningPath::getLearnerId" backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java
```

结果：0 命中。

## TDD / Verification

### RED

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AnalyticsServiceTest#teacherClassSummaryLegacyOverloadIsRemoved+analyticsServiceLegacyTeacherHelperIsRemoved+rolesFirstTeacherClassSummaryDoesNotGrantAdminWhenOnlySubjectNameIsAdmin+rolesFirstTeacherClassSummaryDoesNotGrantTeacherWhenOnlySubjectOwnsCourse test
```

结果：`4 run, 4 failures`。

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AnalyticsServiceTest#teacherClassSummaryDoesNotInferClassMembershipFromLearningPathsWhenCourseAccessServiceIsMissing test
```

结果：`1 run, 1 failure`，期望 `0`，实际 `1`。

### GREEN focused

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AnalyticsServiceTest test
```

结果：`5 run, 0 failures`。

### Compile guard

```powershell
cd D:\多元agent\backend
mvn --% -DskipTests compile
```

结果：success。

### Adjacent regression

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AnalyticsServiceTest,AnalyticsControllerTest,CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
```

结果：`73 run, 0 failures`。

### Full backend

```powershell
cd D:\多元agent\backend
mvn test
```

结果：`482 run, 0 failures, 0 errors, 1 skipped`。

## Acceptance

- [x] `AnalyticsService` no longer exposes public `teacherClassSummary(String, String)`.
- [x] `AnalyticsService` no longer contains `isLegacyTeacherUser(String)`.
- [x] `requireTeacherClassAccess(...)` no longer grants admin/teacher access from subject name when explicit role facts are false.
- [x] Roles-first `teacherClassSummary(String, String, boolean, boolean)` remains intact.
- [x] HTTP class summary behavior remains covered by existing controller tests.
- [x] No REST API contract, DTO, DB, dependency, frontend, `AnalyticsController`, `CourseAccessService`, or formal OAuth2/JWK/Spring Security change.
- [x] Parent P3-4 remains not fully complete; broader class/course authorization matrix, formal OAuth2/JWK/Spring Security, and broader permission penetration tests remain open.

