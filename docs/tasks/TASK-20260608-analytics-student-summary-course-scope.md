# TASK-20260608 学生分析摘要课程范围权限收口

## Status

Completed

## Done Criteria

- [x] `GET /api/analytics/students/{learnerId}/summary` 支持可选 `courseId`。
- [x] student 只能读取自己；带 `courseId` 时必须 enrolled。
- [x] teacher 必须提供 `courseId`，且只能读取 own-course active enrolled learner 的课程内摘要。
- [x] admin 可读取任意 learner 全局或课程内摘要。
- [x] course-scoped 摘要不混入其他课程 path/mastery/wrong-question 信号。
- [x] RED/GREEN 已记录。
- [x] Focused / adjacent / full backend verification 已执行或说明限制。
- [x] Evidence / Acceptance / Changelog / Memory 已更新。

## Implementation Checklist

1. [x] 在 `AnalyticsControllerTest` 添加 failing tests。
2. [x] 修改 `AnalyticsController.studentSummary(...)` 接收 `courseId` 并传入 current user id / role helper。
3. [x] 修改 `AnalyticsService.studentSummary(...)` 做授权与 course scope 聚合。
4. [x] 保持原有 student owner global summary 测试通过。
5. [x] 更新 P3 planning 和 memory。

## Test Commands

```powershell
cd backend
mvn --% -Dtest=AnalyticsControllerTest test
mvn --% -Dtest=AnalyticsControllerTest,CourseKnowledgeControllerTest,AssessmentControllerTest test
mvn test
```

## Completion Evidence

- RED：旧行为已观察到 teacher/admin/student/course scoped 新测试失败。
- GREEN focused：`mvn --% -Dtest=AnalyticsControllerTest test`，22 tests，0 failures，0 errors。
- Adjacent：`mvn --% -Dtest=AnalyticsControllerTest,CourseKnowledgeControllerTest,AssessmentControllerTest test`，60 tests，0 failures，0 errors。
- Full backend：`mvn test`，350 tests，0 failures，0 errors，1 skipped。
- Evidence：`docs/evidence/EVIDENCE-20260608-analytics-student-summary-course-scope.md`。
- Acceptance：`docs/acceptance/ACCEPT-20260608-analytics-student-summary-course-scope.md`。
