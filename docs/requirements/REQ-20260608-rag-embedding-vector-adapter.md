# REQ - P3-2-A RAG Embedding Service 与可选 VectorDB Adapter 边界

## 1. 追踪

- PRD：`docs/product/PRD-20260608-rag-embedding-vector-adapter.md`
- TODO：`docs/planning/backend-architecture-todolist.md` P3-2 `增加 embedding service 和可选 VectorDB adapter`

## 2. 功能需求

| 编号 | 需求 | 优先级 | 验收标准 |
|---|---|---|---|
| FR-01 | `EmbeddingService` 必须暴露批量 chunk embedding contract。 | 必须 | 测试可断言 disabled / provider-not-configured 状态。 |
| FR-02 | embedding 默认 disabled/noop，不调用外部 provider。 | 必须 | 无 provider 配置时返回 `DISABLED`。 |
| FR-03 | 配置 embedding model 但无真实 provider 实现时必须返回安全错误码。 | 必须 | 返回 `PROVIDER_ERROR` + 固定错误码，不包含 raw text/secret。 |
| FR-04 | 必须新增可选 `VectorIndexAdapter` 边界与默认 noop 实现。 | 必须 | noop `isEnabled=false`，upsert/search 安全返回 disabled。 |
| FR-05 | `IndexService` 必须在 chunk 生成后、保存新 chunk 前完成 embedding/vector 阶段，避免失败时留下 searchable 新 chunk。 | 必须 | metadata 写 `embeddingStatus` 与 `vectorIndexStatus`，成功后才保存新 chunks。 |
| FR-06 | adapter disabled 时索引可成功。 | 必须 | 文档最终 `INDEXED`，chunk metadata 为 disabled 状态。 |
| FR-07 | embedding/vector enabled 但失败时索引不得伪装成功。 | 必须 | task/document 标记失败或进入 retry，错误码为安全值。 |
| FR-08 | `ChunkService` 必须保留 keyword + recency + RRF，并可接入 vector branch。 | 必须 | vector disabled 时行为兼容；enabled 时 vector candidates 可参与融合。 |
| FR-09 | vector search 必须在 allowed KB 范围内执行。 | 必须 | request 包含 `allowedKbIds`；forbidden candidates 不进入最终结果。 |
| FR-10 | `RagQueryService` 不改变公开响应 DTO，仅记录安全 vector metadata。 | 必须 | `sourcesJson.hybrid.vectorEnabled/vectorCandidateCount` 与实际状态一致。 |

## 3. 非功能需求

| 编号 | 需求 |
|---|---|
| NFR-01 | 不新增 Maven dependency。 |
| NFR-02 | 不新增或修改 DB schema。 |
| NFR-03 | 不修改 frontend。 |
| NFR-04 | 不保存 raw vector、raw provider error、API key、Authorization header、full chunk 到 metadata/log/trace。 |
| NFR-05 | 所有新增状态值使用低基数枚举或固定错误码。 |
| NFR-06 | 测试不依赖网络、Docker、外部 VectorDB 或真实 API key。 |

## 4. 边界场景

| 场景 | 预期 |
|---|---|
| embedding provider 未配置 | `DISABLED`，索引继续成功。 |
| ai model provider 非 `none` 且 embedding model 有值但未接 provider | `PROVIDER_ERROR`，索引失败/可重试。 |
| vector adapter disabled | upsert/search 均 no-op，查询继续 keyword/recency/RRF。 |
| vector adapter 返回 forbidden KB chunk | 服务层剔除，不进入 citation/query log source。 |
| vector adapter 异常 | 查询 fallback 到 keyword/recency；索引 enabled upsert 失败则失败/重试。 |

## 6. 当前状态

状态：已完成。

验收以 `docs/evidence/EVIDENCE-20260608-rag-embedding-vector-adapter.md` 与 `docs/acceptance/ACCEPT-20260608-rag-embedding-vector-adapter.md` 为准。本轮只关闭 boundary/noop/fake，不代表真实 provider/VectorDB 已接入。

## 5. 禁止项

- 禁止 frontend 持有 embedding/LLM key。
- 禁止全库 vector search 后只在 response 层过滤。
- 禁止把 prompt/permission 写成授权控制。
- 禁止把完整向量数组塞入 `metadataJson` / `sourcesJson`。
- 禁止新增 dependency 而无 `docs/security/` review。
