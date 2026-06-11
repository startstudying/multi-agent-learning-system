# REQ - P3-2 子任务：Vector embedding payload contract

日期：2026-06-10

## 背景

`backend-architecture-todolist.md` 中 P3-2 仍有 `real VectorDB` 后续工作。专家并行审查确认：当前代码已经具备 `EmbeddingService`、`VectorIndexAdapter`、`IndexService`、`ChunkService` 的边界，但 embedding 向量没有通过内部 contract 传递给 vector adapter。

如果直接接入真实 VectorDB，会出现两个问题：

1. document indexing 阶段没有 vector payload 可 upsert。
2. query 阶段 adapter 仍接收 raw question，未来真实 VectorDB adapter 可能被迫自行调用 embedding provider，破坏边界。

因此本切片先补齐内部 payload contract，不新增真实 VectorDB provider。

## 目标

- 让 `EmbeddingService` 成功结果携带 chunk id 对齐的 embedding vectors。
- 让 `IndexService` 把 vectors 放入 `VectorUpsertRequest` 的 chunk references。
- 让 `ChunkService` query 侧先生成 query vector，再调用 `VectorIndexAdapter.search(...)`。
- 保持默认 noop/disabled 行为。
- 不新增依赖、不改 API/DB/frontend。

## 非目标

- 不接入 Qdrant、Redis、Milvus、Weaviate、pgvector 或云 VectorDB。
- 不新增 Maven dependency。
- 不新增 VectorDB runtime config。
- 不新增 Flyway migration。
- 不修改 REST API/DTO/frontend。
- 不修改 parser/OCR/Agent/Orchestrator。
- 不关闭 P3-2 real VectorDB parent item。

## 需求

1. `EmbeddingBatchResult` 在 `SUCCEEDED` 时必须包含 embedding vectors。
2. 每个 document embedding vector 必须能和 `chunkId` 对齐。
3. query embedding 必须由 `EmbeddingService` 生成，不由 vector adapter 内部生成。
4. `VectorSearchRequest` 不应携带 raw question 给 adapter。
5. `VectorUpsertRequest` 不应携带 raw chunk content、prompt、question、secret 或 provider response。
6. vector payload 的字符串表示不能泄露 raw float values。
7. query embedding/provider 失败时，RAG query 必须 fallback 到 keyword/recency/RRF。
8. 默认未配置 embedding/vector 时，行为仍为 disabled/noop。

## 验收摘要

- focused tests 覆盖 embedding vector payload、upsert request payload、query vector search、fallback。
- adjacent tests 覆盖 indexing、retrieval、RAG query 相关路径。
- full backend `mvn test` 通过，或明确记录无法运行原因。

