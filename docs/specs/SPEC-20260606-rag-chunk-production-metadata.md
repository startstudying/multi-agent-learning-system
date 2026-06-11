# SPEC - RAG Chunk 生产化元数据补齐

## 1. 数据模型

新增 Flyway migration：

```text
backend/src/main/resources/db/migration/V17__rag_chunk_production_metadata.sql
```

`kb_doc_chunk` 新增字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `chunk_hash` | `varchar(64) not null` | 稳定 SHA-256 chunk 摘要 |

新增约束与索引：

- `uk_kb_doc_chunk_document_version_hash(document_id, document_version, chunk_hash)`
- `idx_kb_doc_chunk_document_version_hash(document_id, document_version, chunk_hash)`

## 2. Chunk 生产规则

### 2.1 切分策略

- 使用 Java 标准库实现 token-ish 切分。
- 默认最大 token 窗口为 220。
- 默认 overlap 为 40。
- 不依赖第三方 tokenizer。
- 切分结果必须覆盖长文本场景，且相邻 chunk 存在重叠上下文。

### 2.2 Markdown heading hierarchy

Markdown 解析必须识别 `#` 到 `######` 标题。

每个 chunk 需附带：

- `sectionTitle`: 当前 chunk 所属最近标题。
- `headingLevel`: 最近标题层级。
- `headingPath`: 从顶层标题到当前标题的路径。

对于非 Markdown 文档，heading hierarchy 为空，但仍要保留 chunk 元数据结构的一致性。

### 2.3 稳定 hash

`chunkHash` 必须满足：

- 使用 SHA-256。
- 输入包含 `documentId`、`documentVersion`、`headingPath`、`content` 的归一化结果。
- 不包含 `chunkIndex`、`createdAt`、随机 id 或任何不稳定字段。
- 相同文档版本在重复索引时 hash 保持不变。

### 2.4 去重与唯一性

- 不再仅按正文内容做全局去重。
- 允许不同 heading 下的相同正文保留独立 chunk。
- 持久化层通过 `(document_id, document_version, chunk_hash)` 防止同版本重复 chunk 重复写入。

## 3. 元数据

`metadataJson` 允许保存：

- `parser`
- `embeddingModel`
- `contentLength`
- `chunkingStrategy`
- `chunkTokenCount`
- `overlapTokenCount`
- `headingLevel`
- `headingPath`

不允许保存：

- 原始正文片段
- overlap 原文
- hash 原料
- storage key
- provider 错误文本

## 4. 服务边界

- `IndexService` 负责 chunk 生产、hash 计算和 metadata 填充。
- `IndexTaskWorkerScheduler` 只负责调度和消费，不实现独立 chunk 规则。
- `KbDocChunkRepository` 只负责持久化查询。

## 5. 验收规则

- `IndexServiceTest` 覆盖 token-ish overlap、稳定 hash、heading hierarchy、重复正文不同标题不误去重。
- `IndexTaskWorkerSchedulerTest` 覆盖 worker 与手动路径的一致 metadata。
- `SchemaConvergenceMigrationTest` 覆盖 V17 文本。
- `MysqlMigrationSmokeTest` 覆盖真实 schema。

## 6. 实现结果

已完成。实现、验收与回顾分别见 `docs/evidence/EVIDENCE-20260606-rag-chunk-production-metadata.md`、`docs/acceptance/ACCEPT-20260606-rag-chunk-production-metadata.md`、`docs/retrospectives/RETRO-20260606-rag-chunk-production-metadata.md`。
