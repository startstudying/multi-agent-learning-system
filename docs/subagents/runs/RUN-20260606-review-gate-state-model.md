# Subagent Run: Review Gate 状态模型加固

## Subagent Decision

Use Subagents: Yes。

Reason: 用户要求启动多 subagent 并行开发；当前后端 TODO 横跨 Orchestrator、RAG、Review Gate、安全和测试，适合 L1 并行审查。

Parallelism Level: L1。

Implementation Mode: Single Codex implementation with parallel analysis。当前切片文件集中在 Review Gate，避免并行写入冲突。

## 已收到结论

### Architecture / P0 Review

结论：P0 剩余项主要是 workflow failure strategy、RAG query/document upload 幂等、Review Gate 状态模型。建议优先做可查询失败和 retry 策略，但 Review Gate 状态模型也是明确 P0-4 缺口。

本轮处理：落地 P0-4 状态模型；workflow failure strategy 记录为下一候选切片。

### Test Strategy

结论：建议为 RAG query replay、document upload idempotency、Review Gate 状态模型分别设计回归测试。Review Gate 需要覆盖 `REJECTED`、结构化字段、`PUBLISHED` 才可 release。

本轮处理：新增/更新 `ResourceReviewControllerTest`、`ReviewGovernanceServiceTest`、`SchemaConvergenceMigrationTest`，并完成聚焦、回归、全量测试。

### Security / Permission

结论：Review Gate 接口仍缺教师/管理员授权；`DevAuthFilter` 仍可通过 `X-User-Id` 伪造身份；学习画像、路径和 analytics 也存在后续权限加固空间。

本轮处理：不把权限模型混入状态模型切片；记录为 P3-4 后续任务。

### RAG / Agent

结论：RAG query replay、document upload idempotency、citation governance 仍未完成；资源生成 citation 还没有结构化落 `source_citation`。

本轮处理：只保存 Review Gate 的 `citationCheck` 结构化审核字段，不改 RAG 检索和 citation 生产链路。

### Backend Implementation

结论：一个低风险候选是 workflow runtime failure evidence；另一个明确缺口是 Review Gate 状态模型。本轮如做 Review Gate，应保持文件范围集中并新增 migration/test。

本轮处理：完成 Review Gate 状态、字段、migration 和测试。

## 集成决策

- 当前切片完成 P0-4 的状态模型和结构化审核字段。
- 下一个 P0 候选 1：`P0-1 Orchestrator Workflow 运行期失败证据持久化`。
- 下一个 P0 候选 2：`P0-3 RAG_QA query replay / response snapshot`。
- 安全加固候选：`P3-4 Review Gate 教师/管理员授权`。
