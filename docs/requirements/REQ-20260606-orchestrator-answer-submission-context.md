# Orchestrator ANSWER_SUBMISSION 上下文收敛需求

## Functional Requirements

| ID | Requirement |
|---|---|
| FR-01 | `POST /api/orchestrator/workflows` 必须支持 `workflowType=ANSWER_SUBMISSION` 执行真实 answer submission 子流程。 |
| FR-02 | 外层 `learnerId` 必须等于当前用户，否则返回 `403 FORBIDDEN`。 |
| FR-03 | `ANSWER_SUBMISSION` 必须要求外层 `requestId` 非空。 |
| FR-04 | `payloadJson` 必须包含非空 `questionId` 和非空 `answer`。 |
| FR-05 | payload 校验必须发生在创建 `agent_task` 前。 |
| FR-06 | Assessment 子流程必须复用 Orchestrator `traceId`。 |
| FR-07 | 成功 workflow 必须转为 `DONE`。 |
| FR-08 | 成功 workflow 必须包含 assessment 子步骤 trace。 |
| FR-09 | 同 learner + requestId + same payload 必须 replay 首次 workflow response。 |
| FR-10 | replay workflow 必须精确匹配 `workflowType`、`ownerUserId`、`learnerId`、`requestId`、`questionId`、`answerLength`，不能只按 `requestId` 返回第一条候选。 |
| FR-11 | 如果 assessment 幂等层返回已有提交且 traceId 与当前 workflow 不同，必须返回已有 winning workflow，并清理当前 transient task/trace。 |
| FR-12 | 同 learner + requestId + different payload 必须返回 `409 CONFLICT`，且不新增 workflow 或业务记录。 |
| FR-13 | 直接 `/api/assessment/answers` 行为保持兼容。 |

## Non-functional Requirements

| ID | Requirement |
|---|---|
| NFR-01 | 不新增依赖。 |
| NFR-02 | 不新增数据库 migration。 |
| NFR-03 | Orchestrator 不直接访问 assessment repository。 |
| NFR-04 | 所有权限检查必须在后端 Service 层完成。 |
| NFR-05 | `requestId` 校验必须在 service 层存在，不能只依赖 Controller 或 Orchestrator。 |
| NFR-06 | 测试必须覆盖 create、get、invalid payload、replay、exact replay、trace drift、conflict、direct assessment 兼容。 |
