# Orchestrator Resource Generation 上下文统一计划

## Skill Selection Report

## Task Type

后端架构优化，属于 P0-1 Orchestrator Workflow 上下文统一。

## Selected Skills

| Skill | Why Needed |
|---|---|
| `ai-learning-agent-development` | 保持 AI 学习系统后端模块边界 |
| `ai-learning-architecture` | 设计 Orchestrator 与子流程状态/trace 关系 |
| `spring-ai-agent-backend` | 修改 Spring Boot Agent 服务和 DTO/API 行为 |
| `agent-trace-governance` | 统一 `agent_task`、`agent_trace`、`traceId` |
| `test-driven-development` | 先写失败测试锁定上下文统一 |
| `verification-before-completion` | 完成前跑聚焦和全量验证 |

## Missing Skills

无。

## GitHub Research Needed

No。用户已提供 Spring AI / Spring AI Alibaba / 多 Agent 教育系统参考方向；本轮不新增依赖，不复制外部代码，只按现有后端架构收敛。

## Confidence Check

| Check | Result |
|---|---|
| No duplicate implementation | PASS：当前没有直接把 `RESOURCE_GENERATION` 接入 Orchestrator 的实现。 |
| Architecture compliance | PASS：沿用 Spring Boot Service、JPA Repository、`AgentRunRecorder`。 |
| Official docs verified | N/A：不使用新外部 API 或依赖。 |
| OSS reference | PASS：本轮基于用户提供的 Spring AI 多 Agent / Agentic RAG 参考方向，不引入代码。 |
| Root cause | PASS：根因是 Orchestrator 只建 envelope，资源生成另起 `agent_task`。 |

Confidence：0.90，可以进入实现。

## Subagent Decision

Use Subagents: Yes。

Reason: 用户明确要求多 subagent 并行开发；上一轮 Epicurus 已指出 P0-1 风险。本轮再启动一个只读 code-reviewer 并行审查方案，主线程执行 TDD 和实现。

Parallelism Level: L1 并行审查。

Implementation Mode: Single Codex implementation，避免多个 agent 修改 Orchestrator / ResourceGenerationService 造成冲突。

## 执行步骤

1. 创建本轮 PRD/REQ/SPEC/PLAN/TASK/Context。
2. 修改 `OrchestratorWorkflowControllerTest`，写失败测试：
   - Orchestrator 创建 `RESOURCE_GENERATION` 后复用同一 `agentTaskId/traceId`。
   - trace steps 包含 `workflow_start + resource generation`。
   - 直接资源生成接口仍独立。
3. 运行聚焦测试确认 RED。
4. 修改 `AgentRunRecorder.recordTraceSteps(...)` 支持追加序号。
5. 修改 `ResourceGenerationService` 增加 workflow-aware 内部方法。
6. 修改 `OrchestratorWorkflowService` 在 `RESOURCE_GENERATION` 时调用资源生成服务。
7. 运行聚焦测试和全量后端测试。
8. 更新 TODO、Evidence、Acceptance、Memory、Changelog。

## 风险

- 如果资源生成模型失败，Orchestrator 外层事务必须保留失败 trace；需要沿用 `noRollbackFor`。
- Orchestrator 幂等仍未完全解决；本轮不处理重复 `requestId` 创建 workflow 的恢复语义。
- 目前仍通过 `agent_task.inputJson` marker 查询 workflow，后续大规模查询应考虑独立 workflow 表。
