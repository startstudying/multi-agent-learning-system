# Orchestrator Resource Generation 上下文统一任务

## 任务清单

- [x] 读取项目记忆、P0 TODO、Orchestrator 和资源生成代码。
- [x] 复用上一轮 subagent 结论，并启动本轮只读方案审查 subagent。
- [x] 创建本轮 workflow 文档。
- [x] 写失败测试覆盖 Orchestrator 发起资源生成复用上下文。
- [x] 实现 `AgentRunRecorder` trace 追加序号。
- [x] 实现 `ResourceGenerationService` workflow-aware 入口。
- [x] 实现 `OrchestratorWorkflowService` 调用资源生成。
- [x] 跑聚焦测试。
- [x] 跑全量后端测试。
- [x] 更新 Evidence、Acceptance、Memory、Changelog、总 TODO。

## Done Criteria

- [x] `POST /api/orchestrator/workflows` with `RESOURCE_GENERATION` 返回 `WAITING_REVIEW`。
- [x] 返回的 `agentTaskId` 等于 `resource_generation_task.agentTaskId`。
- [x] 返回的 `traceId` 等于 `resource_generation_task.traceId`。
- [x] 同一 `agent_task` 下 trace steps 有 8 个，且 sequence 从 1 到 8。
- [x] `GET /api/orchestrator/workflows/{workflowId}` 返回同一上下文和完整 steps。
- [x] 直接 `/api/resources/generation-tasks` 行为不回退。
- [x] `mvn test` 通过或记录失败原因。
