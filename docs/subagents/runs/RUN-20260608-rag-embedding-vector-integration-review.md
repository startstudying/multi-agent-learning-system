# RUN-20260608 RAG Embedding / Vector Adapter Integration Review

## 1. 结论

本轮 P3-2-A 采用 **boundary/noop/fake 最小切片**：

- 建立 `EmbeddingService` 批量 embedding 边界。
- 建立可选 `VectorIndexAdapter` 边界与默认 disabled/noop 实现。
- 将离线索引链路接入 embedding/vector 阶段。
- 将在线检索预留 vector branch，并继续通过 `HYBRID_RRF` 融合。
- 不新增 Maven dependency，不新增 DB schema，不改公开 RAG API，不改 frontend。

真实 Spring AI embedding provider、真实 VectorDB SDK、collection/index schema、API key 配置与外部服务运行治理，全部作为后续独立切片处理。

## 2. 输入报告

| 报告 | 结论摘要 |
|---|---|
| `RUN-20260608-rag-embedding-vector-backend-expert.md` | 当前 `EmbeddingService` 仍是占位；`IndexService` 有 EMBEDDING 阶段但未真实 embedding；建议先做 service facade + adapter boundary。 |
| `RUN-20260608-rag-embedding-vector-test-plan.md` | 需要覆盖 disabled 默认、enabled 失败、metadata 不含 raw vector/content、vector branch 权限过滤与 fallback。 |
| `RUN-20260608-rag-embedding-vector-integration-plan.md` | 明确本切片范围为 `rag-embedding-vector-adapter-boundary`，不接真实 provider/VectorDB。 |
| `RUN-20260608-rag-embedding-vector-security-quality.md` | 默认关闭、无新增依赖、权限先行、VectorDB metadata 最小化、错误码脱敏。 |
| 迟到 Security subagent 通知 | 与既有报告一致，并补充：真实依赖接入前需要处理或记录当前 dependency-check 中既有 CRITICAL/HIGH 风险。 |

## 3. 冲突与裁决

| 冲突点 | 裁决 |
|---|---|
| 是否立即接真实 embedding provider | 否。本切片只定义 contract。真实 provider 后续单独 dependency review。 |
| 是否立即接真实 VectorDB | 否。本切片只提供 adapter interface 和 noop 实现。 |
| 是否新增 schema 保存 embedding 状态 | 否。最小切片复用 chunk `metadataJson` 与 query `sourcesJson` 记录短状态。 |
| vector adapter 是否可接收 chunk 正文 | 否。adapter 边界只允许接收 chunk id、KB/doc/version/hash 与 embedding 引用/向量对象，不复制 raw chunk content 到 vector metadata。 |
| vector search 是否可全库检索后过滤 | 否。必须先授权，再向 adapter 下推 `allowedKbIds`，并在服务层二次校验候选 chunk。 |
| adapter enabled 但 upsert 失败时索引是否成功 | 否。enabled 阶段失败必须写安全错误码并失败/重试，不能伪装 `INDEXED`。 |

## 4. 最终范围

### 纳入

1. `EmbeddingService`
   - 保留 `currentModelVersion()`。
   - 增加批量 chunk embedding contract。
   - 默认 disabled/noop，不调用外部 provider。
   - provider 配置存在但未实现时返回安全错误码。
2. `VectorIndexAdapter`
   - `isEnabled()`。
   - `upsert(...)`。
   - `deleteDocument(...)` 如当前切片实现成本可控则纳入；否则记录为后续一致性增强。
   - `search(...)` 必须接收 `allowedKbIds`。
3. `IndexService`
   - chunk 保存后进入 embedding/vector 阶段。
   - disabled 时索引成功并写 `DISABLED` metadata。
   - enabled 失败时写安全错误码并进入失败/重试状态。
4. `ChunkService`
   - 保留 keyword + recency + RRF。
   - adapter disabled 时保持 `vectorEnabled=false`。
   - adapter enabled 时 vector candidates 加入 RRF。
   - vector candidates 必须按 allowed KB 二次过滤。
5. `RagQueryService`
   - 不改 response DTO。
   - 在 `sourcesJson.hybrid` 中继续记录 vector 状态与候选数。

### 排除

- Spring AI provider starter。
- VectorDB SDK。
- Milvus / Chroma / OpenSearch / pgvector 生产实现。
- 新 Flyway migration。
- Frontend 改动。
- 公开 RAG API 改动。
- parser/OCR 增强。

## 5. 安全边界

- API key 不进入代码、默认配置、frontend、日志、trace、query log、memory。
- provider raw error 不持久化，只记录安全错误码。
- VectorDB metadata 不包含 chunk 正文、excerpt、question、answer、prompt、storage key、用户标识或 secret。
- vector search 必须先经过 `PermissionService.requireReadableKbIds(...)`。
- vector search request 必须带 `allowedKbIds`，并在返回后由服务层再次校验 chunk 所属 KB。
- `sourcesJson` 只写枚举、计数、布尔值、低敏状态和安全错误码。

## 6. TDD 计划

优先补齐以下 RED/GREEN 覆盖：

1. `EmbeddingServiceTest`
   - disabled 时不调用 provider，返回 `DISABLED`。
   - 配置了 embedding model 但无真实 provider 时返回安全 `PROVIDER_ERROR`。
2. `NoopVectorIndexAdapterTest`
   - 默认 disabled，upsert/search 均安全 no-op。
3. `IndexServiceTest`
   - disabled 默认索引成功，metadata 写 `embeddingStatus=DISABLED` 与 `vectorIndexStatus=DISABLED`。
   - enabled provider 未实现时索引失败，错误码为安全值。
4. `ChunkServiceVectorRetrievalTest`
   - adapter enabled 返回 vector candidates 时参与 RRF。
   - adapter 返回 forbidden chunk 时被服务层剔除。
   - adapter error/disabled 时 fallback 到 keyword/recency。
5. `RagQueryServiceTest`
   - `sourcesJson.hybrid.vectorEnabled/vectorCandidateCount` 与候选结果一致。
   - no-source 不泄露 forbidden candidate count。

## 7. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller/API 不变；Service 内部接 adapter boundary。 |
| Frontend rules | PASS | 不改 frontend，不暴露 provider key。 |
| Agent / RAG rules | PASS | RAG retrieval 继续权限先行；citations 仍由授权 chunk 生成。 |
| Security | PASS | 不新增依赖；不写 secret；vector metadata 最小化。 |
| API / Database | PASS | 不改公开 API，不改 DB schema。 |

## 8. 最终实施模式

- Parallelism：L1/L2 专家并行分析与设计已完成。
- Implementation：Main Codex 串行实现与集成。
- L3 并行编码：不启用；本切片文件高度耦合，避免多个 agent 同时修改同一 service/test。

