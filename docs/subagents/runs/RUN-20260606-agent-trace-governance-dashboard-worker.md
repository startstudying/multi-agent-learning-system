# Agent Trace 治理看板 Worker Run

## 角色

并行后端 worker，负责 P2-5「Agent Trace 治理看板」最小后端切片。

## 输入

- 用户指定工作区：`D:\多元agent`
- 用户指定允许写入范围
- 用户指定不修改共享收口文件、不修改 `evaluation` 包和 V14

## 已完成分析

- 已阅读 `AGENTS.md`、项目记忆、后端记忆、Agent/RAG 记忆、后端 TODO、架构基线与漂移检查。
- 已确认本地 migration 最大版本为 V14。
- 已确认 `agent_tool_call` 表在 V2 中存在，但缺少 Java entity/repository/service/API。
- 已确认现有 trace detail API 位于 `AgentTraceController`，查询逻辑当前在 `ResourceGenerationService.getTrace(...)`。

## 设计结论

- 新增 `AgentTraceGovernanceService` 承接 trace list/detail 聚合，避免继续扩大 `ResourceGenerationService` 职责。
- `AgentTraceController` 保留 `/api/agent/tasks/{taskId}/trace` 与 cancel，并新增 `/api/agent/traces`。
- `AgentRunRecorder` 新增服务层 tool call 写入入口，统一敏感字段清理。
- V15 只补 `agent_tool_call` 的治理列与索引，不修改已有 migration。

## 风险控制

- 当前目录不是 git repository，无法用 git status 审计并行改动。
- 严格只修改 Context Pack 允许文件。
- 不新增依赖。
- 不保存 raw secret / full private document。
