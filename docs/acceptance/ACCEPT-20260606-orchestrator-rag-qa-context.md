# Orchestrator RAG_QA 上下文收敛验收

## 验收结论

通过。

本轮完成 `RAG_QA` 的 Orchestrator 最小闭环，workflow、trace、RAG query log 和 citation 统一到同一个 `traceId`，并且无效 payload 在创建 task 之前就被拒绝。

## 验收项

| 验收项 | 结果 | 证据 |
|---|---|---|
| `workflowType=RAG_QA` 可执行真实 RAG 查询 | PASS | `OrchestratorWorkflowControllerTest.createsRagQaWorkflowAndReusesWorkflowTraceContext` |
| 复用统一 `workflowId / agentTaskId / traceId` | PASS | 同上 |
| `kb_query_log.traceId` 与 Orchestrator traceId 一致 | PASS | `RagQueryServiceTest.queryWithTraceIdPersistsProvidedTraceId` |
| `source_citation.traceId` 与 Orchestrator traceId 一致 | PASS | 同上 |
| 无来源时仍返回 `DONE` 且不写 citation | PASS | `OrchestratorWorkflowControllerTest.createsRagQaWorkflowWithNoSourceAndNoCitations` |
| GET workflow 返回相同上下文和步骤 | PASS | `OrchestratorWorkflowControllerTest.returnsWorkflowStatusContextAfterRagQa` |
| 无效 payload 不创建 `agent_task` | PASS | `OrchestratorWorkflowControllerTest.rejectsInvalidRagQaPayloadBeforeCreatingWorkflowTask` |
| `mvn test` 全量通过 | PASS | 102 tests, 0 failures, 0 errors |

## 约束确认

- 未新增数据库迁移
- 未修改现有 `/api/rag/query` 或 `/api/tutor/ask` 合同
- 未把 trace 控制权下放到 RAG service
- 未扩展到 `ANSWER_SUBMISSION`

## 验收备注

本轮只完成 P0-1 的 `RAG_QA` 切片。运行期权限/安全失败的 durable rollback / recovery 语义仍属于后续工作，不影响本轮验收通过。
