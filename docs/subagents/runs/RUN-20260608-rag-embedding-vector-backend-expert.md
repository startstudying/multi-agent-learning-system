# RUN-20260608 RAG Embedding / Vector Adapter Backend Expert

## 1. 任务类型 / 技能选择

- 任务类型：RAG / retrieval / indexing 架构分析。
- 关联 TODO：`docs/planning/backend-architecture-todolist.md` P3-2 `增加 embedding service 和可选 VectorDB adapter`。
- 相关技能：`educational-rag-pipeline`、`rag-hybrid-retrieval`、`model-gateway-boundary`、`rag-parser-boundary`。
- GitHub research：本 boundary 切片不需要；真实 provider / VectorDB dependency 切片需要。

## 2. 现有代码事实

### 当前 P3-2 状态

- P3-2 中 `hybrid retrieval、RRF、reranker timeout fallback` 已完成。
- 剩余与本切片相关的是 `增加 embedding service 和可选 VectorDB adapter`。
- `PROJECT_MEMORY` 与 `AGENT_RAG_MEMORY` 均确认真实 embedding / VectorDB 仍是后续增强。

### Embedding 仍是占位

- `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java` 只有 `currentModelVersion()`，返回 `noop-embedding-v1`。
- 当前没有 `embedDocuments` / `embedQuery` / provider 调用 / 向量维度。
- `IndexService` 已注入 `EmbeddingService`，但只把 model version 写入 chunk metadata。

### Indexing pipeline 已有 EMBEDDING 阶段，但未实际嵌入

- `IndexService.processIndexTask` 在 chunking 后设置 `indexStatus=EMBEDDING`，随后直接删除旧 chunks 并保存新 chunks。
- Worker、lease、retry、progress 机制已可复用。
- 当前 `EMBEDDING` 是阶段名，不代表已有真实 embedding。

### Retrieval 已预留 vector metadata

- `ChunkService.retrieveAllowedChunks(...)` 已实现 keyword + recency + RRF。
- `RetrievalResult` 已有 `vectorCandidateCount` 与 `vectorEnabled`。
- `RagQueryService` 已把 `vectorEnabled=false` / `vectorCandidateCount=0` 写入 `sourcesJson.hybrid`。

### Schema 当前无 vector 持久化字段

- `kb_doc_chunk` 只有 chunk 文本、页码、章节、metadata、`chunk_hash`。
- V17 只新增 `chunk_hash` 和唯一/查询索引。
- `KbDocChunk` 实体没有 embedding/vector 状态字段。

### 模型边界已有但不覆盖 embedding

- `AiModelGateway` 当前覆盖结构化生成、safe error 和 metrics。
- `AiModelProperties` 已有 `embeddingModel` 字段。
- `application.yml` 暴露 `AI_EMBEDDING_MODEL`，但没有 VectorDB adapter 配置。

## 3. 推荐最小切片边界

推荐做 **Embedding Service facade + Noop/disabled VectorIndexAdapter + IndexService/ChunkService 接入点**，不改变公开 API。

纳入范围：

1. 扩展 `EmbeddingService`：
   - 保留 `currentModelVersion()`。
   - 增加 `embedDocuments(...)`、`embedQuery(...)`。
   - 默认实现 deterministic/noop，不接真实 provider。
2. 新增 `VectorIndexAdapter`：
   - `enabled()`
   - `upsertDocumentChunks(...)`
   - `deleteDocument(...)`
   - `search(allowedKbIds, queryEmbedding, topK)`
   - 默认 `NoopVectorIndexAdapter` 返回空命中。
3. `IndexService`：
   - chunk 保存后可进入 embedding / vector upsert。
   - adapter disabled 时索引成功，metadata 明确 disabled。
   - adapter enabled 但 upsert 失败时写安全错误码并进入失败/重试。
4. `ChunkService`：
   - 保留 keyword + recency + RRF。
   - 增加 vector branch，仅接收 allowed KB 后搜索并参与 RRF。
   - disabled 时保持当前行为与测试兼容。
5. `RagQueryService`：
   - 继续记录 `vectorEnabled` / `vectorCandidateCount`。
   - 不改变公开 DTO。

## 4. 依赖 / Schema 建议

### 最小切片不新增依赖

- `backend/pom.xml` 已有 Spring AI BOM，但没有 provider starter。
- 本切片只做接口、noop/fake adapter、测试 contract，不新增 Maven dependency。

### 最小切片不强制新增 DB schema

- 可先使用现有 `metadataJson` 与 `sourcesJson` 表达 disabled / vector metadata。
- 外部 vector id 可由 `documentId + documentVersion + chunkHash` 构造。
- 若进入真实生产 VectorDB，再新增 V18 schema 记录 vector 状态。

### 后续真实 provider 建议新增 schema

长期可新增：

- `vector_id`
- `embedding_model`
- `embedding_dimension`
- `embedding_status`
- `vector_index_name`
- `vector_indexed_at`
- `vector_error_code`

## 5. 允许修改文件建议

必改候选：

- `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java`
- `backend/src/main/java/com/learningos/rag/application/IndexService.java`
- `backend/src/main/java/com/learningos/rag/application/ChunkService.java`
- `backend/src/main/java/com/learningos/rag/application/RetrievalResult.java`
- `backend/src/main/java/com/learningos/config/RagProperties.java`
- `backend/src/main/resources/application.yml`

可新增：

- `backend/src/main/java/com/learningos/rag/application/EmbeddingVector.java`
- `backend/src/main/java/com/learningos/rag/application/VectorIndexAdapter.java`
- `backend/src/main/java/com/learningos/rag/application/NoopVectorIndexAdapter.java`
- 相关测试类。

不建议触碰：

- `frontend/**`
- 公开 RAG DTO
- 真实 provider SDK dependency
- 无关 Agent / Assessment / Resource 模块

## 6. API 是否应变化

建议不改变公开 API。

- `RagQueryResponse` 保持 `answer/sources/traceId/retrieval`。
- vector 细节继续写内部 `sourcesJson.hybrid`。
- 前端无需改动。

## 7. 架构漂移风险

| 风险 | 说明 | 建议 |
|---|---|---|
| 权限后过滤 | VectorDB 若先全库搜索再过滤，会违反 RAG 权限规则 | search request 必须携带 allowed KB，服务层二次校验 |
| 新依赖未评审 | 直接引入 provider / VectorDB SDK 违反依赖门禁 | 本切片不新增依赖；真实接入单独 dependency review |
| provider raw error 泄漏 | embedding/vector 错误不能进入日志/API | 只写安全错误码 |
| stale vector | reindex 删除 MySQL chunk 后旧向量残留 | upsert 前 delete/覆盖 document version 向量 |
| API 契约漂移 | 公开暴露 vector details 会扩大前端变更 | 保持 API 不变 |

## 8. 后续衔接

阶段 A：本轮边界切片。

- embedding facade。
- noop vector adapter。
- IndexService / ChunkService 接入点。
- disabled 默认行为。

阶段 B：真实 provider / VectorDB。

- Spring AI embedding adapter。
- 具体 VectorDB adapter。
- dependency review / official docs / GitHub reference。
- V18 schema 或独立 vector metadata 表。

## 9. 结论

本轮建议先做无依赖、无 API 变更、可测试的 embedding/vector adapter boundary。这样能把索引、检索、权限、日志、失败语义固定住，同时避免一次性引入真实 provider、VectorDB、schema 和运维风险。
