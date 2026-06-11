# REQ：P3-2 real VectorDB adapter minimum integration

## 范围

实现 Qdrant VectorDB 最小 adapter 集成，挂接现有 `VectorIndexAdapter` 端口。

## 功能需求

### R1 默认禁用

- 系统默认使用 `NoopVectorIndexAdapter`。
- 默认启动和默认测试不连接外部 Qdrant。

### R2 显式启用

- 只有在以下配置满足时才启用真实 adapter：
  - `learning-os.rag.vector.enabled=true`
  - `learning-os.rag.vector.provider=qdrant`
  - `learning-os.rag.vector.qdrant.host` 非空
  - `learning-os.rag.vector.qdrant.port` 有效
  - `learning-os.rag.vector.qdrant.collection-name` 非空

### R3 Upsert

- `VectorIndexAdapter.upsert(VectorUpsertRequest)` 将 chunk vectors 写入 Qdrant。
- 每个 point 必须包含 vector 和低敏 payload。
- payload 只允许：`chunkId`、`kbId`、`documentId`、`documentVersion`、`chunkHash`、`chunkIndex`。

### R4 Delete

- `deleteDocument(kbId, documentId, documentVersion)` 删除指定 document/version 的已有 vectors。
- 删除失败必须返回安全错误码，不泄漏 provider 原始异常。

### R5 Search

- `search(VectorSearchRequest)` 使用 query vector 检索。
- 必须下推 `allowedKbIds` filter。
- 必须传递 `topK`。
- 返回值只能是 `VectorSearchHit(chunkId, score)`。

### R6 安全失败

- provider upsert/search/delete 异常不得抛出 raw exception 给上层。
- 错误码必须为白名单低敏值：
  - `VECTOR_UPSERT_FAILED`
  - `VECTOR_SEARCH_FAILED`
  - `VECTOR_DELETE_FAILED`
  - `VECTOR_PROVIDER_NOT_CONFIGURED`

### R7 RAG fallback

- 在线 search 失败时，`ChunkService` 继续 keyword/recency/RRF fallback。
- indexing upsert/delete 失败时，index task 不得伪装成功。

## 非功能需求

### N1 安全

- 不记录 API key。
- 不记录 raw vector values。
- 不记录 raw chunk content。
- Qdrant endpoint 只能来自静态配置。

### N2 兼容

- 不改 REST API。
- 不改 DB migration。
- 不改 frontend。
- 不改 Agent/Orchestrator contract。

### N3 可测试

- 提供 fake gateway/client 测试，不依赖真实 Qdrant。
- 默认 full backend 不需要外部服务。

## 验收需求

- RED/GREEN focused 测试。
- Adjacent RAG/vector 测试。
- Full backend `mvn test`。
- Evidence 与 Acceptance 文档。
