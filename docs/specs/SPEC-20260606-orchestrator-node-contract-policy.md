# Orchestrator 节点契约与策略显式化 SPEC

## 1. API 变更

现有接口保持不变：

```http
POST /api/orchestrator/workflows
GET /api/orchestrator/workflows/{workflowId}
POST /api/orchestrator/workflows/{workflowId}/retry
```

`steps[]` 和 `recentFailedStep` 增加字段：

```json
{
  "stepId": "step_runtime_failure",
  "agentName": "Orchestrator",
  "status": "FAILED",
  "summary": "Workflow RAG_QA failed with FORBIDDEN. Error: FORBIDDEN",
  "latencyMs": 0,
  "model": null,
  "promptVersion": null,
  "sequenceNo": 2,
  "inputDto": "RagQaWorkflowRequest",
  "outputDto": "WorkflowFailureEvidence",
  "failurePolicy": "SANITIZE_AND_PERSIST_FAILED_TRACE",
  "retryPolicy": "RESUBMIT_ORIGINAL_REQUEST",
  "retryable": false
}
```

## 2. 节点契约矩阵

| Workflow | Step | Input DTO | Output DTO | Failure Policy | Retry Policy | Retryable |
|---|---|---|---|---|---|---|
| all | `workflow_start` | `CreateWorkflowRequest` | `OrchestratorWorkflowResponse` | `VALIDATE_BEFORE_TASK_OR_PERSIST_RUNTIME_FAILURE` | 按 workflow 决定 | 按 workflow 决定 |
| `RESOURCE_GENERATION` | `step_planner` / `step_teacher` / `step_resource` / `step_question` / `step_critic` / `step_tutor` / `step_safety` | `ResourceGenerationRequest` | `ResourceGenerationResponse` | `PERSIST_FAILED_TRACE` | `RETRY_WORKFLOW_RESOURCE_GENERATION_ONLY` | true |
| `RAG_QA` | `step_rag_safety` / `step_rag_retrieval` / `step_rag_answer` | `RagQaWorkflowRequest` | `RagQueryResponse` | `SANITIZE_AND_PERSIST_FAILED_TRACE` | `RESUBMIT_ORIGINAL_REQUEST` | false |
| `ANSWER_SUBMISSION` | `step_assessment_safety` / `step_assessment_grading` / `step_assessment_feedback` / `step_assessment_mastery` / `step_assessment_replan` | `AnswerSubmitRequest` | `AnswerSubmitResponse` | `SANITIZE_AND_PERSIST_FAILED_TRACE` | `RESUBMIT_ORIGINAL_REQUEST` | false |
| all | `step_runtime_failure` | 按 workflow 决定 | `WorkflowFailureEvidence` | `SANITIZE_AND_PERSIST_FAILED_TRACE` | 按 workflow 决定 | 按 workflow 决定 |

## 3. nextActions 策略

| 状态 | Workflow | nextActions |
|---|---|---|
| `FAILED` | `RESOURCE_GENERATION` | `INSPECT_TRACE`, `RETRY_WORKFLOW` |
| `FAILED` | `RAG_QA` | `INSPECT_TRACE`, `RESUBMIT_ORIGINAL_REQUEST` |
| `FAILED` | `ANSWER_SUBMISSION` | `INSPECT_TRACE`, `RESUBMIT_ORIGINAL_REQUEST` |
| `WAITING_REVIEW` | 任意 | `OPEN_REVIEW_QUEUE`, `CHECK_STATUS` |
| `DONE` | 任意 | `VIEW_RESULT` |

## 4. 数据库

不新增迁移。契约是代码级策略矩阵，响应由 `agent_trace.stepId` 和 workflow envelope 中的 `workflowType` 推导。

## 5. 已知风险

- `LEARNING_GOAL_CREATION` 枚举仍存在，但没有完整下游执行链路，本切片不将其计入已完成节点契约。
- `RESOURCE_GENERATION` 模型失败路径当前仍可能在 `step_resource.summary` 中暴露 provider 异常 message，后续应改为稳定错误码和受控内部日志。
- `ANSWER_SUBMISSION` workflow envelope 仍以 `answerLength` 做脱敏匹配，后续应补 `answerHash` 以避免同长度不同答案的 replay 完整性风险。
