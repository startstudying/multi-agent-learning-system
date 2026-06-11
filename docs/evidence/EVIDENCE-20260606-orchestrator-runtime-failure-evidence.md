# Orchestrator 运行期失败证据持久化证据

## 范围

本轮完成 P0-1 Orchestrator 运行期失败证据持久化最小切片：当 workflow task 已创建后，下游服务抛出 `ApiException`，Orchestrator 会写入 `FAILED agent_task` 和失败 `agent_trace`，同时原样抛出业务错误码。

## 代码证据

- `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java`
  - `createWorkflow(...)` 对 `ApiException` 增加 `noRollbackFor`。
  - 下游 workflow 执行区间捕获 `ApiException`，调用 `recordRuntimeFailure(...)` 后原样抛出。
  - 失败摘要只保存 workflow type 和 error code，不保存完整 question/answer。
- `backend/src/main/java/com/learningos/agent/application/AgentRunRecorder.java`
  - 新增 `recordRuntimeFailure(...)`，只写 `agent_task` 和 `agent_trace`，不写 `model_call_log`，避免把权限/安全失败伪装成模型调用失败。
- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`
  - 新增 `persistsRagQaRuntimeFailureEvidenceWithoutWritingQueryArtifacts`。
  - 覆盖 RAG_QA 未授权 KB 返回 403、保留失败 workflow、失败摘要脱敏、不写 query log/citation。

## TDD 过程

### RED

先写测试后运行：

```powershell
cd backend
mvn "-Dtest=OrchestratorWorkflowControllerTest#persistsRagQaRuntimeFailureEvidenceWithoutWritingQueryArtifacts" test
```

失败结果：

```text
expected: "FAILED"
 but was: "RUNNING"
Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
BUILD FAILURE
```

说明：HTTP 已返回 403，但 workflow task 仍停留在 `RUNNING`，没有失败状态收敛。

### GREEN：单测试

实现运行期失败记录后重新运行：

```powershell
cd backend
mvn "-Dtest=OrchestratorWorkflowControllerTest#persistsRagQaRuntimeFailureEvidenceWithoutWritingQueryArtifacts" test
```

结果：

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### GREEN：Orchestrator 聚焦测试

```powershell
cd backend
mvn "-Dtest=OrchestratorWorkflowControllerTest" test
```

结果：

```text
Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### GREEN：交叉回归

```powershell
cd backend
mvn "-Dtest=OrchestratorWorkflowControllerTest,RagQueryServiceTest,AssessmentControllerTest,AssessmentServiceTest" test
```

结果：

```text
Tests run: 41, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### GREEN：全量后端测试

```powershell
cd backend
mvn test
```

结果：

```text
Tests run: 122, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time: 01:04 min
Finished at: 2026-06-06T11:12:36+08:00
```

## Subagent 审查证据

详见 `docs/subagents/runs/RUN-20260606-orchestrator-runtime-failure-evidence.md`。

关键结论：

- 测试审查建议把 RED 放在 `OrchestratorWorkflowControllerTest`，因为缺口在 Orchestrator 对下游异常的治理。
- 安全审查要求失败摘要脱敏、错误码不被吞、权限失败不写 query/citation 成功证据；本轮实现按该边界处理。

## 架构漂移检查

| 检查项 | 结果 | 说明 |
|---|---|---|
| Backend layering | PASS | 失败治理仍在 Orchestrator/AgentRunRecorder Service 层，Controller 未增加业务逻辑。 |
| Agent Trace | PASS | 运行期失败保留 `workflow_start` 和 `step_runtime_failure`。 |
| Error code | PASS | `ApiException` 原样抛出，HTTP 仍返回 403/409/400 等原错误码。 |
| Privacy | PASS | 失败 evidence 只保存 error code，不保存完整问题原文。 |
| RAG artifacts | PASS | 未授权 KB 不写 `kb_query_log` 或 `source_citation`。 |
