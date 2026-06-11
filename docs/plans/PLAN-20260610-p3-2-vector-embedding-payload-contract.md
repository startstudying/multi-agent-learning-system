# PLAN - P3-2 子任务：Vector embedding payload contract

日期：2026-06-10

## Skill Selection

| Skill | Why |
|---|---|
| feature-development-workflow | 项目要求所有 feature/bug/refactor 走 S/M/L 工作流。 |
| educational-rag-pipeline | 本切片属于 RAG indexing/retrieval 内部 pipeline。 |
| spring-ai-agent-backend | `EmbeddingService` 使用 Spring AI `EmbeddingModel`。 |
| rag-embedding-vector-adapter | 项目专属 embedding/vector 边界规则。 |
| test-driven-development | 先用 RED tests 钉住 vector payload contract。 |

Missing skills：无。

GitHub research needed：No。本切片不新增 VectorDB provider/依赖；未来 real VectorDB L 切片需要官方文档与 dependency review。

New project-specific skill：不新建；更新 `rag-embedding-vector-adapter.md`。

## Task Type

RAG / retrieval internal contract enhancement。

## Size Classification

Size：M。

理由：

- 影响 RAG application 内部多个类与测试。
- 不新增依赖、DB、API、frontend、部署拓扑。
- 不接真实 VectorDB。

升级触发：

- 引入 Qdrant/Redis/Milvus/Weaviate/pgvector 依赖。
- 新增配置、Docker、migration 或外部 smoke。
- 改公开 API/DTO。

## Subagent Decision

Use Subagents：Yes。

Parallelism Level：L1 Parallel Analysis。

专家：

- 依赖/安全专家：确认 real VectorDB 应为 L，推荐 Qdrant 作为未来候选，但本切片不接依赖。
- 架构专家：确认当前缺口是 embedding vector 未进入 internal vector contract。
- 测试专家：确认需要 RED/GREEN 覆盖 payload、fallback、过滤与 metadata 安全。

集成报告：

- `docs/subagents/runs/RUN-20260610-p3-2-vector-embedding-payload-contract-review.md`

## 实施步骤

1. 创建 RED tests：
   - embedding batch exposes chunk vectors。
   - query embedding creates query vector。
   - vector upsert request carries vectors without raw content。
   - ChunkService sends query vector and no raw question to adapter。
2. 实现 `EmbeddingVector` / `QueryEmbeddingResult`。
3. 扩展 `EmbeddingBatchResult`。
4. 更新 `EmbeddingService` document/query embedding。
5. 扩展 `VectorChunkReference`、改造 `VectorSearchRequest`。
6. 更新 `IndexService` upsert payload。
7. 更新 `ChunkService` query vector flow 与 fallback。
8. 运行 focused、adjacent、full backend tests。
9. 更新 Evidence / Acceptance / Changelog / Memory / TODO / skill。

## 风险控制

- 不在任何持久化字段写 raw vector。
- 不把 raw question 传给 vector adapter。
- 保留 noop disabled 行为。
- 不扩大到真实 provider。

