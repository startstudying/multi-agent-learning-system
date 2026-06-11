# EVIDENCE-20260611-p3-4-orchestrator-answer-submission-replay-scope-revalidation

## 1. 任务

P3-4 子任务：orchestrator answer submission replay scope revalidation

## 2. 修改范围

生产代码：

- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java`

测试：

- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`

文档：

- `docs/tasks/TASK-20260611-p3-4-orchestrator-answer-submission-replay-scope-revalidation.md`
- `docs/subagents/runs/RUN-20260611-p3-4-orchestrator-answer-submission-replay-scope-revalidation-security.md`
- `docs/subagents/runs/RUN-20260611-p3-4-orchestrator-answer-submission-replay-scope-revalidation-test.md`
- `docs/subagents/runs/RUN-20260611-p3-4-orchestrator-answer-submission-replay-scope-revalidation-architect.md`
- `docs/evidence/EVIDENCE-20260611-p3-4-orchestrator-answer-submission-replay-scope-revalidation.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

未修改：

- REST API path / request DTO / response DTO
- DB schema / migration
- Maven dependency
- frontend
- auth framework

## 3. 实现摘要

- 新增 Orchestrator MockMvc RED/GREEN 回归：
  - `OrchestratorWorkflowControllerTest.answerSubmissionWorkflowReplayRevalidatesEnrollmentBeforeReturningExistingWorkflow`
- 修复 `AssessmentService.replayAnswerIfPresent(...)`：
  - replay 查询历史 answer 前复用 `requireSubmitQuestionScope(request.learnerId(), request.questionId())`。
  - 与 `submitAnswerWithTraceId(...)` 保持相同的 `questionId -> KnowledgePoint.courseId -> CourseAccessService.requireCourseRead(learnerId, false, false, courseId)` scope 语义。
- dropped enrollment 后，同 `requestId` / 同 payload 的 Orchestrator `ANSWER_SUBMISSION` replay 返回 `FORBIDDEN`，不返回旧 workflow。

## 4. RED 证据

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=OrchestratorWorkflowControllerTest#answerSubmissionWorkflowReplayRevalidatesEnrollmentBeforeReturningExistingWorkflow test
```

结果：

```text
Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
Status expected:<403> but was:<200>
BUILD FAILURE
Finished at: 2026-06-11T02:15:26+08:00
```

RED 结论：

- 第二次 replay 在 enrollment 已变为 `DROPPED` 后仍返回 200 和旧 workflow。
- 测试准确复现 stale authorization replay 缺陷。

## 5. GREEN / 验证记录

### 5.1 Focused

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=OrchestratorWorkflowControllerTest#answerSubmissionWorkflowReplayRevalidatesEnrollmentBeforeReturningExistingWorkflow test
```

结果：

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-11T02:17:28+08:00
```

### 5.2 Adjacent

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=OrchestratorWorkflowControllerTest,AssessmentControllerTest,AssessmentServiceTest test
```

结果：

```text
Tests run: 93, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-11T02:19:01+08:00
```

### 5.3 Full backend

命令：

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

```text
Tests run: 596, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
Finished at: 2026-06-11T02:21:57+08:00
```

说明：

- Maven 输出包含 Mockito dynamic agent / ByteBuddy warning；本轮验证未出现失败或错误。

## 6. 安全扫描补充

### 6.1 MyBatis `${}` 扫描

命令：

```powershell
powershell -ExecutionPolicy Bypass -File C:\Users\wonderful\.codex\skills\java-security-review\scripts\scan-mybatis-dollar.ps1 -ScanDir backend
```

结果：

```text
Only *Mapper.xml and src/**/*.java, excludes pom.xml
IndexTaskRecoveryScheduler.java: @Scheduled(fixedDelayString = "${learning-os.rag.index-recovery.fixed-delay:5m}")
IndexTaskWorkerScheduler.java: @Scheduled(fixedDelayString = "${learning-os.rag.index-worker.fixed-delay:5s}")
QdrantVectorConfiguration.java: @ConditionalOnExpression("'${learning-os.rag.vector.provider:none}' == 'qdrant'")
```

结论：

- 命中均为 Spring 配置占位符，不是 MyBatis Mapper SQL `${}` 注入点。
- 本子任务未新增 MyBatis SQL。

### 6.2 hardcoded secrets 脚本

命令：

```powershell
powershell -ExecutionPolicy Bypass -File C:\Users\wonderful\.codex\skills\java-security-review\scripts\find-hardcoded-secrets.ps1 -ScanDir backend
```

结果：

```text
ParserError: The string is missing the terminator: ".
```

结论：

- 该技能脚本自身存在编码/字符串闭合问题，本轮不能作为有效扫描结果。
- 本子任务未新增生产 secret、API key、依赖配置或前端环境变量。
- 测试中使用的 HS256 test secret 是既有 test fixture 范围。

## 7. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 未新增业务权限；replay scope 修复落在 Assessment Service。 |
| Frontend rules | PASS | 不改前端。 |
| Agent / RAG rules | PASS | 不改 Agent/RAG runtime；Orchestrator 仍通过 Service 编排。 |
| Security | PASS | replay 返回旧响应前重新校验当前对象范围；Bearer spoofed header 测试覆盖在回归内。 |
| API / Database | PASS | 不改 API contract、DTO、DB schema 或 migration。 |

## 8. Acceptance

| 验收项 | 结果 |
|---|---|
| Mini TASK 存在并包含目标、范围、允许/禁止文件、测试命令和验收标准 | PASS |
| 使用专家 subagent 并行分析，且报告落盘 | PASS |
| RED 测试证明 dropped enrollment 后 replay 曾返回旧 workflow | PASS |
| `AssessmentService.replayAnswerIfPresent(...)` replay 前重新校验 `questionId` scope | PASS |
| dropped enrollment 后 Orchestrator `ANSWER_SUBMISSION` replay 返回 `FORBIDDEN` | PASS |
| 拒绝响应无 `data`，不返回旧 workflow/task/trace 元数据 | PASS |
| 拒绝 replay 不新增 answer/grading/mastery/wrong-question/learning-event | PASS |
| 无 API/DTO/DB/dependency/frontend/auth framework 改动 | PASS |
| Focused / adjacent / full backend 验证已运行并通过 | PASS |
| Changelog、Memory、P3-4 TODO 已更新 | PASS |
| P3-4 父项未被误标为完成 | PASS |

## 9. 结论

Acceptance Verdict：PASS。

当前子任务完成。P3-4 父项保持 open，后续仍可继续做更完整业务矩阵抽样复核和正式 production streaming 设计。
