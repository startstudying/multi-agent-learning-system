# Evidence - P3-2 子任务：Vector embedding payload contract

日期：2026-06-10

## 变更摘要

本切片完成 RAG embedding vector 内部 payload contract：

- 新增 `EmbeddingVector`，只在内存中承载向量，`toString()` 只显示 owner/dimension。
- 新增 `QueryEmbeddingResult`，用于 query 侧 embedding 结果。
- `EmbeddingBatchResult` 成功时携带与 chunk id 对齐的 vectors。
- `EmbeddingService` 支持 document chunk embeddings 和 query embedding 两条路径。
- `VectorChunkReference` 可携带 chunk vector，但不输出 raw float values。
- `VectorSearchRequest` 改为携带 query vector，不再把 raw question 交给 adapter。
- `IndexService` 将 embedding vectors 注入 `VectorUpsertRequest`。
- `ChunkService` 在 vector search 前先调用 `EmbeddingService.embedQuery(...)`；query embedding 失败时 fallback 到 keyword/recency/RRF。

未新增 Maven dependency、VectorDB 配置、DB migration、REST API/DTO、frontend、parser/OCR、Agent/Orchestrator 变更。

## RED 证据

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=EmbeddingServiceTest,NoopVectorIndexAdapterTest,IndexServiceVectorPayloadTest,ChunkServiceVectorRetrievalTest test
```

结果：

```text
BUILD FAILURE
COMPILATION ERROR
```

关键失败符合预期：

- `EmbeddingBatchResult.vectors()` 不存在。
- `QueryEmbeddingResult` / `EmbeddingService.embedQuery(...)` 不存在。
- `EmbeddingVector` 不存在。
- `VectorChunkReference.vector()` 不存在。
- `VectorSearchRequest.queryVector()` 不存在。

## Focused GREEN

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=EmbeddingServiceTest,NoopVectorIndexAdapterTest,IndexServiceVectorPayloadTest,ChunkServiceVectorRetrievalTest test
```

结果：

```text
Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

覆盖：

- document chunk embedding result vectors。
- query embedding vector。
- vector request `toString()` 不泄露 raw vector / raw question / chunk content。
- `IndexService` upsert request carries vectors。
- query embedding failure fallback。
- allowed KB vector filtering 保持。

## Adjacent Verification

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=EmbeddingServiceTest,NoopVectorIndexAdapterTest,IndexServiceEmbeddingVectorTest,IndexServiceVectorPayloadTest,IndexServiceTest,ChunkServiceVectorRetrievalTest,RagQueryServiceTest test
```

结果：

```text
Tests run: 49, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Full Backend Verification

命令：

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

```text
Tests run: 586, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

## 安全证据

- `VectorSearchRequest` 不再携带 raw question。
- `VectorUpsertRequest` 不携带 raw chunk content。
- `EmbeddingVector.toString()` 不输出 raw float values。
- chunk `metadataJson` 只记录 `vectorIndexStatus` 等短状态，不写 raw vectors。
- no dependency/API/DB/frontend change。

## 剩余范围

本切片不接入真实 VectorDB provider。后续 real VectorDB adapter 仍需 L 级流程、dependency/security review、官方文档核验和 provider-specific smoke。

