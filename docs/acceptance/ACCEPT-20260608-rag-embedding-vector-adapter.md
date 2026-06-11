# Acceptance - P3-2-A RAG Embedding Service 与可选 VectorDB Adapter 边界

## 1. 验收结论

状态：Accepted。

本切片已按 PRD / REQ / SPEC / PLAN / TASK 完成 boundary/noop/fake 范围：RAG 离线索引和在线检索已具备 embedding service 与 optional VectorDB adapter 的内部边界，默认 disabled/noop，enabled 失败安全失败，不新增 dependency、schema、frontend 或公开 API 变更。

## 2. Done Definition

| 项目 | 状态 | 证据 |
|---|---|---|
| PRD 存在 | PASS | `docs/product/PRD-20260608-rag-embedding-vector-adapter.md` |
| REQ 存在 | PASS | `docs/requirements/REQ-20260608-rag-embedding-vector-adapter.md` |
| SPEC 存在并更新状态 | PASS | `docs/specs/SPEC-20260608-rag-embedding-vector-adapter.md` |
| PLAN / TASK 存在并关闭 | PASS | `docs/plans/PLAN-20260608-rag-embedding-vector-adapter.md`、`docs/tasks/TASK-20260608-rag-embedding-vector-adapter.md` |
| Context Pack 存在 | PASS | `docs/context/CONTEXT-20260608-rag-embedding-vector-adapter.md` |
| Evidence 存在 | PASS | `docs/evidence/EVIDENCE-20260608-rag-embedding-vector-adapter.md` |
| Changelog / Memory / TODO 更新 | PASS | `docs/changelog/CHANGELOG.md`、`docs/memory/*.md`、`docs/planning/backend-architecture-todolist.md` |
| Retrospective / Skill Extraction | PASS | `docs/retrospectives/RETRO-20260608-rag-embedding-vector-adapter.md`、`docs/skills/project-specific/rag-embedding-vector-adapter.md` |

## 3. 功能验收

| 验收项 | 结果 | 说明 |
|---|---|---|
| embedding 批量 contract | PASS | `embedDocumentChunks(documentId, kbId, chunks)` 返回稳定状态和低敏错误码。 |
| 默认 disabled/noop | PASS | provider 为 `none` 或 embedding model 为空时返回 `DISABLED`。 |
| provider 未实现安全失败 | PASS | 配置 embedding model 但无真实 provider 时返回 `EMBEDDING_PROVIDER_NOT_CONFIGURED`，索引不伪装成功。 |
| vector adapter noop | PASS | `NoopVectorIndexAdapter` disabled，delete/upsert/search 均安全 no-op。 |
| 索引失败不留下 searchable 新 chunks | PASS | enabled embedding 失败时先清理旧 chunks，不保存新 chunks，task/document 标记 `FAILED`。 |
| adapter 不接收 raw chunk content | PASS | `VectorUpsertRequest` 仅携带 `VectorChunkReference`。 |
| vector search id-only result | PASS | `VectorSearchResult` 返回 `VectorSearchHit(chunkId, score)`，服务层回表加载。 |
| allowed KB 下推与二次过滤 | PASS | `ChunkService` search request 携带 `allowedKbIds`，回表后再次按 allowed KB 过滤。 |
| query fallback | PASS | vector error 不导致 RAG retrieval 整体失败，继续 keyword/recency/RRF。 |
| metadata 安全 | PASS | 新增 metadata 不写 raw vector、full chunk、provider raw error、secret。 |

## 4. 验证结果

| 命令 | 结果 |
|---|---|
| `mvn --% -Dtest=EmbeddingServiceTest,NoopVectorIndexAdapterTest,IndexServiceEmbeddingVectorTest,ChunkServiceVectorRetrievalTest test` | BUILD SUCCESS，7 tests，0 failures，0 errors |
| `mvn test` | BUILD SUCCESS，287 tests，0 failures，0 errors，1 skipped |

## 5. 非目标确认

- 未实现真实 Spring AI embedding provider。
- 未接入真实 VectorDB SDK。
- 未新增 Flyway migration。
- 未修改 frontend。
- 未修改 public RAG API。
- 未处理复杂 PDF/DOCX/OCR 或真实页码增强。

## 6. 验收备注

本验收只关闭 P3-2 `增加 embedding service 和可选 VectorDB adapter` 的 boundary/noop/fake 切片。后续如接真实 provider/VectorDB，必须重新创建 PRD/REQ/SPEC/PLAN/TASK/Context Pack，并完成 dependency/security review。

代码复核结论：无 Critical/Important 阻断。Minor 建议已记录为后续真实 provider/VectorDB 接入约束：服务层必须对白名单外 errorCode 做安全降级，不能信任 adapter/provider 原始错误文本。
