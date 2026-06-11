# Orchestrator 失败与重试策略 PRD

## 1. 背景

当前 Orchestrator 已支持 `RESOURCE_GENERATION`、`RAG_QA`、`ANSWER_SUBMISSION` 的统一入口和查询上下文，并已覆盖部分 `ApiException` 失败证据。但通用 `RuntimeException`、最小 workflow retry endpoint，以及逐 workflow 的失败/重试策略仍未闭环。

本切片聚焦 P0-1/P0-3 的 Orchestrator 最小补齐：失败必须可查，失败摘要必须脱敏，失败 workflow 必须能由 owner 发起最小重试。

## 2. 目标

- 通用 `RuntimeException` 发生在 workflow task 创建后时，保留 `FAILED agent_task` 和失败 `agent_trace`。
- `GET /api/orchestrator/workflows/{workflowId}` 能看到 `recentFailedStep`、失败 summary、trace summary 和 `RETRY_WORKFLOW`。
- 新增 `POST /api/orchestrator/workflows/{workflowId}/retry`。
- retry 仅允许 owner 对 `FAILED` workflow 发起；非 `FAILED` 终态返回 409。
- SPEC 明确 `RESOURCE_GENERATION`、`RAG_QA`、`ANSWER_SUBMISSION` 的失败/重试策略。

## 3. 非目标

- 不新增 workflow 独立表。
- 不新增数据库字段。
- 不实现后台调度、自动重试、指数退避或 retry queue。
- 不修改 RAG document upload/idempotency、`IndexService` 或共享 memory/changelog/todolist。

## 4. 成功标准

| 标准 | 验收方式 |
|---|---|
| RuntimeException 失败可查询 | Controller 测试断言 task/trace/GET workflow |
| 失败摘要脱敏 | 测试断言不包含原始 question/answer |
| retry endpoint 可用 | Controller 测试断言 FAILED workflow retry 生成新 workflow |
| 非 FAILED workflow 拒绝 retry | Controller 测试断言 409 |
| owner 权限落实 | 查询与 retry 都按 owner 定位 workflow |
