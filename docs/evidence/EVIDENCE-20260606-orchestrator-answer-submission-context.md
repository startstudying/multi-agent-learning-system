# Orchestrator ANSWER_SUBMISSION 上下文收敛证据

## 范围

本轮完成 P0-1 的 `ANSWER_SUBMISSION` 最小切片：

- `POST /api/orchestrator/workflows` 支持 `workflowType=ANSWER_SUBMISSION`
- Orchestrator 创建统一 `workflowId / agentTaskId / traceId`
- Assessment 子流程通过显式 `traceId` 入口执行
- `answer_record`、`grading_result`、`mastery_record`、`wrong_question`、`learning_event` 复用 Orchestrator traceId
- 同 `requestId` 相同 payload replay 首次 workflow
- 同 `requestId` 不同 payload 在创建 workflow 前返回 `409 CONFLICT`
- 无效 payload 在创建 `agent_task` 前返回 `400 VALIDATION_ERROR`

## 复查修复范围

集成复查后补齐以下后端风险点：

- `AssessmentService` 在服务层统一校验 `requestId`：必须非空，trim 后长度不超过 120。
- Orchestrator replay 不再只取第一条 `inputJsonContaining(requestId)` 记录，而是读取候选 workflow 后解析 envelope 精确匹配。
- replay 精确匹配条件包含 `workflowType=ANSWER_SUBMISSION`、`ownerUserId`、`learnerId`、`requestId`、`payload.questionId`、`payload.answerLength`，并在需要时匹配当前 `traceId`。
- 若 assessment 幂等层返回了已有提交导致 `traceId` 与当前 Orchestrator 上下文不同，Orchestrator 返回已有 winning workflow，并删除本次 transient loser `agent_task` / `agent_trace`。
- 全量验证期间发现 RAG timeout 测试 fixture 使用固定时间戳会随日期移动失效，已改为相对未来 cutoff，避免测试因当前时间漂移失败。

## 代码证据

- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java`
  - 新增 `submitAnswerWithTraceId(...)`
  - 新增 `replayAnswerIfPresent(...)`
  - 新增/收敛 `requestId` 服务层校验：非空且长度不超过 120
  - 直接 `submitAnswer(...)` 保持兼容，仍从 `TraceContext` 获取 traceId
- `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java`
  - 新增 `ANSWER_SUBMISSION` payload adapter
  - 新增 answer workflow replay preflight 和精确 envelope 匹配
  - 新增 trace drift winner workflow 返回和 transient task/trace 清理
  - 新增 assessment trace steps
  - 成功 workflow 转为 `DONE`
  - `ANSWER_SUBMISSION` workflow envelope 不保存完整 answer，仅保存 `questionId` 和 `answerLength`
- `backend/src/main/java/com/learningos/agent/repository/AgentTaskRepository.java`
  - 新增按 owner、taskType、requestId marker 查询候选 workflow 的 repository 方法
- `backend/src/main/java/com/learningos/agent/repository/AgentTraceRepository.java`
  - 新增按 `agentTaskId` 删除 transient trace 的 repository 方法
- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`
  - 覆盖 create、GET、replay、exact replay、trace drift、conflict、invalid payload
- `backend/src/test/java/com/learningos/assessment/application/AssessmentServiceTest.java`
  - 覆盖显式 trace replay、preflight empty/conflict/in-progress 分支和 `requestId` 服务层校验
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`
- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java`
  - 将 timeout recovery 测试 cutoff 改为相对当前时间，避免固定时间戳随日期漂移导致全量测试失败

## TDD 记录

### RED

命令：

```powershell
cd backend
mvn "-Dtest=OrchestratorWorkflowControllerTest,AssessmentServiceTest" test
```

结果：

- `BUILD FAILURE`
- 失败原因：`AssessmentService` 尚无 `submitAnswerWithTraceId(...)` 和 `replayAnswerIfPresent(...)`

### GREEN 1

命令：

```powershell
cd backend
mvn "-Dtest=OrchestratorWorkflowControllerTest,AssessmentServiceTest" test
```

结果：

- `Tests run: 25`
- `Failures: 0`
- `Errors: 0`
- `BUILD SUCCESS`

### GREEN 2

命令：

```powershell
cd backend
mvn "-Dtest=OrchestratorWorkflowControllerTest,AssessmentControllerTest,AssessmentServiceTest" test
```

结果：

- `Tests run: 33`
- `Failures: 0`
- `Errors: 0`
- `BUILD SUCCESS`

### RAG Fixture Repair Verification

命令：

```powershell
cd backend
mvn "-Dtest=IndexServiceTest,DocumentControllerTest" test
```

结果：

- `Tests run: 6`
- `Failures: 0`
- `Errors: 0`
- `BUILD SUCCESS`

说明：该命令验证全量测试中暴露的 RAG timeout recovery 测试时间 fixture 修复，不改变生产 RAG 逻辑。

### FULL

命令：

```powershell
cd backend
mvn test
```

结果：

- `Tests run: 117`
- `Failures: 0`
- `Errors: 0`
- `BUILD SUCCESS`

## 验证覆盖

| 项目 | 结果 | 说明 |
|---|---|---|
| `ANSWER_SUBMISSION` workflow 返回 `DONE` | PASS | `createsAnswerSubmissionWorkflowAndReusesWorkflowTraceContext` |
| `workflowId / agentTaskId / traceId` 一致 | PASS | response、`agent_task`、`agent_trace` 断言 |
| assessment 业务表 traceId 对齐 | PASS | answer/grading/mastery/wrong-question/event 断言 |
| GET workflow 返回完整链路 | PASS | `returnsWorkflowStatusContextAfterAnswerSubmission` |
| workflow replay 不新增 task 和业务行 | PASS | `replaysAnswerSubmissionWorkflowWithSameRequestIdWithoutDuplicatingRows` |
| replay 精确匹配避免命中同 requestId 的错误早期 workflow | PASS | `replaysAnswerSubmissionWorkflowByExactEnvelopeInsteadOfFirstRequestIdMarker` |
| trace drift 返回已有 winning workflow 并清理 transient task/trace | PASS | `concurrentAnswerWorkflowTraceDriftReturnsWinnerWorkflowWithoutPersistingSecondTask` |
| payload conflict 不新增 task 和业务行 | PASS | `rejectsAnswerSubmissionWorkflowPayloadConflictWithoutNewRows` |
| invalid payload 不创建 `agent_task` | PASS | `rejectsInvalidAnswerSubmissionPayloadBeforeCreatingWorkflowTask` |
| service 层拒绝空或过长 `requestId` | PASS | `AssessmentServiceTest` |
| 直接 assessment API 兼容 | PASS | `AssessmentControllerTest` 全绿 |

## 架构漂移检查

| 检查项 | 结果 | 说明 |
|---|---|---|
| Backend layering | PASS | Controller 未变化，业务仍在 Service |
| Agent Trace | PASS | Orchestrator 负责 `agent_task` / `agent_trace` |
| Repository boundary | PASS | Orchestrator 不访问 assessment repository |
| Security | WATCH | 本轮只保留 learner 级权限，题目级授权后续 P3 加固 |
| API / Database | PASS | 未新增 endpoint，未改 schema |
| Dependency | PASS | 未新增依赖 |

## 限制

- 本轮不处理通用 workflow retry/recovery。
- 本轮不处理运行期失败 evidence 独立事务化。
- 本轮不补题目/课程/路径级权限校验。
- 本轮不新增 workflow 表或 workflow-level unique key。
