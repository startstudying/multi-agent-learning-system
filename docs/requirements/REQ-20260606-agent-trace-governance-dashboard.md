# Agent Trace 治理看板后端最小切片需求

## 功能需求

1. Trace 列表查询
   - API：`GET /api/agent/traces`
   - 查询参数：
     - `userId`
     - `agentType`
     - `status`
     - `from`
     - `to`
     - `failureReason`
   - 返回任务级列表，包含 `taskId`、`traceId`、`userId`、`agentType`、`status`、`failureReason`、`latencyMs`、`createdAt`、`updatedAt`、`stepCount`、`toolCallCount`。

2. Trace 详情展示
   - 保持现有 API：`GET /api/agent/tasks/{taskId}/trace`
   - 返回新增字段：
     - `toolCalls`
     - `retentionPolicy`
   - `steps` 保持兼容。

3. Tool call 写入
   - 服务层新增 `AgentRunRecorder.recordToolCall(...)`。
   - 写入表：`agent_tool_call`。
   - 写入字段：`agentTaskId`、`toolName`、`inputJson`、`outputJson`、`status`、`errorMessage`、`latencyMs`、`traceId`。

4. Retention policy
   - API 响应中返回固定策略：
     - 长期保留字段：`taskId`、`traceId`、`userId`、`agentType`、`status`、`failureReason`、`latencyMs`、`createdAt`、`updatedAt`、`toolName`、`toolStatus`。
     - 可清理字段：`agent_task.inputJson`、`agent_task.outputJson`、`agent_trace.summary`、`agent_tool_call.inputJson`、`agent_tool_call.outputJson`、`agent_tool_call.errorMessage`。
   - 最小切片只返回策略，不执行物理清理。

## 安全需求

- 不保存 raw secret、API key、password、token。
- 不保存 full private document 或 raw course document。
- `inputJson` 和 `outputJson` 只保存 sanitized summary。
- provider 原始错误或工具原始异常不得原样保存到展示字段。

## 兼容需求

- 现有 `GET /api/agent/tasks/{taskId}/trace` 的 `taskId`、`status`、`steps`、`traceId` 字段保持不变。
- 不改已有 migration，若需要迁移只新增 `V15__agent_tool_call_trace_governance.sql`。
- 不新增依赖。

## 测试需求

- Controller/API test 覆盖过滤参数。
- Service/recorder test 覆盖 tool call 写入、sanitized summaries、retention policy 字段/响应。
- 至少运行：`cd backend; mvn "-Dtest=AgentTraceControllerTest,AgentRunRecorderTest" test`。
