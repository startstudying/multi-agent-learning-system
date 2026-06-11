# RUN - RAG Chunk Production Metadata - RAG Architect

## 1. 角色

RAG / Backend Architect。

## 2. 结论

本切片应保持为 RAG 索引 chunk 生产化元数据的最小闭环，不扩展到 VectorDB、hybrid retrieval、reranker、复杂 PDF/DOCX/OCR 或公开查询 API 调整。

## 3. 建议方案

- 在 `IndexService` 中替换现有按字符长度的固定切分，改为基于 Java 标准库的 token-ish 切分。
- 使用固定 `maxTokens` 与 `overlapTokens` 窗口，保证相邻 chunk 有可控重叠。
- Markdown 解析保留 heading stack，输出 `h1` 到 `h6` 的层级路径。
- 为每个 chunk 生成稳定 SHA-256 `chunkHash`。
- hash 计算需包含 `documentId`、`documentVersion`、heading path、chunk 内容归一化值，避免同文档不同章节的重复内容被误合并。
- `kb_doc_chunk` 增加 `chunk_hash` 列，并建立 `(document_id, document_version, chunk_hash)` 唯一约束或唯一索引。
- 公开 RAG 查询 API 保持不变；生产元数据通过实体和持久化表内部演进。

## 4. 风险

| 风险 | 建议 |
|---|---|
| tokenizer 依赖带来供应链和维护成本 | 本切片不引入依赖，先使用 token-ish split |
| heading 元数据过大 | metadata 中只保存短路径、等级和标题，不保存正文片段 |
| reindex 后 hash 不稳定 | hash 输入使用稳定字段和归一化内容，不包含 chunkIndex、createdAt 或随机 id |
| 误去重导致引用丢失 | 不再仅按正文内容全局去重；至少按文档版本与 heading path 区分 |

## 5. 验收建议

- 同一文档重复索引后，chunk 内容相同时 `chunkHash` 一致。
- 长文本产生多个 chunk，且相邻 chunk 之间存在受控 overlap。
- Markdown 多级标题进入 metadata 和 `sectionTitle`。
- 不同 heading 下相同正文不会被错误合并为一个 chunk。
- V17 migration 和 MySQL smoke 覆盖 `chunk_hash` 列与唯一索引。
