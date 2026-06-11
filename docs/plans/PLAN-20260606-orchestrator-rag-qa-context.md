# Orchestrator RAG_QA 上下文收敛计划

## Skill Selection Report

## Task Type

后端 Orchestrator + RAG workflow context 收敛，属于 P0-1。

## Selected Skills

| Skill | Why Needed |
|---|---|
| `ai-learning-agent-development` | 保持 AI 学习系统后端闭环方向 |
| `ai-learning-architecture` | 设计 RAG_QA workflow、状态和 trace 边界 |
| `spring-ai-agent-backend` | 保持 Spring Boot Service/Controller/DTO 分层 |
| `agent-trace-governance` | 确保 `agent_task/agent_trace/query_log/citation` 可审计 |
| `educational-rag-pipeline` | RAG query、引用和 no-source 规则 |
| `test-driven-development` | 先写失败测试固定 workflow context 语义 |
| `verification-before-completion` | 完成前跑聚焦和全量测试 |

## Missing Skills

无。

## GitHub Research Needed

No。本轮不新增框架或依赖，按当前代码已有 Orchestrator/RAG 结构实现。

## Confidence Check

| Check | Result |
|---|---|
| No duplicate implementation | PASS：`RAG_QA` 枚举存在，但 Orchestrator 未调用 RAG 查询。 |
| Architecture compliance | PASS：Orchestrator 负责 Agent Trace；RAG 服务只接受外部 traceId。 |
| Official docs verified | N/A：不使用新外部 API。 |
| OSS reference | PASS：沿用用户提供的 Agentic RAG 方向，不复制外部代码。 |
| Root cause | PASS：RAG 直接接口有 trace/log/citation，但未绑定 Orchestrator `agentTaskId`。 |

Confidence：0.91，可以进入实现。

## Subagent Decision

Use Subagents: Yes。

Reason: 用户要求多 subagent 并行开发；Franklin 已完成 P0-1 Orchestrator 子流程接入审查，并建议优先接入 `RAG_QA`。

Parallelism Level: L1。

Selected Subagents:

- Franklin：P0-1 Orchestrator Workflow 子流程接入审查。
- Cicero：P0-3/P0-4 安全与幂等治理审查。

Implementation Mode: Single Codex implementation with parallel analysis。

## 执行步骤

1. 创建本轮 PRD/REQ/SPEC/PLAN/TASK/Context。
2. 扩展 `RagQueryServiceTest`，先写失败测试覆盖 `queryWithTraceId(...)`。
3. 扩展 `OrchestratorWorkflowControllerTest`，先写失败测试覆盖 `RAG_QA` create/get/no-source/invalid payload。
4. 实现 `RagQueryService.queryWithTraceId(...)`。
5. 实现 `OrchestratorWorkflowService` 的 `RAG_QA` payload 解析、调用和 trace steps。
6. 运行聚焦测试和全量后端测试。
7. 更新 Evidence、Acceptance、Memory、Changelog、总 TODO、Retrospective。

## 风险

- 当前 workflow 查询仍依赖 `agent_task.inputJson` 包含 `workflowId`，本轮不改独立 workflow 表。
- 运行期权限/安全失败的 durable failed evidence 需要更完整 no-rollback 策略，后续单独做。
- Orchestrator 响应当前不返回 RAG answer，只返回 workflow 状态；RAG 结果通过 query log/citation 关联审计。

## 完成摘要

- 已实现 `RagQueryService.queryWithTraceId(...)`，RAG query log 和 citation 可复用 Orchestrator traceId。
- 已实现 Orchestrator `RAG_QA` 分支，payload 校验发生在 `agent_task` 创建前。
- 已追加 `workflow_start`、`step_rag_safety`、`step_rag_retrieval`、`step_rag_answer` 四步 trace。
- 已验证 no-source 是成功降级，workflow 状态为 `DONE`，且不写 `source_citation`。
- 聚焦测试通过：`mvn "-Dtest=OrchestratorWorkflowControllerTest,RagQueryServiceTest" test`，16 tests，0 failures。
- 全量测试通过：`mvn test`，102 tests，0 failures。
