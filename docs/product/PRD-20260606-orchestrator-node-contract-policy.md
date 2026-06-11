# Orchestrator 节点契约与策略显式化 PRD

## 1. 背景

当前 Orchestrator 已把 `RESOURCE_GENERATION`、`RAG_QA`、`ANSWER_SUBMISSION` 纳入统一 `workflowId / agentTaskId / traceId` 上下文，并能查询步骤、失败步骤和 trace 摘要。但每个 workflow 节点的输入 DTO、输出 DTO、失败策略和重试策略仍隐含在代码和历史 SPEC 中，前端、测试和答辩材料无法直接从 API 看到节点级契约。

本切片用于关闭后端 TODO P0-1 的剩余项：把已执行节点的契约作为稳定响应字段暴露出来，不新增工作流表，不实现新的业务工作流。

## 2. 目标

- 在 Orchestrator workflow 响应的 `steps` 和 `recentFailedStep` 中展示节点契约。
- 每个节点至少包含 `inputDto`、`outputDto`、`failurePolicy`、`retryPolicy`、`retryable`。
- `RESOURCE_GENERATION` 明确支持失败 workflow endpoint retry。
- `RAG_QA` 和 `ANSWER_SUBMISSION` 明确需要调用方重新提交原始请求，避免从脱敏 envelope 还原敏感内容。
- 修正失败 workflow 的 `nextActions`，使其与真实 retry 能力一致。

## 3. 非目标

- 不新增数据库表或迁移。
- 不实现节点级自动重试、退避队列、后台恢复。
- 不实现 `LEARNING_GOAL_CREATION` 下游业务链路。
- 不把原始 RAG question 或 answer 写回 workflow envelope。

## 4. 成功标准

| 标准 | 验收方式 |
|---|---|
| 资源生成 workflow 返回节点契约 | MockMvc 断言 `steps[*]` 字段 |
| RAG/答题失败步骤返回脱敏失败与重提策略 | MockMvc 断言 `recentFailedStep` 字段 |
| 失败 nextActions 与 retry 能力一致 | `RESOURCE_GENERATION` 返回 `RETRY_WORKFLOW`，RAG/答题返回 `RESUBMIT_ORIGINAL_REQUEST` |
| 不引入 schema 变更 | 代码审查和迁移目录无新增 SQL |
