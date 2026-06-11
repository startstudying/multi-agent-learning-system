# Acceptance - RAG Chunk 生产化元数据补齐

## 1. 验收结论

P3-2 的 RAG chunk 生产化元数据子切片已通过验收。

本次验收仅关闭以下 TODO：

```text
补齐生产级 chunk token 切分、overlap、稳定 chunk hash 和 heading hierarchy
```

P3-2 中 parser adapter/OCR、Embedding/VectorDB、hybrid retrieval/RRF、reranker timeout fallback 仍保持未完成。

## 2. 验收清单

| 验收项 | 状态 | 证据 |
|---|---|---|
| `kb_doc_chunk` 持久化稳定 `chunk_hash` | 通过 | V17 migration、`KbDocChunk.chunkHash`、`SchemaConvergenceMigrationTest`、MySQL V17 smoke。 |
| 文档版本维度唯一约束 | 通过 | `uk_kb_doc_chunk_document_version_hash(document_id, document_version, chunk_hash)`。 |
| token-ish window chunking | 通过 | `IndexService` 使用 220 token-ish 窗口；`IndexServiceTest` 断言单 chunk token 数不超过 220。 |
| 40 token overlap | 通过 | `IndexServiceTest.processMarkdownIndexTaskUsesTokenWindowOverlapStableHashAndHeadingHierarchy` 断言相邻 chunk 前 40 token 与上一 chunk 尾部一致。 |
| 保留旧 1000 字符安全上限 | 通过 | `IndexService` 在 token window 内仍按 `MAX_CHUNK_LENGTH = 1000` 收缩；旧清洗测试保留断言。 |
| Markdown heading hierarchy | 通过 | metadata 包含 `headingLevel` 和 `headingPath`；测试断言 `["SQL Foundations", "Advanced joins"]`。 |
| 同正文不同 heading 不误去重 | 通过 | `sameTextUnderDifferentHeadingsIsNotDeduplicatedAway` 断言两个 chunk 保留且 hash 不重复。 |
| 同文档版本重复索引 hash 稳定 | 通过 | `reindexKeepsStableChunkHashesForSameDocumentVersion`。 |
| 手动索引和 worker 索引复用同一逻辑 | 通过 | `IndexTaskWorkerSchedulerTest` 断言 worker 产物包含 `TOKEN_WINDOW_V1` 与 `overlapTokenCount=40`。 |
| metadata 不保存敏感或大块原文 | 通过 | metadata 仅保存 parser、embeddingModel、contentLength、chunkingStrategy、chunkTokenCount、overlapTokenCount、headingLevel、headingPath。 |
| 后端全量回归 | 通过 | `mvn test`：231 tests, 0 failures, 0 errors, 1 skipped。 |
| 真实 MySQL smoke | 通过 | 17 migrations validated/applied，当前版本 v17。 |

## 3. 测试结果

| 命令 | 结果 |
|---|---|
| `mvn --% -Dtest=IndexServiceTest,IndexTaskWorkerSchedulerTest,SchemaConvergenceMigrationTest,MysqlMigrationSmokeTest test` | 通过：31 tests, 0 failures, 0 errors, 1 skipped |
| `mvn test` | 通过：231 tests, 0 failures, 0 errors, 1 skipped |
| `scripts/mysql-migration-smoke.ps1` with `MYSQL_PORT=3307` | 通过：MySQL 8 V1-V17 |

## 4. 非目标确认

- 未新增依赖。
- 未修改前端。
- 未修改公开 RAG query / citation API。
- 未接入真实 tokenizer、VectorDB、hybrid retrieval、RRF 或 reranker。
- 未把整个 P3-2 标记完成，只关闭 chunk 生产化元数据子项。

## 5. 审批状态

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-06 | 通过 |
