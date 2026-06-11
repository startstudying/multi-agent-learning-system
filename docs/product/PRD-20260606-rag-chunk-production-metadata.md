# PRD - RAG Chunk 生产化元数据补齐

## 1. 背景

当前 RAG 索引链路已经具备文档上传、基础切分、索引任务、worker 自动执行和进度可观测能力，但 chunk 生产仍停留在基础字符切分和简单去重阶段，缺少面向生产的稳定 hash、窗口重叠和标题层级信息。

## 2. 目标

在不改变公开 RAG 查询合同的前提下，补齐生产级 chunk 元数据能力，使索引结果更稳定、更可追踪，并为后续 VectorDB、hybrid retrieval、reranker 与更强解析器留下干净的 schema 基础。

## 3. 范围

纳入：

- token-ish chunk 切分。
- chunk overlap。
- 稳定 `chunkHash`。
- Markdown heading hierarchy。
- `kb_doc_chunk` schema 生产化增强。
- 手动索引与 worker 索引共用同一 chunk 生产逻辑。
- 对应测试、迁移、文档更新。

不纳入：

- 真实 tokenizer 依赖。
- 复杂 PDF/DOCX 解析增强。
- OCR fallback。
- VectorDB adapter。
- hybrid retrieval、RRF、reranker。
- 公开 RAG query / citation API 改动。

## 4. 成功标准

- 同一文档版本重复索引后，chunk hash 稳定。
- 长文本能够按窗口切出多个 chunk，且存在可控 overlap。
- Markdown 章节层级写入 chunk 元数据。
- 相同正文在不同 heading 下不会被错误合并。
- V17 migration 与 MySQL smoke 可验证新 schema。

## 5. 非目标

本切片不负责把所有生产级解析器一次做完，也不承诺完成 P3-2 的整条 RAG 索引生产化闭环。它只补 chunk 生产元数据这一段。

## 6. 交付状态

已完成。实现与验收见 `docs/evidence/EVIDENCE-20260606-rag-chunk-production-metadata.md` 和 `docs/acceptance/ACCEPT-20260606-rag-chunk-production-metadata.md`。
