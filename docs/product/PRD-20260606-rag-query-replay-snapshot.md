# RAG 查询重放与响应快照 PRD

## 1. 背景

当前 `RAG_QA` 已接入 Orchestrator，能够复用同一个 `workflowId / agentTaskId / traceId` 写入 query log 和 citation。但同一个学习者重复提交相同 `requestId` 的 RAG 查询时，系统仍会重新创建 workflow、重新写入 `kb_query_log` 和 `source_citation`，不满足 P0-3 “重复 RAG query 不产生重复业务结果”的验收目标。

答题提交已经具备 `requestId + requestHash + responseJson` 的幂等重放能力。本切片将同样的工程模式落到 RAG 查询链路上，使课程资料问答具备可追踪、可重放、可冲突检测的后端闭环。

## 2. 用户故事

- 作为学生，我重复点击同一次课程资料问答时，希望系统返回第一次结果，而不是重复消耗检索和生成链路。
- 作为教师或管理员，我希望 RAG 查询记录和引用证据只保留一次业务结果，避免审计数据被重复点击污染。
- 作为开发者，我希望同一个 `requestId` 被不同 payload 复用时返回明确的 `409 CONFLICT`，而不是生成新的答案。

## 3. 目标

- `RAG_QA` 支持 `requestId` 幂等重放。
- `kb_query_log` 保存 `requestId`、`requestHash` 和完整 `RagQueryResponse` 快照。
- 同一用户、同一 `requestId`、同一规范化 payload 重放第一次响应，不新增 query log、citation 或 workflow task。
- 同一用户、同一 `requestId`、不同 payload 返回 `409 CONFLICT`，且不新增业务记录。
- 无来源回答也必须可快照和重放，不伪造 citation。

## 4. 非目标

- 不实现文档上传幂等。
- 不实现索引任务数据库级锁或后台恢复任务。
- 不实现 hybrid retrieval、RRF、reranker fallback 或真实 VectorDB。
- 不改前端页面。
- 不新增外部依赖。

## 5. 验收指标

| 指标 | 目标 |
|---|---|
| 相同 RAG_QA requestId 重放 | 返回第一次 workflow 和 trace |
| 重复写入控制 | `agent_task`、`kb_query_log`、`source_citation` 不增加 |
| payload 冲突 | HTTP 409，响应 code 为 `CONFLICT` |
| 快照完整性 | `responseJson` 可恢复 answer、sources、traceId、retrieval |
| 权限安全 | 未授权 KB 仍不写 query log / citation |
