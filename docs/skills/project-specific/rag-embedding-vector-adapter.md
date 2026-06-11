# RAG Embedding / Vector Adapter Boundary

## 使用场景

当实现或审查 RAG embedding service、VectorDB adapter、离线索引 embedding/vector 阶段、在线 vector retrieval hook、vector metadata 或 provider/adapter 安全边界时使用。

## 核心规则

1. 默认必须是 disabled/noop，不配置真实 provider 时不得调用外部网络服务。
2. `EmbeddingService` 必须返回状态化结果，至少区分 `DISABLED`、`SUCCEEDED`、`PROVIDER_ERROR`、`DIMENSION_MISMATCH`。
3. Document embedding 成功时应通过 `EmbeddingBatchResult.vectors()` 返回与 chunk id 对齐的内存向量；query embedding 应通过 `EmbeddingService.embedQuery(...)` 生成 `EmbeddingVector` 后再进入 vector search。
4. `VectorSearchRequest` 不应携带 raw question；真实 adapter 只接收 query vector、`allowedKbIds` 和 `topK`。
5. `EmbeddingVector`、`VectorChunkReference`、`VectorSearchRequest` 的字符串表示不得输出 raw float values。
6. 配置了 embedding model 但真实 provider 未实现时，必须安全失败，错误码固定为低敏值，不拼接 provider raw message。
7. enabled embedding/vector upsert 失败时，索引不能伪装 `INDEXED`。
8. 保存新 chunks 前应先完成 embedding/vector boundary；失败时不能留下新的 searchable chunks。
9. `VectorUpsertRequest` 不应携带 raw chunk content、question、answer、prompt、storage key、user id、API key 或 secret。
10. vector search adapter 只返回 hit id/score 等低敏候选，不返回完整 `KbDocChunk` 或 full chunk content。
11. vector search request 必须下推已授权的 `allowedKbIds`。
12. adapter 返回 hits 后，服务层必须回表加载并按 `allowedKbIds` 二次过滤，再进入 RRF/citation。
13. vector search timeout/error 应 fallback 到 keyword/recency/RRF，不能让 RAG query 整体失败。
14. `metadataJson` / `sourcesJson` 只写枚举、计数、boolean、latency、安全错误码和低敏状态；禁止写 raw vector、raw provider response、raw exception、secret 或 full chunk。
15. `IndexService` 或调用方不能盲信 provider/adapter 返回的 `errorCode`；真实接入前必须做白名单/枚举映射，未知值统一降级为固定安全错误码。
16. 真实 provider/VectorDB 接入必须新建 dependency review、security review、官方文档核验和可能的 schema/deployment review。

## 推荐状态与错误码

```text
EmbeddingStatus:
- DISABLED
- SUCCEEDED
- PROVIDER_ERROR
- DIMENSION_MISMATCH

VectorIndexStatus:
- DISABLED
- SUCCEEDED
- VECTOR_UPSERT_FAILED
- PROVIDER_ERROR

Safe error codes:
- EMBEDDING_PROVIDER_NOT_CONFIGURED
- EMBEDDING_FAILED
- VECTOR_UPSERT_FAILED
- VECTOR_SEARCH_FAILED
- DOCUMENT_INDEX_FAILED
```

## 测试建议

- `EmbeddingServiceTest`
  - provider 未配置时返回 `DISABLED`。
  - embedding model 已配置但 provider 未实现时返回安全 `PROVIDER_ERROR`。
  - document embedding 成功时返回 chunk-id-aligned vectors。
  - query embedding 成功时返回 query vector，且 `toString()` 不泄露 raw float values。
- `NoopVectorIndexAdapterTest`
  - default disabled。
  - `deleteDocument` / `upsert` / `search` 均 safe no-op。
  - request `toString()` 不含 raw chunk content、secret、apiKey 或 raw vector values。
- `IndexServiceEmbeddingVectorTest`
  - enabled embedding/provider 失败时 task/document `FAILED`。
  - 失败错误码为安全值。
  - 旧 chunks 被清理且新 chunks 未保存。
- `IndexServiceVectorPayloadTest`
  - enabled embedding/vector path 成功时 `VectorUpsertRequest` 携带 chunk vectors。
  - chunk metadata 不写 raw vectors 或 full chunk content。
- `IndexServiceTest`
  - disabled/noop 时索引成功。
  - chunk metadata 包含 `embeddingStatus=DISABLED`、`vectorIndexStatus=DISABLED`。
- `ChunkServiceVectorRetrievalTest`
  - adapter search request 携带 `allowedKbIds`。
  - adapter search request 携带 query vector，不携带 raw question。
  - forbidden vector hit 被服务层剔除。
  - adapter provider error fallback 到 keyword/recency。
  - query embedding failure 不调用 vector adapter 并 fallback。
- `RagQueryServiceTest`
  - `sourcesJson.hybrid.vectorEnabled/vectorCandidateCount` 与实际 vector branch 状态一致。
  - no-source 和 forbidden candidate 不写入 citation 或 sources metadata。

## 反模式

- 未接真实 provider 时声称语义检索已经可用。
- VectorDB adapter 返回完整 chunk 或由 adapter 自行决定最终权限。
- 全库 vector search 后只在 response 层过滤。
- 在 chunk metadata、query log、trace 或日志中保存 raw vector、raw provider error、prompt、question、full chunk 或 secret。
- 为接入真实 VectorDB 顺手修改 `pom.xml` / migration / API，但没有 Spec-first、dependency review 和 security review。
