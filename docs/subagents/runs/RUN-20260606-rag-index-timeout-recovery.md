# Subagent Run: RAG 索引任务超时恢复

## Subagent Decision

Use Subagents: Yes。

Reason: 用户明确要求启动多 subagent 并行开发；总 TODO 剩余项横跨 P0-1 Orchestrator、P0-3 RAG 幂等恢复、P0-4 Review Gate 状态治理，适合 L1 并行分析。

Parallelism Level: L1。

Selected Subagents:

- Franklin：P0-1 Orchestrator Workflow 下一步切片审查。
- Cicero：P0-3/P0-4 安全与幂等治理审查。

Implementation Mode: Single Codex implementation with parallel analysis。

## 已收到结论

### Cicero / P0-3/P0-4 安全与幂等治理

结论：推荐下一轮实现 P0-3 “RAG 文档索引 `RUNNING` 超时恢复”最小切片。原因是 `kb_index_task` 已有 `retry_count/error_message/started_at/finished_at/updated_at` 字段，不需要迁移；改动集中在实体、Repository、Service 和测试；风险低于文档上传幂等、RAG query 重放和 Review Gate 完整状态模型。

本轮处理：采纳。实现服务层恢复方法，不新增公开 API，不新增迁移。

### Franklin / P0-1 Orchestrator Workflow

结论：建议下一轮优先接入 `RAG_QA` 到统一 Orchestrator workflow context。原因是 RAG 查询链路已有 `traceId`、query log、source citation 和权限过滤，最小切片可以不改数据库 schema，只需让 Orchestrator 传入同一 `traceId` 并追加 RAG trace steps。

本轮处理：记录为下一轮 P0-1 切片；本轮继续完成 P0-3 RAG 索引超时恢复，因为改动更集中且已进入实现流程。
