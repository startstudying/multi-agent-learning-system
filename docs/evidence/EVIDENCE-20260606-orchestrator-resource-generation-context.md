# Orchestrator Resource Generation 上下文统一证据

## 范围

本轮完成 P0-1 的 `RESOURCE_GENERATION` 最小接入：通过 Orchestrator 创建资源生成 workflow 时，资源生成子流程复用同一个 `agentTaskId/traceId`，并把资源生成 Agent 步骤追加到同一条 `agent_trace`。

## 代码证据

- `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java`
  - 在 `workflowType=RESOURCE_GENERATION` 时解析 `payloadJson` 为 `ResourceGenerationRequest`。
  - 创建 workflow envelope 和 `workflow_start` 后调用 `ResourceGenerationService.createTaskInWorkflow(...)`。
  - 创建响应从 `agent_task` 重新读取实际状态，不再硬编码 `RUNNING`。
  - 外层事务增加 `noRollbackFor = AiModelGateway.ModelCallFailedException.class`，避免失败证据被回滚。
- `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java`
  - 增加 workflow-aware 入口，复用传入的 `AgentExecutionContext`。
  - `resource_generation_task.agentTaskId/traceId` 使用 Orchestrator 上下文。
  - `requestId` 命中其他 workflow 时返回 409，避免旧任务被绑定到新 workflow。
- `backend/src/main/java/com/learningos/agent/application/AgentRunRecorder.java`
  - `recordTraceSteps(...)` 按已有 trace 数量追加 `sequenceNo`，支持 `workflow_start` 后追加子流程步骤。

## TDD 过程

### RED

先补测试后运行：

```text
cd backend
mvn "-Dtest=OrchestratorWorkflowControllerTest,AgentRunRecorderTest" test
```

结果：失败符合预期。

关键失败点：

- Orchestrator 创建 `RESOURCE_GENERATION` 仍返回 `RUNNING`。
- trace 只有 `workflow_start` 1 步。
- 模型失败路径返回 200，说明资源生成子流程尚未执行。
- invalid resource payload 返回 200，说明 payload 未校验。
- `AgentRunRecorder.recordTraceSteps(...)` 二次调用后 sequence 从 1 重新开始。

### GREEN：聚焦测试

实现后运行：

```text
cd backend
mvn "-Dtest=OrchestratorWorkflowControllerTest,AgentRunRecorderTest,ResourceGenerationControllerTest" test
```

结果：

```text
Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-06T01:57:53+08:00
```

### GREEN：全量后端测试

```text
cd backend
mvn test
```

结果：

```text
Tests run: 94, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-06T01:59:26+08:00
```

## Subagent 审查证据

本轮启动只读 code-reviewer subagent `Sagan` 审查 P0-1 方案。采纳的关键意见：

- 必须测试 `workflow_start + 7` 个资源生成步骤，且 sequence 为 `1..8`。
- 必须覆盖失败路径，避免 Orchestrator 外层事务回滚失败证据。
- 必须明确 `requestId` 命中旧资源任务时不能绑定到新 workflow。
- 必须补 `AgentRunRecorder` 二次追加 trace 的单测。

## 架构漂移检查

| 检查项 | 结果 | 说明 |
|---|---|---|
| Backend layering | PASS | Controller 仍只委托 Service；Orchestrator 编排在 Service 层。 |
| Frontend rules | PASS | 未修改前端。 |
| Agent / RAG rules | PASS | 资源生成仍写 `agent_task`、`agent_trace`、model/token log；RAG 未改。 |
| Security | PASS | `learnerId` 与当前用户不一致时拒绝；未新增依赖或密钥。 |
| API / Database | PASS | API 行为按本轮 SPEC 扩展；未改数据库 schema。 |

## 已知限制

- P0-1 只接入 `RESOURCE_GENERATION`，`RAG_QA` 和 `ANSWER_SUBMISSION` 仍未统一到 Orchestrator 上下文。
- workflow 查询仍依赖 `agent_task.inputJson` envelope marker，后续高频查询或恢复任务建议引入独立 workflow 表。
- Orchestrator 级 `requestId` 幂等仍未完整设计；本轮只避免旧资源任务被错误绑定到新 workflow。
