# RUN-20260608 RAG Embedding / Vector Adapter Integration Plan

## 1. 推荐切片

推荐切片名：

```text
P3-2-A rag-embedding-vector-adapter-boundary
```

中文标题：

```text
RAG Embedding Service 与可选 VectorDB Adapter 边界切片
```

## 2. 纳入范围

1. Embedding Service 从占位升级为可测试边界。
2. 新增可选 VectorDB adapter interface。
3. 默认实现为 noop / disabled，不引入真实 provider 或 VectorDB dependency。
4. 离线索引链路接入 embedding/vector 阶段。
5. 在线检索预留 vector branch 并与现有 RRF 衔接。
6. 保持公开 RAG API 不变。
7. 记录安全 metadata，不记录 raw vector、provider raw error、API key、full chunk。

## 3. 排除范围

- 不接真实 Spring AI embedding provider。
- 不新增 VectorDB 客户端依赖。
- 不实现生产级 Milvus / Chroma / OpenSearch / pgvector。
- 不做 parser/OCR 增强。
- 不改 frontend。
- 不改公开 RAG API。

## 4. 与刚完成 hybrid retrieval 的衔接

当前链路：

```text
ChunkService.retrieveAllowedChunks(...)
-> keyword branch
-> recency branch
-> vector disabled metadata
-> RRF fusion
-> RerankerService fallback
-> RagQueryService sourcesJson
```

建议：

- 保持 `HYBRID_RRF` 不变。
- vector branch 成为 `HYBRID_RRF` 的一个候选分支。
- `RetrievalResult` 可补 `vectorStatus`、`vectorLatencyMs`、`vectorErrorCode`。
- adapter disabled 时现有测试不应大改。

## 5. 预期冲突与决策建议

| 冲突点 | 预期分歧 | 建议决策 |
|---|---|---|
| 是否立即接真实 provider | RAG/模型专家希望直接用 Spring AI；安全/测试要求先做边界 | 先做 boundary/noop/fake |
| 是否新增 VectorDB 依赖 | 后端可能倾向一次接 SDK | 本切片不新增依赖 |
| embedding 失败是否让索引失败 | 产品希望 keyword 可用；质量担心静默降级 | disabled 可成功；enabled 失败应失败或 retry |
| 是否改 DB schema | 后端可能想加 vector 状态字段 | 最小切片不强制；真实接入再加 V18 |
| vector 权限过滤 | 性能想直接查 VectorDB | 必须 filter + 二次校验 |
| query log 记录粒度 | 观测想记录候选详情 | 只记计数、状态、耗时、安全错误码 |

## 6. GitHub Reference Gate

本 boundary/noop 切片不强制需要。

以下情况需要开启：

- 选择真实 VectorDB：Milvus、Chroma、OpenSearch、pgvector 等。
- 新增 Spring AI embedding provider 或 VectorStore dependency。
- 需要确定 collection / namespace / metadata filter 最佳实践。

## 7. PRD / REQ / SPEC / PLAN / TASK 关键点

### PRD

- 目标：为 RAG 索引链路建立 embedding/vector 扩展边界。
- 非目标：不接真实 provider / VectorDB，不改变公开 API。
- 用户价值：为语义检索和 RAG quality 提升铺路。

### REQ

- Embedding service 支持批量 chunk embedding contract。
- 默认 disabled/noop 不影响现有索引成功。
- Vector adapter disabled 时保持 `vectorEnabled=false`。
- adapter enabled 时 upsert 失败写安全错误码和可恢复状态。
- vector search 结果必须按 allowed KB 二次校验。
- query log/task error 不保存 raw vector/raw provider error/API key/full chunk。
- 重索引必须避免旧版本向量残留。

### SPEC

- 新接口：`EmbeddingService`、`VectorIndexAdapter`、`VectorSearchResult`。
- 状态：`DISABLED`、`SUCCEEDED`、`PROVIDER_ERROR`、`DIMENSION_MISMATCH`、`VECTOR_UPSERT_FAILED`。
- 流程：`parse -> chunk -> save chunks -> embed -> vector upsert -> mark indexed`。
- disabled/enabled 行为差异。
- 权限过滤策略。
- 失败和 retry 策略。

### PLAN / TASK

建议拆成：

1. `TASK-01 embedding-contract`
2. `TASK-02 vector-adapter-contract`
3. `TASK-03 indexing-integration`
4. `TASK-04 retrieval-metadata-and-regression`

## 8. 实施顺序建议

1. 写 PRD/REQ/SPEC/PLAN/TASK/Context。
2. 写 RED contract tests。
3. 实现 embedding contract。
4. 实现 noop vector adapter。
5. 接入 IndexService，默认 disabled。
6. 衔接 ChunkService / RAG query metadata。
7. 跑 focused / adjacent / full tests。
8. 补 Evidence / Acceptance / Changelog / Memory / Skill。

## 9. 最终建议

本轮不要直接接真实 VectorDB 或 Spring AI embedding provider。先做 **Embedding Service + VectorDB Adapter Boundary**，用 noop/fake 实现把索引、检索、权限、日志、失败语义固定住。真实 provider / VectorDB 作为下一独立切片。
