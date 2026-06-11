# RUN-20260609 P3-4-L class analytics roles-first course scope

## Security & Quality

只读分析结论：当前 `GET /api/analytics/classes/{courseId}/summary` 存在 roles-first 与 anti-enumeration 缺口。

关键证据：

- `D:\多元agent\backend\src\main\java\com\learningos\analytics\api\AnalyticsController.java` 的 `teacherClassSummary(...)` 只传 `currentUserId`，未传 `isAdmin()` / `isTeacherUser()`。
- `D:\多元agent\backend\src\main\java\com\learningos\analytics\application\AnalyticsService.java` 的 `teacherClassSummary(...)` 先查 course，missing 直接 `NOT_FOUND`，非 admin 可区分 missing/foreign。
- `D:\多元agent\backend\src\main\java\com\learningos\knowledge\application\CourseAccessService.java` 仍以 `"admin"`、`"teacher"`、`"teacher_"` 进行 legacy role inference。
- `D:\多元agent\backend\src\main\java\com\learningos\common\auth\CurrentUserService.java` 已具备 roles-first `isAdmin()` / `isTeacherUser()`。

建议：class summary controller 传入 roles flags；service 中 admin missing 保留 `NOT_FOUND`，非 admin missing/foreign 统一 `FORBIDDEN`；teacher role 仍必须满足 `token.sub == Course.teacherId`。

## Backend Expert

最小变更范围：

- `D:\多元agent\backend\src\main\java\com\learningos\analytics\api\AnalyticsController.java`
- `D:\多元agent\backend\src\main\java\com\learningos\analytics\application\AnalyticsService.java`
- `D:\多元agent\backend\src\test\java\com\learningos\analytics\api\AnalyticsControllerTest.java`

不建议在本切片全量迁移 `CourseAccessService`，因为该服务被 analytics、assessment、learning、rag、knowledge、agent 多模块引用。P3-4-L 应只修 class summary 的已证实缺口。

## Test Engineer

建议 RED 测试：

- `teacherClassSummaryUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader`
- `teacherClassSummaryAllowsBearerTeacherWhenSubjectOwnsCourse`
- `teacherClassSummaryRejectsBearerStudentWithSpoofedAdminHeader`
- `teacherClassSummaryReturnsForbiddenForNonAdminMissingAndForeignCourse`
- `teacherClassSummaryAdminMissingCourseRemainsNotFound`

预期 RED：

- Bearer `ADMIN` subject `ops_admin` 访问 existing course 当前会被 legacy `admin` 判断拒绝。
- Bearer/非 admin 访问 missing course 当前会返回 `NOT_FOUND`，应改为 `FORBIDDEN`。

建议命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AnalyticsControllerTest test
mvn --% -Dtest=AnalyticsControllerTest,CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
mvn test
```
