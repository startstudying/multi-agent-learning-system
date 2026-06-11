# Subagent Run: Orchestrator 节点契约与策略显式化

## 1. 决策

Use Subagents: Yes

Parallelism Level: L1 Parallel Analysis

Implementation Mode: Single Codex

## 2. 子代理分工

| Agent | 任务 | 写权限 |
|---|---|---|
| Architect | 分析 workflow/node 边界、契约表达方式、是否需要 schema | 无 |
| Security & Quality | 分析失败/重试策略、脱敏、权限和幂等风险 | 无 |
| Test Engineer | 设计 RED/GREEN 测试断言 | 无 |

## 3. 已收到结论

Test Engineer 建议在现有 `steps[]` / `recentFailedStep` 上增加 `inputDto`、`outputDto`、`failurePolicy`、`retryPolicy` 字段，保持最小变更。

Architect 结论：当前完整可执行 workflow 为 `RESOURCE_GENERATION`、`RAG_QA`、`ANSWER_SUBMISSION`；`LEARNING_GOAL_CREATION` 仅为枚举残留，不应在本切片伪造完整契约。最小落地不需要新增 DB。

Security & Quality 结论：失败 workflow 的 `nextActions` 不能对 `RAG_QA` / `ANSWER_SUBMISSION` 暴露不可用的 `RETRY_WORKFLOW`，应返回 `RESUBMIT_ORIGINAL_REQUEST`；资源生成模型失败 summary 仍可能包含 provider message，记录为后续安全加固风险。

## 4. 集成决策

- 采用 `steps[]` / `recentFailedStep` 增量字段方案。
- 不新增 DB，不新增 endpoint。
- `step_runtime_failure` 的 `inputDto` 映射到对应业务 DTO，而不是入口 `CreateWorkflowRequest`。
- `FAILED RESOURCE_GENERATION` 保持 `RETRY_WORKFLOW`。
- `FAILED RAG_QA` / `FAILED ANSWER_SUBMISSION` 改为 `RESUBMIT_ORIGINAL_REQUEST`。
