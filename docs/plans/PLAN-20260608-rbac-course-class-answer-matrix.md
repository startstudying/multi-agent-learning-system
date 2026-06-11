# PLAN-20260608 P3-4-C 权限矩阵安全前置

## 1. 实施步骤

1. [x] RED：在 `CourseKnowledgeControllerTest` 增加 course list/detail/graph scope 测试。
2. [x] RED：在 `AssessmentControllerTest` 增加 student 禁止 grading evaluation 测试。
3. [x] GREEN：`CourseController` 改为传入 `currentUserId`。
4. [x] GREEN：`KnowledgeCatalogService` 增加 scoped list/detail/graph。
5. [x] GREEN：`AssessmentController` 调用带 user 的 grading evaluation。
6. [x] GREEN：`GradingEvaluationService` 增加 teacher/admin gate。
7. [x] 运行聚焦测试。
8. [x] 运行相邻回归。
9. [x] 运行后端全量测试。
10. [x] 更新 Evidence / Acceptance / Memory / Changelog / TODO。

## 2. 风险

| Risk | Mitigation |
|---|---|
| 既有测试默认无 `X-User-Id` 导致 student 空列表 | 已有写测试显式 teacher；必要时补 header。 |
| student 未来应可读取 enrolled course | 当前无 class/enrollment schema，明确返回空列表并保留后续切片。 |
| 非 admin missing/foreign 语义影响旧测试 | 只调整当前课程读取测试，admin 仍保留 NOT_FOUND。 |

## 3. 验证命令

```powershell
cd backend
mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest test
mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest,AnalyticsControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,DocumentControllerTest test
mvn test
```

## 4. 验证结果

| 命令 | 结果 |
|---|---|
| `mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest test` | PASS；19 tests，0 failures，0 errors |
| `mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest,AnalyticsControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,DocumentControllerTest test` | PASS；71 tests，0 failures，0 errors |
| `mvn test` | PASS；302 tests，0 failures，0 errors，1 skipped |
