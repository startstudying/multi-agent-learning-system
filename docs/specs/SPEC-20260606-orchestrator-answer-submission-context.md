# Orchestrator ANSWER_SUBMISSION 上下文收敛规格

## API

复用现有入口：

```http
POST /api/orchestrator/workflows
```

请求示例：

```json
{
  "workflowType": "ANSWER_SUBMISSION",
  "learnerId": "alice",
  "payloadJson": "{\"questionId\":\"q_sql_join\",\"answer\":\"JOIN duplicates happen when a one-to-many relation is joined.\"}",
  "requestId": "req_answer_workflow_once"
}
```

## Payload Adapter

Orchestrator 解析后构造现有 DTO：

```java
new AnswerSubmitRequest(
    request.learnerId(),
    payload.questionId,
    payload.answer,
    request.requestId()
)
```

`learnerId` 和 `requestId` 只从外层 `CreateWorkflowRequest` 读取，避免内外层冲突。

## Orchestration Flow

```text
validate owner / requestId / payload
-> replay existing workflow if assessment request already exists
-> start agent_task
-> record workflow_start
-> AssessmentService.submitAnswerWithTraceId(...)
-> record assessment trace steps
-> transition agent_task to DONE
-> return workflow response
```

## Trace Steps

| Step ID | Agent | Status |
|---|---|---|
| `workflow_start` | `Orchestrator` | `RUNNING` |
| `step_assessment_safety` | `AssessmentAgent` | `DONE` |
| `step_assessment_grading` | `AssessmentAgent` | `DONE` |
| `step_assessment_feedback` | `FeedbackDiagnosisAgent` | `DONE` |
| `step_assessment_mastery` | `KnowledgeDiagnosisAgent` | `DONE` |
| `step_assessment_replan` | `LearningPathPlannerAgent` | `DONE` |

## Replay Strategy

最小实现不新增 workflow 表。Orchestrator 在创建新 task 前调用 assessment service 做 replay preflight：

- 如果不存在同 learner + requestId 的 answer，则创建新 workflow。
- 如果存在且 payload hash 一致，则查找首次精确匹配的 `ANSWER_SUBMISSION` workflow 并返回其 status context。
- 如果存在且 payload hash 不一致，则抛出 `409 CONFLICT`。

workflow 候选通过 `agent_task.inputJson` marker 查询后必须解析 envelope，再精确匹配：

- `workflowType = ANSWER_SUBMISSION`
- `ownerUserId = current user`
- `learnerId = request.learnerId`
- `requestId = request.requestId`
- `payload.questionId = request.questionId`
- `payload.answerLength = request.answer.length`
- 当调用方要求当前 trace 绑定时，`traceId` 也必须一致

首次 workflow 通过现有 `agent_task.inputJson` envelope 查询，后续应在 P0-3 设计独立 workflow 表或 workflow-level unique key。

## Trace Drift Strategy

并发或幂等重放下，`AssessmentService.submitAnswerWithTraceId(...)` 可能返回已有 answer snapshot，snapshot 内的 `traceId` 属于首次 winning workflow，而不是当前新建的 Orchestrator 上下文。

处理策略：

```text
start transient workflow
-> assessment returns response with different traceId
-> locate exact winning workflow by response.traceId
-> return winning workflow status context
-> delete transient loser agent_trace and agent_task
```

这样避免同一次答题提交留下两个 workflow 任务，同时保持业务记录 traceId 与首次成功 workflow 对齐。

## Failure Strategy

本切片只处理前置校验和成功路径。业务冲突在 task 创建前返回。运行期 durable failure/retry/recovery 留给后续 P0-1/P0-3 workflow node failure strategy。

## Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 仍只委托 service。 |
| Agent trace | PASS | Orchestrator 负责 `agent_task` 和 `agent_trace`。 |
| Repository boundary | PASS | Orchestrator 不直接访问 assessment repository。 |
| API / DB | PASS | 不新增 endpoint，不改 schema。 |
| Dependency | PASS | 不新增依赖。 |
