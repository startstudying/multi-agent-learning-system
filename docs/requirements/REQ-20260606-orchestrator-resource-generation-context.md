# Orchestrator Resource Generation 上下文统一需求

## 功能需求

| ID | Requirement |
|---|---|
| FR-01 | `POST /api/orchestrator/workflows` 在 `workflowType=RESOURCE_GENERATION` 时必须执行资源生成子流程。 |
| FR-02 | 资源生成子流程必须复用 Orchestrator 已创建的 `agentTaskId`。 |
| FR-03 | 资源生成子流程必须复用 Orchestrator 已创建的 `traceId`。 |
| FR-04 | `agent_trace` 必须按顺序包含 `workflow_start` 和资源生成 Agent 步骤，且 `sequenceNo` 不重复。 |
| FR-05 | 创建响应和查询响应的 `status` 必须反映资源生成后的实际任务状态。 |
| FR-06 | 直接调用 `/api/resources/generation-tasks` 时仍保持现有独立任务行为。 |
| FR-07 | 未审核资源正文仍受 Review Gate 约束，不因 Orchestrator 调用而泄漏。 |

## 非功能需求

| ID | Requirement |
|---|---|
| NFR-01 | 不新增依赖。 |
| NFR-02 | 不新增数据库迁移。 |
| NFR-03 | Controller 只做 HTTP 委托，编排逻辑位于 Service 层。 |
| NFR-04 | Agent 任务状态流转继续通过 `AgentRunRecorder`。 |
| NFR-05 | 失败时必须保留已有 `FAILED` trace/model call 行为。 |

## 输入要求

`payloadJson` 至少支持：

```json
{
  "goalId": "goal_spring_boot",
  "pathNodeId": "node_sql_join",
  "resourceTypes": ["LECTURE", "EXERCISE"]
}
```

如果 `resourceTypes` 缺失，后端可以使用资源生成服务的默认资源类型集合。

## 验收要求

- 聚焦测试覆盖 Orchestrator 发起资源生成后的上下文统一。
- 聚焦测试覆盖直接资源生成接口仍独立创建 `agentTaskId`。
- 全量后端测试通过。
