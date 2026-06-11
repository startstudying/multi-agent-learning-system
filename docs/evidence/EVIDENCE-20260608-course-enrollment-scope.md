# EVIDENCE-20260608 P3-4-D Course Enrollment Scope

## 1. 变更摘要

- 新增 `course_enrollment` V19 migration。
- 新增 `CourseEnrollment` entity、`CourseEnrollmentRepository`、`CourseAccessService`。
- `KnowledgeCatalogService` 课程 list/detail/graph 改为通过 `CourseAccessService` 使用 active enrollment 过滤学生课程。
- `LearningWorkflowService.createPathForUser` 对现存 course-bound `goalId` 校验 learner active enrollment。
- `ResourceGenerationService.createTask/createTaskInWorkflow` 对现存 course-bound `goalId` 校验 learner active enrollment。
- `AnalyticsService.teacherClassSummary` 的 learner set 改为 active enrollment，learning path 只作为已报名 learner 的学习信号。

## 2. 测试覆盖

已添加/更新测试：

- `CourseKnowledgeControllerTest`
  - student course list 只返回 active enrolled course。
  - student detail / knowledge graph 需要 active enrollment。
- `LearningWorkflowControllerTest`
  - course-bound path 未报名返回 `FORBIDDEN` 且不落库。
  - active enrollment 可生成 course DAG path。
  - 非 course template goal 保持兼容。
- `ResourceGenerationControllerTest`
  - course-bound resource generation 未报名返回 `FORBIDDEN` 且不创建 task/resource/review。
  - active enrollment 可创建 resource generation task。
  - 非 course template goal 保持兼容。
- `AnalyticsControllerTest`
  - teacher class summary learner set 来自 active enrollment。
  - dropped learner 的 legacy learning path / wrong question / resource task 不进入聚合。
- `SchemaConvergenceMigrationTest`
  - V19 migration 包含 `course_enrollment` 表、状态列、唯一约束与索引。
- `MysqlMigrationSmokeTest`
  - 最新版本常量更新为 V19 / 19。
  - smoke 断言 `course_enrollment` 关键列与索引。

## 3. 验证命令

计划执行命令：

```powershell
cd backend
mvn --% -Dtest=CourseKnowledgeControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,AnalyticsControllerTest,SchemaConvergenceMigrationTest test
mvn --% -Dtest=CourseKnowledgeControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,AnalyticsControllerTest,AssessmentControllerTest,DocumentControllerTest,ResourceReviewControllerTest test
mvn test
```

## 4. 当前验证结果

早期 Codex 工具层执行 `mvn --% ...` / shell / Node REPL 时曾被同一环境错误阻断：

```text
windows sandbox: setup refresh failed with status exit code: 1
```

随后改用等价 Maven 参数格式完成验证：

```powershell
cd backend
mvn "-Dtest=CourseKnowledgeControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,AnalyticsControllerTest,SchemaConvergenceMigrationTest" test
```

结果：

```text
Tests run: 74, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

```powershell
cd backend
mvn "-Dtest=ResourceReviewControllerTest" test
```

结果：

```text
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

```powershell
cd backend
mvn "-Dtest=CourseKnowledgeControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,AnalyticsControllerTest,AssessmentControllerTest,DocumentControllerTest,ResourceReviewControllerTest" test
```

结果：

```text
Tests run: 87, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

```powershell
cd backend
mvn test
```

结果：

```text
Tests run: 312, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

## 5. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 仍只委托 Service；课程权限集中到 `CourseAccessService`。 |
| Frontend rules | PASS | 未修改 frontend。 |
| Agent / RAG rules | PASS | 未修改 Agent Tool/RAG 检索链路；resource generation 在 Service 层校验权限。 |
| Security | PASS | 权限在后端代码执行，不依赖 Prompt；未新增依赖。 |
| API / Database | PASS | V19 schema 与 SPEC 对齐；未新增 endpoint。 |

## 6. Subagent Review Evidence

- Test expert subagent reviewed the needed RED coverage and confirmed the core cases: active/dropped enrollment for course list/detail/graph, course-bound path/resource forbidden checks, teacher summary active enrollment membership, and V19 static migration assertions.
- Security reviewer subagent flagged centralization, anti-enumeration, and scope-sprawl risks. The implementation uses `CourseAccessService` as the shared authorization boundary and keeps the slice limited to course enrollment scope.
- Code reviewer subagent flagged three high-risk points:
  - `FORBIDDEN` / `NOT_FOUND` semantics: implemented per SPEC in `CourseAccessService.requireCourseRead`; admin missing is `NOT_FOUND`, non-admin missing/foreign/not-enrolled is `FORBIDDEN`.
  - Non-course goals: `requireLearnerEnrolledForExistingCourse` only enforces when `goalId` resolves to an existing course.
  - Resource generation actor scope: this slice preserves owner-only resource creation and does not expand teacher/admin proxy creation, matching SPEC.
