# PRD - P3-2-A RAG Embedding Service 与可选 VectorDB Adapter 边界

## 1. 问题陈述

当前 P3-2 已完成 parser、chunk metadata、index worker、hybrid retrieval/RRF/reranker fallback 等基础能力，但 `EmbeddingService` 与可选 VectorDB adapter 仍缺少稳定边界。离线索引阶段只能写入占位 embedding model，在线检索阶段只能记录 vector disabled metadata。

本切片目标是在不接真实 provider、不新增依赖、不改 DB schema、不改公开 API 的前提下，建立可测试、可审计、可后续替换的 embedding/vector adapter 边界。

## 2. 目标用户

| 用户 | 诉求 |
|---|---|
| 学生 | 后续 RAG 能演进到语义检索，当前能力不能因未配置 vector 而退化失败。 |
| 教师 | 文档索引状态可解释，索引失败有安全错误码。 |
| 管理员 | 能从 query/index metadata 判断 vector 是否启用、是否降级。 |
| 后端开发者 | 后续接真实 embedding/VectorDB 时不重写 RAG 主流程。 |

## 3. MVP 目标

- 扩展 `EmbeddingService` 为批量 chunk embedding 边界。
- 新增可选 `VectorIndexAdapter` 与默认 `NoopVectorIndexAdapter`。
- `IndexService` 接入 embedding/vector 阶段。
- `ChunkService` 支持 vector branch 参与 RRF，但默认 disabled。
- 继续记录安全 metadata，不暴露 raw vector、raw provider error、secret、full chunk。

## 4. 非目标

- 不接真实 Spring AI embedding provider。
- 不引入 VectorDB SDK。
- 不实现 Milvus / Chroma / OpenSearch / pgvector。
- 不新增 Flyway migration。
- 不改 frontend。
- 不改 public RAG API。
- 不做 parser/OCR 增强。

## 5. 成功指标

| 指标 | 目标 |
|---|---|
| 默认可用性 | 未配置 embedding/vector 时索引与查询继续成功。 |
| 失败安全性 | enabled 但 provider/adapter 失败时写安全错误码，不写 raw error。 |
| 权限安全 | vector candidates 只在 allowed KB 范围内参与融合，并经服务层二次校验。 |
| 数据最小化 | metadata 不保存 raw vector/full chunk/API key/provider raw payload。 |
| 架构稳定 | 不新增依赖、schema、frontend/API 变更。 |

## 6. 用户流程

```text
文档上传
-> index task
-> parse
-> chunk
-> delete old chunks
-> embedding boundary
-> optional vector adapter
-> save new chunks after embedding/vector success
-> mark indexed / failed with safe code

用户提问
-> permission filter
-> keyword + recency
-> optional vector search within allowed KBs
-> service-side candidate verification
-> RRF fusion
-> answer + citations
-> query log metadata
```

## 7. 依赖与后续

- 依赖现有 P3-2 parser/chunker/hybrid retrieval。
- 后续真实 provider/VectorDB 切片必须单独完成 dependency review、security review、可能的 schema review 与外部服务运维方案。

## 8. 当前状态

状态：已完成。

本切片已关闭 boundary/noop/fake 范围；真实 Spring AI embedding provider、真实 VectorDB SDK、向量集合部署和外部服务运维仍是后续独立切片。
