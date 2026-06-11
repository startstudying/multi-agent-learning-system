# RAG 查询重放与响应快照规格

## 1. 概述

本规格补齐 P0-3 中 `RAG_QA` 的 query replay / response snapshot。实现采用现有答题提交幂等模式：规范化 payload 计算 SHA-256 hash，首次成功写入响应快照，重复相同请求直接返回快照，不重复写业务证据。

## 2. 服务设计

### 2.1 RAG 查询幂等入口

新增服务入口：

```java
queryWithTraceIdAndRequestId(userId, kbIds, question, topK, traceId, requestId)
replayQueryIfPresent(userId, kbIds, question, topK, requestId)
```

规则：

- `requestId` 必须非空且长度不超过 120。
- `requestHash` 基于 `userId`、规范化 `kbIds`、trim 后 `question`、解析后的 `topK` 生成。
- 找到同一 `userId + requestId`：
  - hash 相同且 `responseJson` 存在：反序列化并返回首次响应。
  - hash 不同：抛出 `ApiException(CONFLICT)`。
  - hash 相同但 `responseJson` 缺失：抛出 `ApiException(CONFLICT)`，表示请求仍在处理或快照不完整。
- 未找到记录：执行原 RAG 流程，保存 query log、source citation 和 response snapshot。

### 2.2 Orchestrator 行为

`RAG_QA` 必须读取顶层 `CreateWorkflowRequest.requestId`：

- 在创建新 `agent_task` 前调用 `replayQueryIfPresent(...)`。
- 如果命中，按 `ownerUserId + workflowType + requestId + payload` 精确查找首次 workflow task 并返回。
- 如果冲突，直接返回 409，不创建 workflow task。
- 如果未命中，创建 workflow task 并调用幂等 RAG 服务入口。

### 2.3 Response Snapshot

`response_json` 存储完整 `RagQueryResponse`：

```json
{
  "answer": "...",
  "sources": [],
  "traceId": "trc_first",
  "retrieval": {
    "strategy": "COURSE_RAG",
    "queryComplexity": "COMPLEX",
    "noSource": false,
    "retrievalCount": 1,
    "candidateCount": 1,
    "citationCount": 1,
    "downgraded": false,
    "message": "..."
  }
}
```

重放响应必须保留首次 `traceId`，不能使用第二次请求头中的 traceId。

## 3. 数据库迁移

新增 `V7__rag_query_replay_snapshot.sql`：

- 给 `kb_query_log` 增加 `request_id`
- 给 `kb_query_log` 增加 `request_hash`
- 给 `kb_query_log` 增加 `response_json`
- 增加唯一索引 `uk_kb_query_user_request`

## 4. API 行为

### 4.1 `/api/rag/query`

请求体新增可选字段：

```json
{
  "kbIds": ["kb_sql"],
  "question": "Why does SQL JOIN duplicate rows?",
  "topK": 5,
  "requestId": "req_rag_once"
}
```

提供 `requestId` 时启用 replay；不提供时沿用现有行为。

### 4.2 `/api/orchestrator/workflows`

`workflowType=RAG_QA` 时 `requestId` 必填。重复相同请求返回首次 workflow envelope；不同 payload 返回 `409 CONFLICT`。

## 5. 测试策略

- 迁移文本测试：确认 V7 字段和唯一索引。
- `RagQueryServiceTest`：首次快照、成功重放、payload 冲突、无来源重放。
- `OrchestratorWorkflowControllerTest`：workflow 重放不新增业务行、payload 冲突不创建 task、精确 envelope 匹配。

## 6. 架构漂移检查

- Controller 只做请求/响应转发。
- RAG 权限过滤仍在服务层执行。
- Agent task/trace 仍由 Orchestrator 和 recorder 负责。
- 不新增依赖。
- 不改变失败证据脱敏规则。
