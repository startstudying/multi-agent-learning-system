# Orchestrator Resource Generation 上下文统一 PRD

## 背景

当前 Orchestrator 已能创建 workflow envelope，并可按 `workflowId` 查询 `agent_task` 与 `agent_trace` 摘要。但资源生成接口仍会自行创建新的 `agent_task` 与 `traceId`，导致通过 Orchestrator 发起的 `RESOURCE_GENERATION` 只是一个外层壳，实际资源生成链路不在同一个 `workflowId / agentTaskId / traceId` 上。

## 目标

完成 P0-1 的最小闭环切片：通过 `POST /api/orchestrator/workflows` 发起 `RESOURCE_GENERATION` 时，资源生成子流程复用 Orchestrator 创建的 `agentTaskId` 和 `traceId`，并把资源生成步骤追加到同一条 `agent_trace` 中。

## 用户价值

- 管理员或教师可以用一个 `workflowId` 查到资源生成的完整 Agent 步骤。
- 学生端或前端只需要保存 workflow 级上下文，不需要拼接多个孤立任务。
- 后续 RAG、Assessment 接入 Orchestrator 时有可复用模式。

## 非目标

- 本轮不新增独立 workflow 表。
- 本轮不接入 RAG QA 和 Answer Submission。
- 本轮不新增数据库迁移。
- 本轮不改变直接调用 `/api/resources/generation-tasks` 的行为。
- 本轮不实现 workflow retry/recovery。

## 成功标准

- Orchestrator 创建 `RESOURCE_GENERATION` 后，返回状态为 `WAITING_REVIEW`。
- `resource_generation_task.agentTaskId` 等于 Orchestrator 返回的 `agentTaskId`。
- `resource_generation_task.traceId` 等于 Orchestrator 返回的 `traceId`。
- `agent_trace` 同一任务下包含 `workflow_start` 与资源生成 Agent 步骤。
- `GET /api/orchestrator/workflows/{workflowId}` 能看到完整步骤和审核等待状态。
