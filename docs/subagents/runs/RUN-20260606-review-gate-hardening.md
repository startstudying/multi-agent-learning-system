# Subagent Run: Review Gate 强约束

## Subagent Decision

Use Subagents: Yes。

Reason: 用户明确要求启动多 subagent 并行开发；总 TODO 横跨 P0-1、P0-3、P0-4，适合 L1 并行分析。

Parallelism Level: L1。

Selected Subagents:

- Sartre：P0-3 RAG 文档上传/索引/RAG 查询幂等与恢复审查。
- Fermat：P0-4 Review Gate 绕过路径安全审查。
- Epicurus：P0-1 Orchestrator Workflow 剩余项架构审查。

Implementation Mode: Single Codex implementation with parallel analysis。

## 已收到结论

### Sartre / P0-3 RAG 幂等与恢复

结论：当前文档上传、索引任务、RAG 查询都没有 `requestId` 或业务唯一键。建议本轮若做 P0-3，只实现“索引任务 active 去重”：`createPendingTask(document)` 在最新任务为 `PENDING/RUNNING` 时返回已有任务，`FAILED/SUCCEEDED` 时创建新任务。上传 requestId 和 RAG 查询重放需要迁移和响应快照，建议下一轮完整设计。

本轮处理：记录为后续 P0-3 最小切片，不与 P0-4 同时实现，避免扩散。

### Fermat / P0-4 Review Gate 安全审查

结论：FAIL。`POST /api/resources/generation-tasks` 和 `GET /api/resources/generation-tasks/{taskId}` 会在 `PENDING_CRITIC / WAITING_REVIEW` 状态下返回 `markdownContent`；`canReleaseToLearner(taskId)` 只保护了正式学生发布端点。

本轮处理：已在 `ResourceGenerationService` 的响应序列化出口按 `canReleaseToLearner` 判断是否填充正文；未审核时 `GeneratedResourceResponse.markdownContent` 为 `null` 且不序列化。覆盖了创建响应、任务详情、requestId 幂等重放路径。

### Epicurus / P0-1 Orchestrator Workflow 审查

结论：当前 Orchestrator 仍是 workflow envelope / query 层，未真正把资源生成、RAG、答题反馈等子流程纳入同一个 `workflowId/agentTaskId/traceId` 执行上下文。建议下一轮先做 `RESOURCE_GENERATION` 复用 Orchestrator 上下文的最小切片，再扩展 RAG 和 Assessment。

本轮处理：记录为后续 P0-1 最小切片；本轮先完成 P0-4，因为审核状态会影响资源生成工作流终态和 learner 可见性。
