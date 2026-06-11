# Orchestrator RAG_QA 上下文收敛 PRD

## 背景

当前 Orchestrator 已支持统一 workflow 入口，并且 `RESOURCE_GENERATION` 已经接入同一个 `agentTaskId/traceId` 执行上下文。但课程资料问答仍主要通过 `/api/rag/query` 和 `/api/tutor/ask` 直接调用 `RagQueryService`，没有纳入统一 `workflowId/agentTaskId/traceId` 的 Agent Trace 视图。

RAG 查询本身已经具备权限过滤、query log、source citation 和 `traceId` 字段，因此适合先做 P0-1 的最小接入切片。

## 目标

完成 P0-1 的 `RAG_QA` 最小接入：通过 `POST /api/orchestrator/workflows` 创建 `workflowType=RAG_QA` 时，Orchestrator 创建统一 workflow envelope 和 Agent task，调用 RAG 查询并复用同一个 `traceId`，随后在同一个 `agent_task` 下追加 RAG trace steps。

## 用户价值

- 课程资料问答不再是孤立接口，能通过 `workflowId` 查询完整过程。
- RAG query log、source citation 和 Agent Trace 使用同一个 `traceId`，便于审计。
- 后续可在同一 Orchestrator 模型下继续接入 `ANSWER_SUBMISSION`。

## 非目标

- 本轮不接入 `ANSWER_SUBMISSION`。
- 本轮不修改 `/api/rag/query` 和 `/api/tutor/ask` 的现有合同。
- 本轮不新增独立 workflow 表。
- 本轮不实现 workflow retry/recovery。
- 本轮不新增数据库迁移。

## 成功标准

- `workflowType=RAG_QA` 成功请求返回 `DONE`。
- 响应包含同一 `workflowId/agentTaskId/traceId`。
- trace steps 包含 `workflow_start` 和 RAG 子步骤，sequence 连续。
- `kb_query_log.traceId` 等于 Orchestrator traceId。
- 有来源时 `source_citation.traceId` 等于 Orchestrator traceId。
- 无来源时 workflow 仍为 `DONE`，但不写 source citation。
- 无效 payload 不创建半成品 workflow task。
