# Acceptance - P3-2 子任务：Vector embedding payload contract

日期：2026-06-10

## Verdict

Accepted.

## Acceptance Criteria

| Criteria | Status | Evidence |
|---|---|---|
| `EmbeddingBatchResult.vectors()` 与 chunk id 对齐 | PASS | `EmbeddingServiceTest.callsRealEmbeddingModelAdapterWhenProviderAndEmbeddingModelAreConfigured` |
| query vector 由 `EmbeddingService` 生成 | PASS | `EmbeddingServiceTest.createsQueryEmbeddingVectorWithoutLeakingRawVectorInStringRepresentation` |
| `VectorUpsertRequest` 包含 vector payload | PASS | `IndexServiceVectorPayloadTest.vectorUpsertReceivesEmbeddingVectorsWithoutRawChunkContent` |
| `VectorSearchRequest` 不包含 raw question | PASS | `ChunkServiceVectorRetrievalTest.usesQueryEmbeddingVectorForVectorSearchRequestWithoutPassingRawQuestion` |
| vector payload `toString()` 不泄露 raw float values | PASS | `EmbeddingServiceTest` / `NoopVectorIndexAdapterTest` |
| query embedding failure 不导致 RAG query 失败 | PASS | `ChunkServiceVectorRetrievalTest.fallsBackWithoutCallingVectorAdapterWhenQueryEmbeddingFails` |
| 默认 noop/disabled 行为保持 | PASS | `NoopVectorIndexAdapterTest` 与 full backend |
| full backend tests 通过 | PASS | `mvn test`: `586 run, 0 failures, 0 errors, 1 skipped` |

## Accepted Limitation

本切片只完成内部 vector payload contract。P3-2 real VectorDB parent item 保持 open；真实 Qdrant/Redis/Milvus/Weaviate/pgvector adapter、dependency review、security review、runtime config、external smoke 仍是后续 L 级任务。

