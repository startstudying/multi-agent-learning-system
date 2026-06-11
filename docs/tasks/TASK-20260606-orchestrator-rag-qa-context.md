# Orchestrator RAG_QA 上下文收敛任务

## 任务清单

- [x] 读取项目记忆、Orchestrator/RAG 代码和 subagent 报告
- [x] 创建本轮 workflow 文档
- [x] 编写失败测试覆盖 RAG service 外部 traceId 和 Orchestrator RAG_QA
- [x] 实现 `RagQueryService.queryWithTraceId(...)`
- [x] 实现 Orchestrator `RAG_QA` 分支和 trace steps
- [x] 运行聚焦测试
- [x] 运行全量后端测试
- [x] 更新 Evidence、Acceptance、Memory、Changelog、总 TODO、Retrospective

## Done Criteria

- [x] `RAG_QA` workflow 成功返回 `DONE`
- [x] `kb_query_log.traceId` 等于 Orchestrator traceId
- [x] 有来源时 `source_citation.traceId` 等于 Orchestrator traceId
- [x] no-source workflow 仍为 `DONE` 且不写 citation
- [x] GET workflow 返回相同 `agentTaskId / traceId / steps`
- [x] 无效 payload 不创建 `agent_task`
- [x] 直接 RAG 查询行为不回退
- [x] `mvn test` 通过

## 完成记录

- RED：`mvn "-Dtest=OrchestratorWorkflowControllerTest,RagQueryServiceTest" test` 首次失败，原因是 `queryWithTraceId(...)` 缺失。
- GREEN：聚焦测试通过，16 tests，0 failures，0 errors。
- FULL：`mvn test` 通过，102 tests，0 failures，0 errors。
- Subagent：`Kuhn` 完成只读架构评审，报告归档到 `docs/subagents/runs/RUN-20260606-orchestrator-rag-qa-context-review.md`。

## 后续未纳入

- `ANSWER_SUBMISSION` workflow context
- workflow retry/recovery
- 运行期权限/安全失败的 durable failed workflow evidence
