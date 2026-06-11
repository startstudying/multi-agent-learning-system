# RUN-20260609 P3-4 子任务：Analytics teacherClassSummary legacy cleanup - Architect

## 角色

Architect / 架构边界审查。

## 结论

建议删除 `AnalyticsService.teacherClassSummary(String courseId, String currentUserId)` 两参 legacy overload，并删除 `isLegacyTeacherUser(String)`。该入口会从 subject-name 推断 admin/teacher 权限，和 P3-4 roles-first 授权方向冲突。

## 关键判断

- HTTP 主路径已经通过 `AnalyticsController` 传入显式 admin/teacher role facts。
- 服务层公共授权入口应保留为 `teacherClassSummary(String, String, boolean, boolean)`。
- `requireTeacherClassAccess(...)` 不应在 `courseAccessService == null` 时回退到 `"admin"` 或 `currentUserId == course.teacherId`。
- `classLearnerIds(...)` 在缺少 `CourseAccessService` 时应 fail-closed，不能从 `LearningPath.goalId == courseId` 反推班级成员。

## 不触碰范围

- 不改 REST API path / DTO / schema / dependency / frontend。
- 不引入正式 OAuth2/JWK/Spring Security。
- 不扩大到 broader class/course authorization matrix。

