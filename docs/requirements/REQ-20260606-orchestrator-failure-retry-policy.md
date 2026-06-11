# Orchestrator 失败与重试策略需求

## 1. 追踪

- PRD：`docs/product/PRD-20260606-orchestrator-failure-retry-policy.md`
- 需求编号：REQ-20260606-orchestrator-failure-retry-policy

## 2. 功能需求

| 编号 | 需求 | 优先级 | 验收方式 |
|---|---|---|---|
| FR-01 | workflow task 创建后发生通用 `RuntimeException` 时，`agent_task.status` 必须写为 `FAILED` | 必须 | 测试读取 repository |
| FR-02 | 通用失败必须追加 `step_runtime_failure` trace，且 summary 不包含原始 question/answer/document content | 必须 | 测试断言 trace |
| FR-03 | `GET workflow` 必须返回最近失败步骤和失败 trace summary | 必须 | MockMvc GET 断言 |
| FR-04 | 新增 `POST /api/orchestrator/workflows/{workflowId}/retry` | 必须 | MockMvc POST 断言 |
| FR-05 | retry 仅允许当前 owner 操作自己的 workflow | 必须 | 服务按 owner 查询，缺失或越权返回 `NOT_FOUND` |
| FR-06 | retry 只允许 `FAILED` workflow；非 `FAILED` 返回 409 `CONFLICT` | 必须 | Controller 测试覆盖 |
| FR-07 | retry 最小实现基于原 workflow envelope 重建新 workflow，返回新的 `workflowId/agentTaskId/traceId` | 必须 | 测试断言新旧 ID 不同 |

## 3. 约束

- retry request body 可为空。
- 本切片不改下游业务幂等规则；如果原始 `requestId` 已被下游业务占用，下游服务仍可按既有规则返回 replay 或 conflict。
- `ANSWER_SUBMISSION` envelope 只保留 `answerLength`，无法从脱敏 envelope 还原完整 answer；因此最小 retry endpoint 对该类型返回 409，提示使用新的 create request 重试。

## 4. 安全需求

- 失败 summary 不记录完整用户输入。
- owner 以 `CurrentUserService.currentUserId()` 为准。
- 不新增依赖，不写 secrets。
