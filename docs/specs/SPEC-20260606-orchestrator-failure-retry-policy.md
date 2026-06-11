# Orchestrator 失败与重试策略规格

## 1. 范围

本规格补齐 Orchestrator Workflow 的通用失败证据和最小 retry API。实现复用 `agent_task.inputJson` 中的 workflow envelope，不新增 workflow 表。

## 2. API

### 2.1 查询 workflow

```http
GET /api/orchestrator/workflows/{workflowId}
```

失败 workflow 返回：

```json
{
  "status": "FAILED",
  "recentFailedStep": {
    "stepId": "step_runtime_failure",
    "status": "FAILED",
    "summary": "Workflow RAG_QA failed with INTERNAL_ERROR."
  },
  "traceSummary": {
    "failedSteps": 1,
    "lastStatus": "FAILED"
  },
  "nextActions": ["INSPECT_TRACE", "RETRY_WORKFLOW"]
}
```

### 2.2 重试 workflow

```http
POST /api/orchestrator/workflows/{workflowId}/retry
```

请求体：无。

行为：

- 按 `ownerUserId + workflowId` 查询原 workflow。
- 原 workflow 不存在或不属于当前用户：404 `NOT_FOUND`。
- 原 workflow `status != FAILED`：409 `CONFLICT`。
- 原 workflow 支持重试：读取 envelope，重建 `CreateWorkflowRequest`，调用 `createWorkflow(...)` 创建新 workflow。
- 响应结构同创建 workflow。

## 3. 失败证据策略

### 3.1 通用 RuntimeException

捕获范围：

```text
startRun
record workflow_start
execute downstream workflow
```

上述阶段之后发生的通用 `RuntimeException` 必须执行：

```text
agent_task.status = FAILED
agent_task.outputJson.status = FAILED
agent_task.outputJson.recoverable = true
agent_trace.stepId = step_runtime_failure
agent_trace.status = FAILED
agent_trace.summary = Workflow {type} failed with INTERNAL_ERROR. Error: INTERNAL_ERROR
```

随后继续抛出原异常，让全局异常处理返回 500 `INTERNAL_ERROR`。

前置 DTO/payload 校验仍在 `startRun` 前完成，校验失败不创建 durable workflow。

## 4. 各 workflow 策略

| Workflow | 失败策略 | Retry 策略 |
|---|---|---|
| `RESOURCE_GENERATION` | 下游模型失败由资源生成链路写入 `step_resource`；Orchestrator 通用 `RuntimeException` fallback 写入 `step_runtime_failure`。失败摘要可包含 provider 错误，但不得包含完整 prompt 或生成内容。 | 对 `FAILED` workflow 可基于原 envelope 重建 workflow。retry 会生成新的 `requestId`，并在新 workflow envelope/response 中写入 `retryOfWorkflowId`，避免撞到原失败业务任务的幂等记录。 |
| `RAG_QA` | 权限等 `ApiException` 写入脱敏 `step_runtime_failure`，错误码保留；通用 `RuntimeException` 写入 `INTERNAL_ERROR` 脱敏失败 evidence。不写伪成功 query/citation。 | 对 `FAILED` workflow 可基于 envelope 中的脱敏 payload 重建，但 RAG envelope 仅保存 question hash/length，无法还原原 question。因此本切片只允许 retry 失败证据完整且 payload 可还原的 workflow；当前 `RAG_QA` retry 返回 409，调用方应重新提交原问题。 |
| `ANSWER_SUBMISSION` | Assessment 运行期异常写入 `step_runtime_failure`；answer 原文不进入 workflow envelope 或 failure summary。 | envelope 仅保存 `questionId` 和 `answerLength`，无法还原完整 answer；retry endpoint 返回 409，调用方应重新提交原答案。 |

## 5. 当前最小 retry 支持矩阵

| Workflow | Endpoint retry |
|---|---|
| `RESOURCE_GENERATION` | 支持 |
| `RAG_QA` | 409，需要重新 create |
| `ANSWER_SUBMISSION` | 409，需要重新 create |

说明：`RAG_QA` 和 `ANSWER_SUBMISSION` 的 workflow envelope 为脱敏快照，不能为了 retry 重新保存敏感输入。本切片优先保证失败证据和安全边界。

## 6. 测试

- `OrchestratorWorkflowControllerTest`
  - 通用 RuntimeException 失败证据。
  - retry FAILED resource workflow。
  - retry 非 FAILED workflow 返回 409。

测试命令：

```powershell
cd backend
mvn "-Dtest=OrchestratorWorkflowControllerTest" test
```
