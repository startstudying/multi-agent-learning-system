# SPEC：P3-2 real VectorDB adapter minimum integration

## 架构决策

### 端口

保留现有 application port：

- `com.learningos.rag.application.VectorIndexAdapter`

真实 Qdrant adapter 实现该端口。`IndexService`、`ChunkService` 不直接依赖 Qdrant SDK 或 Spring AI `VectorStore`。

### 依赖

新增：

- `org.springframework.ai:spring-ai-qdrant-store`

不新增：

- `spring-ai-starter-vector-store-qdrant`
- Testcontainers
- Qdrant server runtime dependency

### 配置

新增配置命名空间：

```yaml
learning-os:
  rag:
    vector:
      enabled: ${RAG_VECTOR_ENABLED:false}
      provider: ${RAG_VECTOR_PROVIDER:none}
      qdrant:
        host: ${RAG_VECTOR_QDRANT_HOST:}
        port: ${RAG_VECTOR_QDRANT_PORT:6334}
        use-tls: ${RAG_VECTOR_QDRANT_USE_TLS:false}
        api-key: ${RAG_VECTOR_QDRANT_API_KEY:}
        collection-name: ${RAG_VECTOR_QDRANT_COLLECTION:learning_os_chunks}
        initialize-schema: ${RAG_VECTOR_QDRANT_INITIALIZE_SCHEMA:false}
```

默认值保证不外连。

## 新增类

### `RagVectorProperties`

位置：

- `backend/src/main/java/com/learningos/config/RagVectorProperties.java`

职责：

- 读取 `learning-os.rag.vector.*`。
- 判断是否启用 Qdrant。
- 对 host、port、collection 做基本校验。

### `QdrantVectorOperations`

位置：

- `backend/src/main/java/com/learningos/rag/vector/QdrantVectorOperations.java`

职责：

- 作为真实 Qdrant client 的窄接口，方便用 fake operations 做单元测试。
- 只暴露项目需要的操作：
  - `deleteDocument(...)`
  - `upsert(...)`
  - `search(...)`
- Native 实现只在该类内使用 Qdrant Java client API，避免 application service 直接接触 SDK。

### `QdrantVectorPoint`

位置：

- `backend/src/main/java/com/learningos/rag/vector/QdrantVectorPoint.java`

职责：

- 表示待写入 VectorDB 的低敏 point。
- 不包含 raw chunk content。

### `QdrantVectorIndexAdapter`

位置：

- `backend/src/main/java/com/learningos/rag/vector/QdrantVectorIndexAdapter.java`

职责：

- 实现 `VectorIndexAdapter`。
- 映射 `VectorUpsertRequest` -> `QdrantVectorPoint`。
- 映射 `VectorSearchRequest` -> Qdrant search request。
- 捕获 provider 异常并返回安全结果。

### `QdrantVectorConfiguration`

位置：

- `backend/src/main/java/com/learningos/rag/vector/QdrantVectorConfiguration.java`

职责：

- 条件装配 `QdrantVectorIndexAdapter`。
- 当未启用或 provider 非 `qdrant` 时不创建真实 adapter。
- 在不存在其他 `VectorIndexAdapter` bean 时，通过 `@ConditionalOnMissingBean(VectorIndexAdapter.class)` 提供 Noop fallback bean。

## 修改类

### `NoopVectorIndexAdapter`

修改：

- 移除 component-scanned Spring service 语义，保留为普通实现类。
- Noop fallback 的 Spring bean 注册由 `QdrantVectorConfiguration` 统一负责。

目的：

- 真实 adapter 存在时不产生多 bean 冲突；默认禁用时仍稳定提供 Noop adapter。

### `application.yml`

修改：

- 增加 `learning-os.rag.vector.*` 默认禁用配置。

### `pom.xml`

修改：

- 增加 `spring-ai-qdrant-store` 依赖。

## 数据映射

### Upsert point payload

允许字段：

```json
{
  "chunkId": "chk_x",
  "kbId": "kb_x",
  "documentId": "doc_x",
  "documentVersion": 1,
  "chunkHash": "sha256...",
  "chunkIndex": 0
}
```

禁止字段：

- `content`
- `question`
- `answer`
- `prompt`
- `storageBucket`
- `storageKey`
- `userId`
- `apiKey`
- `secret`

### Search filter

- 对 `allowedKbIds` 构造 Qdrant payload filter：`kbId in allowedKbIds`。
- 即使 filter 下推，服务层仍按 `allowedKbIds` 回表二次过滤。

### Native Qdrant API mapping

- Upsert 使用 `PointStruct.Builder#setVectors(VectorsFactory.vectors(...))` 写入向量。
- Upsert 使用 `PointStruct.Builder#putAllPayload(...)` 写入白名单 payload。
- Search 使用 `SearchPoints.Builder#setWithPayload(...)` 且 include fields 仅包含 `chunkId`。
- Search 使用 `SearchPoints.Builder#setWithVectors(WithVectorsSelector(enable=false))` 禁止返回 vector。
- Delete 使用 `kbId + documentId + documentVersion` payload filter 删除同一文档版本的 points。
- Provider 异常在 adapter 层折叠为固定错误码，不把 raw exception、host、api key 或 provider response 写入业务结果。

## 错误码

| 场景 | 返回 |
|---|---|
| 缺配置 | `VECTOR_PROVIDER_NOT_CONFIGURED` |
| delete 失败 | `VECTOR_DELETE_FAILED` |
| upsert 失败 | `VECTOR_UPSERT_FAILED` |
| search 失败 | `VECTOR_SEARCH_FAILED` |

## 测试策略

### Focused

- `QdrantVectorIndexAdapterTest`
- `RagVectorConfigurationTest`

### Adjacent

- `NoopVectorIndexAdapterTest`
- `IndexServiceVectorPayloadTest`
- `IndexServiceEmbeddingVectorTest`
- `ChunkServiceVectorRetrievalTest`
- `RagQueryServiceTest`

### Full

- `mvn test`

## Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 不变；Service 通过 adapter port |
| Frontend rules | PASS | 不改 frontend，不增加前端 LLM/API key |
| Agent / RAG rules | PASS | permission filter 在 retrieval 前；citation 仍回表 |
| Security | PASS with dependency review | 新依赖已有 review；默认禁用 |
| API / Database | PASS | 不改 API/DB |
