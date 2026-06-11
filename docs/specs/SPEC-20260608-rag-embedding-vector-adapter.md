# SPEC - P3-2-A RAG Embedding Service 与可选 VectorDB Adapter 边界

## 1. 概述

本规格定义 RAG embedding/vector 最小边界切片。该切片只建立内部 application-layer contract，使离线索引和在线检索可以安全感知 embedding/vector 状态；真实 provider 与真实 VectorDB 仍是后续独立切片。

## 2. 追踪

- PRD：`docs/product/PRD-20260608-rag-embedding-vector-adapter.md`
- REQ：`docs/requirements/REQ-20260608-rag-embedding-vector-adapter.md`
- 集成评审：`docs/subagents/runs/RUN-20260608-rag-embedding-vector-integration-review.md`

## 3. 内部模型

建议/允许的 application-layer 类型：

```java
record ChunkEmbeddingInput(String chunkId, String content) {}

enum EmbeddingStatus {
    DISABLED,
    SUCCEEDED,
    PROVIDER_ERROR,
    DIMENSION_MISMATCH
}

record EmbeddingBatchResult(
    EmbeddingStatus status,
    String modelVersion,
    int chunkCount,
    long latencyMs,
    String errorCode
) {}

interface VectorIndexAdapter {
    boolean isEnabled();
    VectorUpsertResult upsert(VectorUpsertRequest request);
    VectorSearchResult search(VectorSearchRequest request);
}

enum VectorIndexStatus {
    DISABLED,
    SUCCEEDED,
    VECTOR_UPSERT_FAILED,
    PROVIDER_ERROR
}
```

后续如需真实向量对象，可扩展 `EmbeddingVector` / `EmbeddedChunk`，但不得把 raw vector 写入 `metadataJson`、`sourcesJson` 或 trace。

## 4. 后端流程

### 4.1 Offline indexing

```text
IndexService.processIndexTask
-> parse document
-> split/deduplicate chunks
-> delete old chunks
-> embeddingService.embedDocumentChunks(...)
-> vectorIndexAdapter.upsert(...)
-> merge short metadata
-> save chunks
-> mark INDEXED / FAILED
```

状态规则：

- `EmbeddingStatus.DISABLED`：索引成功，metadata 写 disabled。
- `EmbeddingStatus.SUCCEEDED`：索引继续 vector upsert。
- `EmbeddingStatus.PROVIDER_ERROR` / `DIMENSION_MISMATCH`：索引失败或进入 worker retry。
- `VectorIndexStatus.DISABLED`：索引成功，metadata 写 disabled。
- `VectorIndexStatus.SUCCEEDED`：索引成功。
- `VectorIndexStatus.VECTOR_UPSERT_FAILED` / `PROVIDER_ERROR`：索引失败或进入 worker retry。

### 4.2 Online retrieval

```text
RagQueryService
-> ContentSafetyService
-> PermissionService.requireReadableKbIds(...)
-> ChunkService.retrieveAllowedChunks(allowedKbIds, question, topK)
   -> keyword branch
   -> recency branch
   -> optional vector branch
   -> allowed-KB second verification
   -> RRF
-> RerankerService fallback
-> answer/citation/query log
```

vector branch 规则：

- adapter disabled 或 embedding disabled：`VectorSearchResult.DISABLED`。
- adapter enabled：`VectorSearchRequest.allowedKbIds` 必须非空。
- adapter 返回 chunk 后，服务层必须过滤 `chunk.kbId in allowedKbIds`。
- adapter error 不应导致 RAG query 整体失败；query fallback 到 keyword/recency。

## 5. API 合同

不修改公开 API：

- `POST /api/rag/query`
- `GET /api/rag/query`
- `POST /api/orchestrator/workflows` with `workflowType=RAG_QA`

新增内容仅体现在内部 `kb_query_log.sourcesJson`：

```json
{
  "hybrid": {
    "retrievalMode": "HYBRID_RRF",
    "vectorEnabled": false,
    "vectorCandidateCount": 0,
    "fusedCandidateCount": 2
  }
}
```

## 6. DB schema

本切片不新增 schema。

复用：

- `kb_doc_chunk.metadata_json`：保存短状态，如 `embeddingModel`、`embeddingStatus`、`vectorIndexStatus`。
- `kb_query_log.sources_json`：保存 vector enabled/count 等短 metadata。

禁止：

- 保存完整 float vector。
- 保存 chunk 正文副本。
- 保存 provider raw error / response。
- 保存 API key / endpoint credential。

## 7. 配置

沿用：

```yaml
learning-os:
  ai-model:
    provider: ${AI_MODEL_PROVIDER:none}
    embedding-model: ${AI_EMBEDDING_MODEL:}
```

本切片不新增真实 provider key 配置。后续如新增 key，必须通过环境变量或 secret manager，并完成 dependency/security review。

## 8. 错误码

| 错误码 | 用途 |
|---|---|
| `EMBEDDING_PROVIDER_NOT_CONFIGURED` | embedding model 已配置但本切片无真实 provider。 |
| `EMBEDDING_FAILED` | embedding 失败 fallback code。 |
| `VECTOR_UPSERT_FAILED` | vector upsert 失败 fallback code。 |
| `DOCUMENT_INDEX_FAILED` | 非预期索引失败 fallback code。 |

错误码必须固定、低敏，不拼接 provider raw message。

## 9. 测试策略

- Unit：`EmbeddingServiceTest`、`NoopVectorIndexAdapterTest`。
- Index integration：`IndexServiceTest` 覆盖 disabled metadata 与 enabled failure。
- Retrieval：新增/扩展 `ChunkServiceVectorRetrievalTest` 覆盖 vector branch、forbidden candidate filtering、fallback。
- RAG query：扩展 `RagQueryServiceTest` 覆盖 `sourcesJson` vector metadata。
- Regression：`IndexServiceTest`、`IndexTaskWorkerSchedulerTest`、`RagQueryServiceTest`、`OrchestratorWorkflowControllerTest`。

## 10. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 不感知 adapter；RAG service 负责编排。 |
| Frontend | PASS | 无 frontend 改动。 |
| Agent/RAG | PASS | 权限仍先于 retrieval；citations 仍从授权 chunk 生成。 |
| Security | PASS | 不新增依赖，不写 secret，metadata 最小化。 |
| API/DB | PASS | 不改公开 API，不改 schema。 |

## 11. 当前状态

状态：已完成。

实现采用保存新 chunks 前先完成 embedding/vector boundary 的顺序：旧 chunks 会先清理，embedding/vector 失败时不会留下新的 searchable chunks；disabled/noop 时写短 metadata 并正常保存。真实 provider、真实向量对象、真实 VectorDB SDK 和部署治理仍待后续独立规格。
