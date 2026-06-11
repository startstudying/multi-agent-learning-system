# Evidence - P3-2-A RAG Embedding Service 与可选 VectorDB Adapter 边界

## 1. 证据摘要

本切片完成无新增依赖、无 DB schema 变更、无 frontend 变更、无公开 API 变更的 RAG embedding/vector adapter boundary：

- `EmbeddingService` 提供批量 chunk embedding contract。
- `VectorIndexAdapter` 提供 `deleteDocument`、`upsert`、`search` 边界，默认 `NoopVectorIndexAdapter` disabled。
- `IndexService` 在保存新 chunks 前执行 `EMBEDDING` / `VECTOR_UPSERT` 边界，enabled 失败不会留下新的 searchable chunks。
- `VectorUpsertRequest` 只传 `VectorChunkReference(chunkId, chunkHash, chunkIndex)`，不传 raw chunk content。
- `VectorSearchResult` 只返回 `VectorSearchHit(chunkId, score)`，由 `ChunkService` 回表加载并按 `allowedKbIds` 二次过滤后再进入 RRF。
- 默认 disabled/noop 时索引与查询继续成功，并记录短状态 metadata。

## 2. Fresh GREEN 验证

### 2.1 P3-2-A 聚焦测试

命令：

```powershell
cd backend
mvn --% -Dtest=EmbeddingServiceTest,NoopVectorIndexAdapterTest,IndexServiceEmbeddingVectorTest,ChunkServiceVectorRetrievalTest test
```

结果：

```text
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-08T10:20:10+08:00
```

覆盖点：

- embedding 未配置时返回 `DISABLED`，不调用外部 provider。
- embedding model 已配置但本切片无真实 provider 时返回 `PROVIDER_ERROR` + `EMBEDDING_PROVIDER_NOT_CONFIGURED`。
- noop vector adapter disabled，`deleteDocument` / `upsert` / `search` 均安全 no-op。
- vector upsert request 不暴露 raw chunk content、secret 或 API key。
- enabled embedding 失败时 task/document 标记失败，错误码安全，旧 searchable chunks 被清理且不保存新 chunks。
- vector search request 下推 `allowedKbIds`，adapter 返回 forbidden hit 时服务层剔除。
- vector provider error 时 fallback 到 keyword/recency/RRF，不让 RAG retrieval 整体失败。

### 2.2 全量后端测试

命令：

```powershell
cd backend
mvn test
```

结果：

```text
Tests run: 287, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
Finished at: 2026-06-08T10:12:34+08:00
```

说明：

- 首次全量命令使用 124 秒工具超时，未产生失败输出；确认无残留 Java/Maven 进程后，以更长超时重新执行同一 `mvn test` 并通过。
- Maven 输出包含 Mockito / ByteBuddy 动态 agent 的 JDK 未来兼容性 warning；本切片未新增该行为，后续可单独治理。

## 3. 需求到证据映射

| 需求 | 证据 | 状态 |
|---|---|---|
| FR-01 `EmbeddingService` 批量 contract | `EmbeddingServiceTest` | PASS |
| FR-02 默认 disabled/noop | `EmbeddingServiceTest.returnsDisabledBatchResultWhenEmbeddingProviderIsNotConfigured` | PASS |
| FR-03 provider 未实现时安全错误码 | `EmbeddingServiceTest.reportsConfiguredEmbeddingModelWithoutCallingExternalProvider` | PASS |
| FR-04 `VectorIndexAdapter` + noop | `NoopVectorIndexAdapterTest.staysDisabledAndReturnsSafeResults` | PASS |
| FR-05 chunk 生成后、保存前执行 embedding/vector | `IndexServiceEmbeddingVectorTest.embeddingProviderFailureDoesNotLeaveSearchableChunksOrRawError` | PASS |
| FR-06 adapter disabled 索引成功 | `IndexServiceTest` metadata 断言 `embeddingStatus=DISABLED` / `vectorIndexStatus=DISABLED`，全量测试通过 | PASS |
| FR-07 enabled 失败不伪装成功 | `IndexServiceEmbeddingVectorTest` 断言 task/document `FAILED` | PASS |
| FR-08 vector branch 参与 RRF / error fallback | `ChunkServiceVectorRetrievalTest` | PASS |
| FR-09 allowed KB 下推与二次过滤 | `ChunkServiceVectorRetrievalTest.filtersVectorCandidatesAgainstAllowedKnowledgeBasesBeforeRrfFusion` | PASS |
| FR-10 `sourcesJson` 安全 vector metadata | `RagQueryServiceTest` 相关回归在全量测试中通过 | PASS |

## 4. 依赖 / Schema / Frontend / API Gate

| Gate | 结果 | 说明 |
|---|---|---|
| Dependency | PASS | 未修改 `backend/pom.xml`，未新增 Maven dependency。 |
| DB schema | PASS | 未新增或修改 `backend/src/main/resources/db/migration/**`。 |
| Frontend | PASS | 未修改 `frontend/**`。 |
| Public API | PASS | 未修改 `/api/rag/query` 或 Orchestrator `RAG_QA` 合同。 |
| Secrets | PASS | 未新增 API key、secret、credential 配置或日志。 |

## 5. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller/API 不感知 embedding/vector；RAG application service 编排 adapter boundary；Repository 仍由 Service 使用。 |
| Frontend rules | PASS | 未修改 frontend，未引入前端 LLM/embedding 调用。 |
| Agent / RAG rules | PASS | RAG retrieval 继续权限先行；vector request 下推 allowed KB，返回后服务层二次过滤；citation 仍来自授权 chunk。 |
| Security | PASS | 不新增依赖，不写 secret；provider raw error 不持久化；adapter search 返回 id-only hit。 |
| API / Database | PASS | API 与 SPEC 一致；无 schema 变更；无未记录 endpoint。 |

## 6. 剩余边界

- 真实 Spring AI embedding provider 未实现。
- 真实 VectorDB SDK、collection/index schema、部署、备份和运维告警未实现。
- 真实 provider/VectorDB 接入前必须重新执行 dependency review、security review、官方文档核验和可能的 schema review。
- 代码复核发现当前 `safePipelineError(...)` 对本切片固定错误码安全；后续真实 provider/VectorDB 接入前必须增加服务层 errorCode 白名单/枚举映射，未知 provider/adapter 错误统一降级为固定安全错误码。
- 复杂 PDF/DOCX/OCR、真实页码和章节层级增强不属于本切片。
