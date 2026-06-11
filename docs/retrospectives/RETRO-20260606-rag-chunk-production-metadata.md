# RAG Chunk 生产化元数据补齐复盘

## 有效做法

- 把 P3-2 拆成 chunk 生产元数据单独切片，避免和 parser/OCR、VectorDB、hybrid retrieval、reranker 混在一起。
- 使用无新依赖的 token-ish window 作为过渡方案，同时保留 1000 字符安全上限，降低对当前索引链路的影响。
- hash 输入包含 `documentId`、`documentVersion`、`headingPath`、`content`，避免同正文跨 heading 被错误合并。
- metadata 保持结构化且短小，只保存检索生产需要的策略和 heading 信息，不保存原文片段或 hash 原料。
- 用真实 MySQL smoke 验证 V17，而不是只依赖 H2/JPA schema。

## 问题

- 当前切分仍不是模型 tokenizer，后续接入真实 Embedding 模型后需要校准窗口大小。
- Markdown heading hierarchy 已覆盖，但 PDF/DOCX 的真实章节和页码仍没有生产级解析。
- `KbDocChunk.@PrePersist` fallback hash 只能保证旧夹具可保存，生产路径仍应由 `IndexService` 生成带 headingPath 的 hash。
- 当前 MySQL smoke 覆盖 DDL 可执行性，没有覆盖大文档批量索引性能和真实并发写入。
- 本次是接续已完成实现的收尾阶段，未回滚代码重放历史 RED 测试，只记录了当前新鲜 GREEN 证据。

## 后续改进

- P3-2 后续继续拆分：parser adapter/OCR、Embedding/VectorDB、hybrid retrieval/RRF、reranker timeout fallback。
- 若后续继续扩展 chunk 策略，可考虑把 token window、hash 原料、metadata 白名单抽成更明确的内部策略对象。
- 生产部署前建议增加大文档索引性能测试，观察 chunk 数、metadata 大小、bulk delete/save 成本和唯一索引写入成本。

## Skill Extraction

暂不创建新的项目 skill。现有 `educational-rag-pipeline` 已覆盖 RAG chunk/indexing 的基本规则，本切片沉淀为 P3-2 后续任务经验；如果后续 parser、VectorDB、hybrid retrieval 三个切片继续复用同一套 chunk 生产治理模式，再提取 `rag-index-production-hardening` 项目 skill。
