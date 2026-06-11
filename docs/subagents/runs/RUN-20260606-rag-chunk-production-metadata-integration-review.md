# RUN - RAG Chunk Production Metadata - Integration Review

## 1. 集成结论

接受 RAG Architect、Security & Quality、Test Strategy 三方建议。本切片只交付 P3-2 中“chunk token 切分、overlap、稳定 chunk hash 和 heading hierarchy”的生产化元数据能力，不关闭 P3-2 全项。

## 2. 范围裁剪

纳入本切片：

- token-ish chunk 切分。
- 固定 overlap window。
- Markdown heading hierarchy。
- 稳定 SHA-256 `chunkHash`。
- `kb_doc_chunk.chunk_hash` 和文档版本维度唯一索引。
- 手动索引与 worker 索引共用同一 chunk 生产逻辑。
- 聚焦测试、迁移文本测试、MySQL smoke 更新。

排除本切片：

- 生产级复杂 PDF/DOCX parser、OCR fallback、真实页码抽取。
- embedding provider 或 VectorDB adapter。
- hybrid retrieval、RRF、reranker timeout fallback。
- 公开 RAG query API 变更。
- 新依赖和依赖漏洞治理。

## 3. 冲突处理

| 议题 | 决策 |
|---|---|
| 是否引入 tokenizer | 不引入。使用 Java 标准库 token-ish split，后续 tokenizer 另开依赖评审。 |
| hash 是否全局唯一 | 不全局唯一。唯一约束限定在 `(document_id, document_version, chunk_hash)`。 |
| metadata 是否保存 overlap 原文 | 不保存。只保存计数、策略、heading path 等结构化元数据。 |
| 是否继续正文级去重 | 不按正文全局去重；同一 heading path 内重复 chunk 可由 hash/唯一约束保护，不牺牲跨章节引用。 |

## 4. 架构漂移预检

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 修改集中在 RAG application/domain/repository/migration/test。 |
| Frontend rules | PASS | 不修改前端。 |
| Agent / RAG rules | PASS | 只增强离线索引生产元数据，不改变 RAG answer/citation 合同。 |
| Security | PASS | 无新依赖；hash 和 metadata 脱敏；不新增公开 API。 |
| API / Database | PASS | schema 变更在 SPEC 与 V17 migration 中记录。 |

## 5. 实施模式

Main Codex 单线程实现。Subagents 已完成并行只读分析和评审，避免多个执行者修改同一 RAG 索引文件造成冲突。
