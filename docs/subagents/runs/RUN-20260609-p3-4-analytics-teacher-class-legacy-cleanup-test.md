# RUN-20260609 P3-4 子任务：Analytics teacherClassSummary legacy cleanup - Test Engineer

## 角色

Test Engineer / TDD 与回归覆盖设计。

## 建议测试策略

新增 `backend/src/test/java/com/learningos/analytics/application/AnalyticsServiceTest.java`，用服务层 reflection guard 和最小行为回归覆盖 legacy surface cleanup。

## 必测点

- `AnalyticsService` 不再暴露 public `teacherClassSummary(String, String)`。
- `AnalyticsService` 不再包含 `isLegacyTeacherUser(String)`。
- roles-first overload 不会因为 `currentUserId = "admin"` 且 role facts 为 false 而授予 admin 权限。
- roles-first overload 不会因为 `currentUserId == Course.teacherId` 且 teacher fact 为 false 而授予 teacher 权限。
- `courseAccessService == null` 时，class learner set 不从 `LearningPath` 反推。

## 推荐命令

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AnalyticsServiceTest test
mvn --% -Dtest=AnalyticsServiceTest,AnalyticsControllerTest,CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
mvn test
```

