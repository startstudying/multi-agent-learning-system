# P3-2 子任务：Vector embedding payload contract 专家并行报告

日期：2026-06-10

## 任务定位

本报告汇总三个专家 subagent 的并行只读分析：

- 依赖/安全专家：`019eb226-dccf-7161-9530-1b07d8846ae4`
- RAG/后端架构专家：`019eb227-1d93-7093-bbdb-caea544879ba`
- 测试/验收专家：`019eb227-5db9-72f2-b7b4-f0293e658bb5`

专家均未修改文件。

## 集成结论

直接实现 `real VectorDB adapter` 应定为 **L**，因为会引入真实 provider 依赖、配置、可能的部署拓扑与安全审查。

当前更合适的下一刀是 **M 级前置切片**：

```text
P3-2 子任务：Vector embedding payload contract
```

原因：

- 当前 `EmbeddingService` 调用真实 `EmbeddingModel` 后只返回状态，不把向量传给 vector boundary。
- 当前 `VectorUpsertRequest` 只携带 `chunkId/chunkHash/chunkIndex`，不携带 embedding vector。
- 当前 `VectorSearchRequest` 仍携带 raw question 给 adapter；未来真实 VectorDB adapter 不应自行调用 embedding provider。
- 不先补内部 vector payload contract，后续真实 VectorDB adapter 会变成“假 real adapter”。

## Size Classification

Size：M

理由：

- 影响 RAG application 内部多个类，但不新增依赖。
- 不改公开 REST API、DTO、DB schema、frontend、parser/OCR、Agent/Orchestrator。
- 不接入外部 VectorDB provider，不新增 Docker/部署拓扑。
- 需要 REQ / SPEC / PLAN / TASK / CONTEXT 与 focused/adjacent/full backend 验证。

升级触发：

- 新增 Qdrant / Redis / Milvus / Weaviate / pgvector client 依赖。
- 新增 VectorDB runtime config、Docker Compose、schema 初始化或外部 smoke。
- 修改公开 API、DB migration 或 deployment topology。

## 设计决策

1. 新增内部 `EmbeddingVector` payload，用于承载 chunk/query embedding 向量。
2. `EmbeddingBatchResult` 在成功时返回与 chunk id 对齐的 vectors。
3. `VectorChunkReference` 增加可选 vector payload，但 `toString()` 不输出 raw vector。
4. `VectorSearchRequest` 改为携带 query vector，不把 raw question 交给 adapter。
5. `ChunkService` query 侧先通过 `EmbeddingService` 生成 query vector，再调用 `VectorIndexAdapter.search(...)`。
6. embedding query/provider 失败时，vector branch fallback 到 keyword/recency/RRF，不让 RAG query 整体失败。
7. `IndexService` 将 document chunk embeddings 传入 `VectorUpsertRequest`，但仍不把 raw vectors 写入 DB、metadata、sourcesJson、trace 或日志。
8. 默认 noop 行为保持不变：未配置 embedding/vector 时不外呼。

## 允许修改文件

生产代码：

- `backend/src/main/java/com/learningos/rag/application/EmbeddingVector.java`
- `backend/src/main/java/com/learningos/rag/application/QueryEmbeddingResult.java`
- `backend/src/main/java/com/learningos/rag/application/EmbeddingBatchResult.java`
- `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java`
- `backend/src/main/java/com/learningos/rag/application/VectorChunkReference.java`
- `backend/src/main/java/com/learningos/rag/application/VectorSearchRequest.java`
- `backend/src/main/java/com/learningos/rag/application/IndexService.java`
- `backend/src/main/java/com/learningos/rag/application/ChunkService.java`

测试：

- `backend/src/test/java/com/learningos/rag/application/EmbeddingServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/NoopVectorIndexAdapterTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceVectorPayloadTest.java`
- `backend/src/test/java/com/learningos/rag/application/ChunkServiceVectorRetrievalTest.java`

文档：

- `docs/requirements/REQ-20260610-p3-2-vector-embedding-payload-contract.md`
- `docs/specs/SPEC-20260610-p3-2-vector-embedding-payload-contract.md`
- `docs/plans/PLAN-20260610-p3-2-vector-embedding-payload-contract.md`
- `docs/tasks/TASK-20260610-p3-2-vector-embedding-payload-contract.md`
- `docs/context/CONTEXT-20260610-p3-2-vector-embedding-payload-contract.md`
- `docs/evidence/EVIDENCE-20260610-p3-2-vector-embedding-payload-contract.md`
- `docs/acceptance/ACCEPT-20260610-p3-2-vector-embedding-payload-contract.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/skills/project-specific/rag-embedding-vector-adapter.md`

## 禁止范围

- 不新增 Maven dependency。
- 不实现 Qdrant/Redis/Milvus/Weaviate/pgvector adapter。
- 不新增 VectorDB 配置、Docker Compose、schema init、外部 smoke。
- 不改 REST API、DTO、DB migration、frontend、parser/OCR、Agent/Orchestrator。
- 不把 raw vector、raw provider response、raw exception、chunk full content、question、prompt、secret 写入 metadata/log/trace/sourcesJson。

## 验收标准

- `EmbeddingService` 成功时返回 chunk id 对齐的 embedding vectors。
- embedding vector payload 的 `toString()` 不泄露 raw float values。
- `IndexService` 的 `VectorUpsertRequest` 携带每个 chunk 的 vector payload。
- `VectorSearchRequest` 使用 query vector，而不是 raw question。
- query embedding 失败时，RAG retrieval fallback 到 keyword/recency/RRF。
- `allowedKbIds` 下推与服务层二次过滤保持不变。
- 默认 noop/disabled 行为保持不变。
- full backend tests 通过。

