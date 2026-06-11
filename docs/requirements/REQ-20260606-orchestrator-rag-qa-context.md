# Orchestrator RAG_QA 上下文收敛需求

## 功能需求

| ID | Requirement |
|---|---|
| FR-01 | `POST /api/orchestrator/workflows` 必须支持 `workflowType=RAG_QA` 执行真实 RAG 查询。 |
| FR-02 | `RAG_QA` payload 必须包含非空 `kbIds` 数组和非空 `question`。 |
| FR-03 | `RAG_QA` 可选支持 `topK`，缺失或非法值沿用 RAG 服务默认值。 |
| FR-04 | Orchestrator 必须为 `RAG_QA` 创建统一 `workflowId`、`agentTaskId`、`traceId`。 |
| FR-05 | RAG 查询必须复用 Orchestrator 的 `traceId`，不能自行生成新 trace。 |
| FR-06 | `kb_query_log.traceId` 必须等于 Orchestrator traceId。 |
| FR-07 | 有 citation 时，`source_citation.traceId` 必须等于 Orchestrator traceId。 |
| FR-08 | Orchestrator 必须在同一 `agent_task` 下追加 RAG trace steps。 |
| FR-09 | 成功 RAG workflow 最终状态为 `DONE`，`nextActions` 包含 `VIEW_RESULT`。 |
| FR-10 | 无效 payload 必须返回 `VALIDATION_ERROR` 且不创建 `agent_task`。 |

## 非功能需求

| ID | Requirement |
|---|---|
| NFR-01 | 不破坏直接 RAG 查询和 Tutor 查询的现有 API 行为。 |
| NFR-02 | RAG 服务不依赖 `AgentRunRecorder`，Agent Trace 仍由 Orchestrator 负责。 |
| NFR-03 | 不新增数据库迁移或依赖。 |
| NFR-04 | 聚焦测试和全量后端测试通过。 |

## 验收要求

- Orchestrator 集成测试覆盖有来源、无来源、payload 校验和 GET workflow 查询。
- RAG service 测试覆盖外部 traceId 传入后 query log / citation 复用该 traceId。
- 直接 `query(...)` 仍能生成或读取 traceId。
