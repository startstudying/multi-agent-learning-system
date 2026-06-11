# Agent Trace 治理看板后端最小切片任务

## TASK-1 文档与边界

- [x] 阅读项目记忆、后端记忆、Agent/RAG 记忆、TODO、架构基线。
- [x] 检查 migration 最大版本，确认本地最大为 V14。
- [x] 创建 PRD / REQ / SPEC / PLAN / TASK / CONTEXT / worker run 文档。
- [x] 明确不修改共享收口文件。

## TASK-2 RED 测试

- [ ] `AgentTraceControllerTest` 覆盖 trace 查询过滤参数。
- [ ] `AgentTraceControllerTest` 覆盖 trace detail 的 `toolCalls` 和 `retentionPolicy`。
- [ ] `AgentRunRecorderTest` 覆盖 tool call 写入与敏感字段清理。
- [ ] 运行 focused 测试并确认 RED。

## TASK-3 GREEN 实现

- [ ] 新增 `AgentToolCall` entity 和 repository。
- [ ] 新增 `AgentTraceGovernanceService`。
- [ ] 扩展 DTO。
- [ ] 扩展 `AgentRunRecorder.recordToolCall(...)`。
- [ ] 更新 `AgentTraceController`。
- [ ] 新增 `V15__agent_tool_call_trace_governance.sql`。

## TASK-4 验证与收口

- [ ] 运行 `cd backend; mvn "-Dtest=AgentTraceControllerTest,AgentRunRecorderTest" test`。
- [ ] 创建 Evidence。
- [ ] 创建 Acceptance。
- [ ] 记录未完成风险。

## Done Criteria

- 查询过滤、tool call 展示、retention policy 均有测试覆盖。
- tool call 输入/输出/错误展示为 sanitized summary。
- 不新增依赖。
- 不修改 `rag`、`assessment`、`evaluation` 和共享收口文件。
