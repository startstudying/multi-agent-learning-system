# SPEC - P3-2 子任务：Vector embedding payload contract

日期：2026-06-10

## 范围

本规格只定义 RAG application 内部 embedding vector payload contract。

## 内部模型

### `EmbeddingVector`

新增内部 record：

```java
EmbeddingVector(String ownerId, float[] values)
```

规则：

- `ownerId` 对 document chunks 使用 `chunkId`。
- query vector 可使用固定 owner id，例如 `query`。
- `values` 仅在内存中传递，不持久化。
- accessor 应返回 defensive copy。
- `toString()` 只输出 ownerId 和 dimension，不输出 raw float values。

### `EmbeddingBatchResult`

扩展字段：

```java
List<EmbeddingVector> vectors
```

规则：

- `DISABLED` / `PROVIDER_ERROR` / `DIMENSION_MISMATCH` 时 vectors 为空。
- `SUCCEEDED` 时 vectors 数量必须等于 `chunkCount`。
- 每个 vector 的 ownerId 必须等于对应 `ChunkEmbeddingInput.chunkId`。

### `QueryEmbeddingResult`

新增 query embedding 结果：

```java
QueryEmbeddingResult(
  EmbeddingStatus status,
  String modelVersion,
  EmbeddingVector vector,
  long latencyMs,
  String errorCode
)
```

规则：

- query embedding 失败不得抛出到 RAG query 主流程。
- 失败时 `ChunkService` 降级到 keyword/recency/RRF。

## Vector contract

### `VectorChunkReference`

扩展为：

```java
VectorChunkReference(String chunkId, String chunkHash, Integer chunkIndex, EmbeddingVector vector)
```

兼容旧构造可保留：

```java
VectorChunkReference(String chunkId, String chunkHash, Integer chunkIndex)
```

规则：

- 不包含 raw chunk content。
- `toString()` 不泄露 raw vector values。

### `VectorSearchRequest`

改为：

```java
VectorSearchRequest(List<String> allowedKbIds, int topK, EmbeddingVector queryVector)
```

规则：

- 不携带 raw question。
- `allowedKbIds` 必须继续下推给 adapter。
- adapter 仍只能返回 `VectorSearchHit(chunkId, score)`。

## 流程

### Indexing

```text
parsed sections
-> chunks
-> EmbeddingService.embedDocumentChunks(...)
-> EmbeddingBatchResult.vectors
-> VectorUpsertRequest.chunks[].vector
-> VectorIndexAdapter.upsert(...)
-> chunk metadata only stores short status
```

### Query

```text
allowedKbIds + question
-> keyword/recency candidates
-> EmbeddingService.embedQuery(question)
-> VectorSearchRequest(allowedKbIds, topK, queryVector)
-> VectorIndexAdapter.search(...)
-> service reloads chunks by id
-> service filters by allowedKbIds again
-> RRF
```

## 安全约束

- raw vector 不写入 `metadataJson`。
- raw vector 不写入 `sourcesJson`。
- raw provider response/error 不写入 task/document/query log。
- `VectorUpsertRequest` 不携带 chunk content。
- `VectorSearchRequest` 不携带 raw question。
- adapter 返回 forbidden chunk id 时，服务层必须继续二次过滤。

## 架构漂移检查

- Backend owns AI API calls：保持。
- VectorDB adapter 仍位于 Service/application boundary：保持。
- RAG retrieval permission filtering：保持下推 + 二次过滤。
- 不引入 Agent Tool -> Repository 直接访问：保持。
- 不新增 dependency/API/DB/frontend：保持。

