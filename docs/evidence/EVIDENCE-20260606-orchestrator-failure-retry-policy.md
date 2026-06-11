# Orchestrator 失败与重试策略证据

## 1. 范围

本切片完成：

- 通用 `RuntimeException` 运行期失败证据。
- `POST /api/orchestrator/workflows/{workflowId}/retry` 最小 endpoint。
- `RESOURCE_GENERATION`、`RAG_QA`、`ANSWER_SUBMISSION` 的失败/重试策略规格。

## 2. 代码证据

| 文件 | 变更 |
|---|---|
| `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java` | 捕获通用 `RuntimeException` 并写入脱敏 `step_runtime_failure`；新增 `retryWorkflow(...)`；retry workflow envelope 写入 `retryOfWorkflowId` |
| `backend/src/main/java/com/learningos/orchestrator/api/OrchestratorWorkflowController.java` | 新增 `POST /api/orchestrator/workflows/{workflowId}/retry` |
| `backend/src/main/java/com/learningos/orchestrator/dto/CreateWorkflowRequest.java` | 新增内部 `retryOfWorkflowId` 字段并保留四参构造 |
| `backend/src/main/java/com/learningos/orchestrator/dto/OrchestratorWorkflowResponse.java` | 响应新增 `retryOfWorkflowId` |
| `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java` | 新增 RuntimeException evidence、FAILED retry、非 FAILED retry 409 覆盖 |

## 3. TDD 过程

### RED

命令：

```powershell
cd backend
mvn "-Dtest=OrchestratorWorkflowControllerTest#persistsGenericRuntimeFailureEvidenceWithoutLeakingSensitiveInput+retriesFailedResourceGenerationWorkflowAsNewWorkflowForOwner+rejectsRetryForNonFailedWorkflow" test
```

结果：失败，符合预期。

关键失败：

```text
persistsGenericRuntimeFailureEvidenceWithoutLeakingSensitiveInput:
expected: "FAILED" but was: "RUNNING"

retriesFailedResourceGenerationWorkflowAsNewWorkflowForOwner:
Status expected:<200> but was:<500>

rejectsRetryForNonFailedWorkflow:
Status expected:<409> but was:<500>
```

说明：旧实现没有通用 `RuntimeException` failure evidence，且 retry endpoint 未映射。

### GREEN：新增行为聚焦测试

命令：

```powershell
cd backend
mvn "-Dtest=OrchestratorWorkflowControllerTest#persistsGenericRuntimeFailureEvidenceWithoutLeakingSensitiveInput+retriesFailedResourceGenerationWorkflowAsNewWorkflowForOwner+rejectsRetryForNonFailedWorkflow" test
```

结果：

```text
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### GREEN：Orchestrator Controller 聚焦测试

命令：

```powershell
cd backend
mvn "-Dtest=OrchestratorWorkflowControllerTest" test
```

结果：

```text
Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 4. 行为证据

- 通用 `RuntimeException`：
  - HTTP 返回 500 `INTERNAL_ERROR`。
  - durable `agent_task.status = FAILED`。
  - trace 保留 `workflow_start` 并追加 `step_runtime_failure`。
  - `outputJson` 和失败 summary 只写 `INTERNAL_ERROR`，不写原始 answer 或异常 message。
  - `GET workflow` 可见 `recentFailedStep`、`failedSteps=1`、`RETRY_WORKFLOW`。

- retry endpoint：
  - `FAILED RESOURCE_GENERATION` 可由 owner retry。
  - retry 创建新的 `workflowId/agentTaskId/traceId`。
  - retry response 和新 workflow envelope 记录 `retryOfWorkflowId`。
  - 非 owner retry 返回 404 `NOT_FOUND`。
  - 非 `FAILED` workflow retry 返回 409 `CONFLICT`。

## 5. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 只处理 HTTP mapping |
| Agent Trace | PASS | 失败写入 `agent_task/agent_trace` |
| Security | PASS | owner 限定，失败摘要脱敏 |
| API / Database | PASS | 新 endpoint 已写入 SPEC；未新增 schema |

## 6. 限制

- 当前自动 retry 仅支持 `RESOURCE_GENERATION`。
- `RAG_QA` 和 `ANSWER_SUBMISSION` 的 envelope 已脱敏，无法安全还原原始 question/answer，因此 retry endpoint 返回 409，调用方需重新提交 create request。
- 未运行全量 `mvn test`；本切片按要求运行最小相关 Orchestrator controller 测试。
