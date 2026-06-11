# SPEC - P3-2 RAG Hybrid Retrieval / RRF / Reranker Fallback

## 1. 概述

本规格定义 P3-2 的最小 RAG 在线检索生产化切片：在不新增依赖、不改变 DB schema、不接真实 VectorDB/reranker provider 的前提下，实现 allowed KB 内 keyword + recency hybrid-ready retrieval、RRF fusion、状态化 reranker fallback、以及安全 query log metadata。

## 2. 追踪

- PRD：`docs/product/PRD-20260608-rag-hybrid-retrieval.md`
- REQ：`docs/requirements/REQ-20260608-rag-hybrid-retrieval.md`
- TODO：`docs/planning/backend-architecture-todolist.md` P3-2 `hybrid retrieval、RRF、reranker timeout fallback`

## 3. 领域模型

新增 application-layer 内部类型，不暴露到 API：

```java
record RetrievalResult(
    List<KbDocChunk> chunks,
    String retrievalMode,
    int keywordCandidateCount,
    int recencyCandidateCount,
    int vectorCandidateCount,
    int fusedCandidateCount,
    int rrfK
) {}

record RerankResult(
    List<KbDocChunk> chunks,
    RerankerStatus status,
    boolean fallbackUsed,
    Long latencyMs,
    String errorCode
) {}

enum RerankerStatus {
    NOT_CONFIGURED,
    SKIPPED_NO_CANDIDATES,
    SUCCEEDED,
    TIMEOUT_FALLBACK,
    ERROR_FALLBACK
}
```

## 4. API 契约

不修改 API。

```http
POST /api/rag/query
GET /api/rag/query
POST /api/orchestrator/workflows  # workflowType=RAG_QA
```

请求与响应字段沿用既有合同。

### 错误码

| 错误码 | 说明 | 触发条件 |
|---|---|---|
| `FORBIDDEN` | KB 不可读 | requested KB 中存在 forbidden KB |
| `VALIDATION_ERROR` | 请求非法 | question/requestId 等校验失败 |
| `OK` | reranker fallback 后仍成功 | timeout/error 使用 fused candidates |

## 5. 前端交互

不修改 frontend。

## 6. 后端流程

```text
RagQueryService.query*
-> ContentSafetyService.checkUserInput(question)
-> PermissionService.requireReadableKbIds(userId, requestedKbIds)
-> ChunkService.retrieveAllowedChunks(allowedKbIds, question, topK)
   -> bounded allowed chunk pool
   -> keyword scoring branch
   -> recency fallback branch
   -> vector disabled branch metadata
   -> RRF fusion + dedupe
-> RerankerService.rerankOrFallback(question, fusedCandidates, topK)
   -> no candidates: SKIPPED_NO_CANDIDATES
   -> no provider configured: NOT_CONFIGURED
   -> provider succeeds: SUCCEEDED
   -> timeout: TIMEOUT_FALLBACK + fused candidates
   -> error: ERROR_FALLBACK + fused candidates
-> AdaptiveRagRouter.describe(...)
-> build grounded answer / no-source answer
-> persist kb_query_log
-> persist source_citation when sources non-empty
-> record metrics
```

## 7. Agent 工作流

`RAG_QA` Orchestrator workflow 不新增步骤，仍保持：

```text
workflow_start
-> step_rag_safety
-> step_rag_retrieval
-> step_rag_answer
```

reranker fallback 是 `step_rag_retrieval` 内部 metadata，不改变 workflow step contract。

## 8. RAG 工作流

### Retrieval

- 所有 retrieval branch 必须只接收 `allowedKbIds`。
- `keywordCandidateCount` 只统计 keyword score > 0 的候选。
- `recencyCandidateCount` 统计 bounded recency fallback 候选。
- `vectorCandidateCount=0`，`vectorEnabled=false`。
- RRF 使用固定 `rrfK=60`。
- final chunks 数量不超过服务端 `MAX_TOP_K=20` 和请求 `topK`。

### Reranker fallback

- `NOT_CONFIGURED`：默认无真实 reranker provider，不视为错误降级。
- `TIMEOUT_FALLBACK` / `ERROR_FALLBACK`：`retrieval.downgraded=true`，message 只写稳定状态，不写 raw error。
- `SKIPPED_NO_CANDIDATES`：no-source 场景，不调用 reranker。

