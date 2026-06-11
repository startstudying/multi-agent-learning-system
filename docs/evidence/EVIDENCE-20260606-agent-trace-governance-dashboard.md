# Agent Trace 治理看板证据

## RED

命令：

```powershell
cd backend; mvn "-Dtest=AgentTraceControllerTest,AgentRunRecorderTest" test
```

结果：

- 失败，符合预期。
- 失败原因：缺少 `/api/agent/traces` 查询接口、`AgentRunRecorder.recordToolCall(...)`、tool call Java 映射、trace detail 的 `toolCalls` 和 retention policy。

## GREEN

命令：

```powershell
cd backend; mvn "-Dtest=AgentTraceControllerTest,AgentRunRecorderTest" test
```

结果：

- Tests run: 9
- Failures: 0
- Errors: 0
- BUILD SUCCESS

## 覆盖点

- 支持按用户、Agent 类型、状态、时间、失败原因过滤 trace。
- 支持服务层写入 `agent_tool_call`。
- tool call 输入/输出摘要会清理 secret、token、password、private document/full text。
- trace detail 返回 `toolCalls` 和 `retentionPolicy`。
- V15 为 `agent_tool_call` 补充 `trace_id`、`input_summary`、`output_summary`、`retention_class` 和查询索引。

## 仍需说明

- 本轮不做前端看板页面。
- 搜索实现为后端最小切片，后续可改为分页/Specification 查询。
