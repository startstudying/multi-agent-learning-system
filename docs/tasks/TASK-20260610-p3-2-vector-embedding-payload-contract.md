# TASK - P3-2 子任务：Vector embedding payload contract

日期：2026-06-10

## Goal

补齐 RAG embedding vector 在内存中从 `EmbeddingService` 到 `VectorIndexAdapter` 的 payload contract，为后续 real VectorDB adapter 做前置准备。

## Scope

- 新增内部 vector payload record。
- document embedding 成功结果携带 chunk vectors。
- query embedding 成功结果携带 query vector。
- vector upsert/search request 使用 vectors。
- 保持默认 disabled/noop。

## Checklist

- [x] RED tests 创建并失败。
- [x] `EmbeddingVector` / `QueryEmbeddingResult` 实现。
- [x] `EmbeddingBatchResult` 携带 vectors。
- [x] `EmbeddingService` 返回 document/query vectors。
- [x] `VectorChunkReference` 携带 vector 且不泄露 raw vector。
- [x] `VectorSearchRequest` 使用 query vector，不携带 raw question。
- [x] `IndexService` upsert request 注入 chunk vectors。
- [x] `ChunkService` query 侧先生成 query vector，失败 fallback。
- [x] Focused tests 通过。
- [x] Adjacent tests 通过。
- [x] Full backend tests 通过或记录限制。
- [x] Evidence / Acceptance / Changelog / Memory / TODO / skill 更新。

## Acceptance Criteria

- `EmbeddingBatchResult.vectors()` 与 chunk id 对齐。
- query vector 由 `EmbeddingService` 生成。
- `VectorUpsertRequest` 包含 vector payload，但不包含 raw chunk content。
- `VectorSearchRequest` 不包含 raw question。
- vector payload `toString()` 不泄露 float values。
- query embedding failure 不导致 RAG query 失败。
- P3-2 real VectorDB parent item 保持 open。
