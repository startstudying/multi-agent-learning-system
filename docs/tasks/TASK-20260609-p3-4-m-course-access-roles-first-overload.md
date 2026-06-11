# TASK-20260609 P3-4-M Course API / CourseAccessService roles-first overload

## Status

Done

## Done Criteria

- [x] PRD / REQ / SPEC / PLAN / TASK / CONTEXT 已创建。
- [x] Subagent 分析已完成并落盘。
- [x] RED 测试已观察到预期失败。
- [x] `CourseAccessService` 新增 roles-first read/manage/list overload。
- [x] `CourseController` 与 `KnowledgePointController` 使用 role-derived facts。
- [x] `KnowledgeCatalogService` 新增 roles-first public overload，并内部调用 roles-first access service。
- [x] Bearer admin / teacher / student / spoofed header / missing / foreign 测试通过。
- [x] 不新增依赖。
- [x] 不改 DB schema。
- [x] 不改前端。
- [x] Focused verification 已执行并记录。
- [x] Adjacent regression 已执行并记录。
- [x] Full backend Maven verification 已执行或限制已说明。
- [x] Evidence / Acceptance / Changelog / Memory / backend TODO / Retro 已更新。

## Implementation Checklist

1. [x] 修改 `CourseKnowledgeControllerTest`，新增 Bearer JWT helper 与 roles-first Course API 矩阵。
2. [x] 运行 focused RED：`mvn --% -Dtest=CourseKnowledgeControllerTest test`。
3. [x] 修改 `CourseAccessService`：新增 roles-first overload，旧签名保留委托。
4. [x] 修改 `KnowledgeCatalogService`：新增 roles-first create/read/list/manage/graph public overload。
5. [x] 修改 `CourseController`：传入 `currentUserId/isAdmin/isTeacherUser`。
6. [x] 修改 `KnowledgePointController`：传入 `currentUserId/isAdmin/isTeacherUser`。
7. [x] 运行 focused / adjacent / full tests。
8. [x] 收尾文档。

## Verification Results

```text
Focused:  mvn --% -Dtest=CourseKnowledgeControllerTest test
Result:   20 run, 0 failures, 0 errors, 0 skipped

Adjacent: mvn --% -Dtest=CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest,AnalyticsControllerTest test
Result:   63 run, 0 failures, 0 errors, 0 skipped

Full:     mvn test
Result:   403 run, 0 failures, 0 errors, 1 skipped
```

## Test Commands

```powershell
cd backend
mvn --% -Dtest=CourseKnowledgeControllerTest test
mvn --% -Dtest=CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest,AnalyticsControllerTest test
mvn test
```