## 9. 数据库变更

无数据库 schema 变更。

复用字段：

- `kb_query_log.reranker_status`：稳定 reranker 状态枚举。
- `kb_query_log.sources_json`：安全 metadata JSON。

`sourcesJson` 结构示例：

```json
{
  "retrieval": {
    "strategy": "COURSE_RAG",
    "candidateCount": 2,
    "retrievalCount": 2,
    "citationCount": 2,
    "downgraded": true
  },
  "hybrid": {
    "retrievalMode": "HYBRID_RRF",
    "rrfK": 60,
    "keywordCandidateCount": 1,
    "recencyCandidateCount": 2,
    "vectorEnabled": false,
    "vectorCandidateCount": 0,
    "fusedCandidateCount": 2
  },
  "reranker": {
    "status": "TIMEOUT_FALLBACK",
    "fallbackUsed": true,
    "latencyMs": 5,
    "errorCode": "RERANKER_TIMEOUT"
  },
  "sources": [
    {
      "documentId": "doc_sql",
      "documentName": "database-course.md",
      "pageNum": 12,
      "sectionTitle": "Multi table joins",
      "score": 1.0
    }
  ]
}
```

禁止在新增 metadata 中写入 raw prompt、raw reranker request、raw provider response、raw provider error、candidate full text、secret。

## 10. 状态流转

```text
NO_CANDIDATES -> SKIPPED_NO_CANDIDATES
FUSED_CANDIDATES + no provider -> NOT_CONFIGURED
FUSED_CANDIDATES + provider success -> SUCCEEDED
FUSED_CANDIDATES + provider timeout -> TIMEOUT_FALLBACK
FUSED_CANDIDATES + provider error -> ERROR_FALLBACK
```

## 11. 错误处理

- `FORBIDDEN` / validation / content safety 失败不写 query/citation 成功 artifact。
- reranker timeout/error 不抛到 HTTP 层，不写 raw exception。
- RRF 或 serialization 内部错误仍按现有 runtime failure 处理。

## 12. 权限规则

- permission filter 必须先于 retrieval。
- mixed allowed/forbidden KB 必须整体拒绝。
- 不允许 vector/hybrid/reranker 输入中出现 forbidden candidates。
- Prompt 或 reranker provider 不参与权限判断。

## 13. Trace / 日志

- `traceId` 继续写入 `kb_query_log` 和 `source_citation`。
- `metrics` tag 不新增高基数字段。
- `sourcesJson` 仅写白名单 metadata。
- 既有 `question` / `responseJson` replay snapshot 合同不在本切片改变；后续可单独做 query log retention / 脱敏治理。

## 14. 测试策略

1. RED first：
   - `RrfRankerTest`：排序与去重。
   - `RagQueryServiceTest`：keyword 相关性、timeout/error fallback、no-source、权限、脱敏。
2. GREEN：
   - 实现最小内部类型和算法。
3. 回归：
   - `RagQueryServiceTest`
   - `OrchestratorWorkflowControllerTest`
   - `IndexServiceTest`
   - `mvn test`

## 15. 验收清单

- [x] FR-01 权限过滤先于 retrieval。
- [x] FR-02 keyword 相关性排序生效。
- [x] FR-03 recency fallback 保留。
- [x] FR-04 RRF 排序去重生效。
- [x] FR-05 vector disabled metadata 明确。
- [x] FR-06 reranker 状态化结果生效。
- [x] FR-07 timeout/error fallback 不导致 query 失败。
- [x] FR-08 no-source 不伪造 citation。
- [x] FR-09 `rerankerStatus` 写稳定枚举。
- [x] FR-10 `sourcesJson` 新增 metadata 不含 raw prompt/provider error/full chunk。
- [x] 不新增依赖、schema、frontend 改动。

## 16. 实施状态

- 状态：已完成。
- 完成日期：2026-06-08。
- 验收证据：`docs/evidence/EVIDENCE-20260608-rag-hybrid-retrieval.md`、`docs/acceptance/ACCEPT-20260608-rag-hybrid-retrieval.md`。
- 后续边界：真实 embedding service、VectorDB adapter、外部 reranker provider 和 query log retention / 脱敏治理仍需单独 PRD/REQ/SPEC/PLAN/TASK。
