# Agent Trace 治理看板后端最小切片 PRD

## 背景

P2-5 要求把现有 `agent_task`、`agent_trace`、`model_call_log`、`token_usage_log` 变成可查询、可解释、可治理的后端能力。当前系统已经能按单个 `agentTaskId` 查看 trace，但还缺少看板所需的横向过滤、工具调用明细展示，以及 trace retention policy 字段说明。

同时，数据库基线 `V2__learning_agent_loop.sql` 已经包含 `agent_tool_call` 表，但 Java 后端没有实体、仓储、服务层写入或 API 展示，导致工具调用治理只停留在 schema 层。

## 目标

- 提供 Agent Trace 治理看板的最小后端查询接口。
- 支持按用户、Agent 类型、状态、时间窗口、失败原因过滤 trace 任务。
- 在服务层写入 `agent_tool_call`，并通过 trace 详情接口展示工具调用。
- 返回 trace retention policy，区分长期保留的审计字段和可清理的大文本字段。
- 对 tool call 输入、输出、错误信息做敏感字段清理，只保存 sanitized summary。

## 非目标

- 不实现前端看板页面。
- 不实现真实定时清理任务。
- 不修改 `rag`、`assessment`、`evaluation`、`orchestrator` 包。
- 不新增依赖。
- 不修改共享收口文件：`docs/changelog/CHANGELOG.md`、`docs/memory/*`、`docs/planning/backend-architecture-todolist.md`。

## 用户价值

- 管理员可以快速筛选失败或高风险 Agent 执行。
- 教师/治理人员可以看到工具调用是否成功、耗时和安全摘要。
- 后续前端看板可以直接消费最小 API，不需要读取 raw prompt、raw secret 或课程全文。

## 验收标准

- `GET /api/agent/traces` 支持 `userId`、`agentType`、`status`、`from`、`to`、`failureReason` 查询参数。
- `GET /api/agent/tasks/{taskId}/trace` 返回 `toolCalls` 和 `retentionPolicy`。
- `AgentRunRecorder` 能服务层写入 `agent_tool_call`，并清理 secret/private doc/raw token 等敏感内容。
- focused 测试命令通过：`cd backend; mvn "-Dtest=AgentTraceControllerTest,AgentRunRecorderTest" test`。
