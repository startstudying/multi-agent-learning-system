# EVIDENCE-20260611-p3-4-teacher-permission-residual-sampling-matrix

## 1. 任务

P3-4 子任务：teacher permission residual sampling matrix

## 2. 修改范围

本子任务是 S 级 test-only 权限矩阵补强。

生产代码改动：

- 无。

新增/修改测试文件：

- `backend/src/test/java/com/learningos/rag/api/KnowledgeBaseControllerTest.java`
- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java`
- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`

文档更新：

- `docs/tasks/TASK-20260611-p3-4-teacher-permission-residual-sampling-matrix.md`
- `docs/subagents/runs/RUN-20260611-p3-4-teacher-permission-residual-sampling-matrix-test.md`
- `docs/evidence/EVIDENCE-20260611-p3-4-teacher-permission-residual-sampling-matrix.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 3. 新增测试

1. `KnowledgeBaseControllerTest.bearerTeacherListRedactsForeignCourseBoundKnowledgeBasesDespitePublicVisibilityAndSpoofedHeader`
   - 验证 Bearer `TEACHER` list KB 时只返回 token subject 可读的 course-bound KB。
   - 验证 foreign course-bound KB 即使是 `PUBLIC` 也不会泄漏。
   - 验证 spoofed `X-User-Id: admin` 不会提升权限。

2. `DocumentControllerTest.bearerTeacherCannotReindexForeignCourseBoundDocumentDespiteSpoofedHeaderWithoutSideEffects`
   - 验证 Bearer `TEACHER` 不能 reindex foreign course-bound document。
   - 验证拒绝响应不泄漏 foreign document/index task/course 元数据。
   - 验证拒绝路径不新增 index task。

3. `AnalyticsControllerTest.teacherClassSummaryPendingReviewsRedactsForeignCourseReviews`
   - 验证教师 class summary 的 `pendingReviews` 只聚合请求课程内 review。
   - 验证 foreign review/resource/task/course/title 不泄漏。
   - 验证响应不暴露 `markdownContent`。

## 4. 验证记录

### 4.1 Focused

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest,AnalyticsControllerTest test
```

结果：

```text
Tests run: 71, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-11T01:50:17+08:00
```

### 4.2 Adjacent

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest,AnalyticsControllerTest,ResourceReviewControllerTest,ResourceGenerationControllerTest,AssessmentControllerTest test
```

结果：

```text
Tests run: 172, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-11T01:51:42+08:00
```

### 4.3 Full backend

命令：

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

```text
Tests run: 595, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
Finished at: 2026-06-11T01:54:37+08:00
```

说明：

- Maven 输出包含 Mockito dynamic agent / ByteBuddy warning；本轮验证未出现失败或错误。

## 5. Acceptance

| 验收项 | 结果 |
|---|---|
| Mini TASK 存在并包含目标、范围、允许/禁止文件、测试命令和验收标准 | PASS |
| 使用专家 subagent 并行分析，且报告落盘 | PASS |
| Bearer `TEACHER` KB list 不泄漏 foreign course-bound KB | PASS |
| Bearer `TEACHER` reindex foreign document 被拒绝且无 index task 副作用 | PASS |
| 教师 class summary `pendingReviews` 不混入 foreign review | PASS |
| 无生产代码/API/DTO/DB/schema/dependency/frontend 改动 | PASS |
| Focused / adjacent / full backend 验证已运行并通过 | PASS |
| Changelog、Memory、P3-4 TODO 已更新 | PASS |
| P3-4 父项未被误标为完成 | PASS |

## 6. 结论

Acceptance Verdict：PASS。

当前子任务完成。P3-4 父项保持 open，后续建议优先处理：

```text
P3-4 子任务：orchestrator answer submission replay scope revalidation
```
