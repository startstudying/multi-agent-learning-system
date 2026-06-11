# Retrospective - P3-2 Vector embedding payload contract

日期：2026-06-10

## What Changed

- 把 real VectorDB 前置缺口收窄为内部 payload contract。
- 没有引入 VectorDB provider，也没有新增依赖。
- 保持 `VectorIndexAdapter` 边界，避免未来 adapter 自行调用 embedding provider。

## What Worked

- 专家并行分析及时指出 direct real VectorDB 是 L，不应在当前切片硬上。
- TDD 的 RED compile failure 清晰证明 contract 不存在。
- focused/adjacent/full tests 都能稳定验证不破坏现有 noop/hybrid retrieval。

## What To Reuse

- 后续 real VectorDB 切片应沿用这个 contract：
  - document chunks: `EmbeddingBatchResult.vectors()` -> `VectorUpsertRequest.chunks[].vector`
  - query: `EmbeddingService.embedQuery(...)` -> `VectorSearchRequest.queryVector`
  - adapter 只做 vector storage/search，不负责 embedding。

## Follow-Up

| Follow-up | Owner | Notes |
|---|---|---|
| P3-2 real VectorDB adapter dependency/security review | Future Codex | 建议先比较 Qdrant / Redis / pgvector 等，依赖专家倾向 Qdrant。 |
| P3-2 real VectorDB adapter minimum integration | Future Codex | L 级任务，需要 PRD/REQ/SPEC/PLAN/TASK/CONTEXT。 |
| Provider-backed opt-in smoke | Future Codex | 真实 provider 接入后再加，不放入当前切片。 |

