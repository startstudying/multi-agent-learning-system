# RAG_QA Orchestrator 上下文收敛评审

## Summary

建议实现 `RAG_QA` 接入 Orchestrator。当前代码已经具备继续推进的基础：`RAG_QA` 枚举已存在，RAG service 已有 query log / citation / permission filter 基础，Orchestrator 只需要成为 workflow 上下文拥有者。

## 必须满足的行为

1. `POST /api/orchestrator/workflows` 必须支持 `workflowType=RAG_QA`，并执行真实 RAG 查询。
2. payload 校验必须发生在 `agent_task` 创建之前，避免留下半成品 workflow。
3. RAG 查询必须复用 Orchestrator 的 `traceId`，`kb_query_log.traceId`、`source_citation.traceId` 和 workflow response `traceId` 要一致。
4. Orchestrator 必须在同一个 `agent_task` 下追加 `workflow_start`、`step_rag_safety`、`step_rag_retrieval`、`step_rag_answer`。
5. RAG 的权限过滤必须继续留在后端服务内，不能信任前端传入的 `kbIds`。

## 测试建议

- `RagQueryServiceTest` 增加外部 traceId 断言。
- `OrchestratorWorkflowControllerTest` 增加有来源、无来源、无效 payload、GET workflow 四类测试。
- 覆盖非法 `kbIds`、空 question、越权 KB、unsafe question。

## 潜在风险

- 运行期权限/安全失败的 durable failed workflow evidence 仍未完成。
- workflow 查询仍依赖 `agent_task.inputJson` 中的 `workflowId` 标记，长期可维护性一般。
- `RAG_QA` 只做最小切片，不等于 query replay / idempotency 完成。

## 是否建议实现

建议实现。

这次切片的边界清晰，风险可控，而且不会影响现有 RAG/Tutor API 合同。推荐继续保持“Orchestrator 负责 workflow trace，RAG service 负责权限过滤和 citation”的分层。
