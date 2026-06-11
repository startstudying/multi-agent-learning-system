# TASK-20260609 P3-4-L class analytics roles-first course scope

## Status

Done

## Done Criteria

- [x] PRD / REQ / SPEC / PLAN / TASK / CONTEXT 已创建。
- [x] Subagent 分析已完成并汇总。
- [x] RED 测试已观察到预期失败。
- [x] `GET /api/analytics/classes/{courseId}/summary` 使用 roles-first 授权。
- [x] Bearer admin / teacher / student / missing / foreign 测试通过。
- [x] 不新增依赖。
- [x] 不改 DB schema。
- [x] 不改前端。
- [x] Focused verification 已执行并记录。
- [x] Adjacent regression 已执行并记录。
- [x] Full backend Maven verification 已执行或限制已说明。
- [x] Evidence / Acceptance / Changelog / Memory / backend TODO / Retro 已更新。

## Implementation Checklist

1. [x] 修改 `AnalyticsControllerTest`：新增 Bearer ADMIN/TEACHER/STUDENT class summary 测试。
2. [x] 修改 missing course 非 admin 测试语义：`FORBIDDEN`。
3. [x] 新增 admin missing course `NOT_FOUND` 测试。
4. [x] 运行 RED。
5. [x] 修改 `AnalyticsController.teacherClassSummary` 传入 roles flags。
6. [x] 修改 `AnalyticsService.teacherClassSummary` 与授权 guard。
7. [x] 运行 focused / adjacent / full tests。
8. [x] 收尾文档。

## Verification Results

```text
Focused:  mvn --% -Dtest=AnalyticsControllerTest test
Result:   29 run, 0 failures, 0 errors, 0 skipped

Adjacent: mvn --% -Dtest=AnalyticsControllerTest,CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
Result:   56 run, 0 failures, 0 errors, 0 skipped

Full:     mvn test
Result:   396 run, 0 failures, 0 errors, 1 skipped
```

## Test Commands

```powershell
cd backend
mvn --% -Dtest=AnalyticsControllerTest test
mvn --% -Dtest=AnalyticsControllerTest,CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
mvn test
```
