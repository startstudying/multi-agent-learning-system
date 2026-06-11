# RAG 查询重放与响应快照需求

## 1. 追踪

- PRD：`docs/product/PRD-20260606-rag-query-replay-snapshot.md`
- 需求编号：REQ-20260606-rag-query-replay-snapshot

## 2. 功能需求

| 编号 | 需求 | 优先级 | 验收方式 |
|---|---|---|---|
| FR-01 | RAG 查询服务必须支持带 `requestId` 的幂等查询入口。 | 必须 | 服务层测试 |
| FR-02 | 首次成功查询必须在 `kb_query_log` 保存 `request_id`、`request_hash`、`response_json`。 | 必须 | Repository 字段断言 |
| FR-03 | 同一 `userId + requestId + requestHash` 必须重放首次 `RagQueryResponse`。 | 必须 | 表计数不变、traceId 等于首次 traceId |
| FR-04 | 同一 `userId + requestId` 但 `requestHash` 不同必须返回 `CONFLICT`。 | 必须 | 服务层和 MockMvc 409 断言 |
| FR-05 | Orchestrator `RAG_QA` 必须在创建 workflow task 前检查可重放记录。 | 必须 | 第二次请求不新增 `agent_task` |
| FR-06 | Orchestrator `RAG_QA` 重放必须按 envelope 精确匹配，不得只按 `requestId` 字符串命中。 | 必须 | 错误候选 task 测试 |
| FR-07 | 无来源 `NO_SOURCE_REFUSAL` 响应也必须可快照重放。 | 必须 | 无 citation 计数断言 |
| FR-08 | 未授权 KB 和内容安全失败不得写入 query replay 快照。 | 必须 | 既有 forbidden 测试保持通过 |

## 3. 数据需求

`kb_query_log` 需要新增：

- `request_id varchar(120)`
- `request_hash varchar(128)`
- `response_json text`
- 唯一索引 `uk_kb_query_user_request(user_id, request_id)`

## 4. 安全需求

- replay 查询范围必须限定在当前 `userId` 下。
- payload 冲突只返回冲突原因，不返回旧问题全文或旧答案。
- 未授权查询不能创建 replay 快照。
- response snapshot 保存的是用户本次已授权查询的结果，用于同用户重放。

## 5. 兼容需求

- 未提供 `requestId` 的直接 RAG 查询继续按现有非幂等路径执行。
- 现有 `/api/rag/query` 请求体增加可选 `requestId`，不破坏旧客户端。
- SSE chat 流继续使用非幂等查询路径。
