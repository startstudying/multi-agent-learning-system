# REQ-20260610 P3-2 子任务：industrial parser reading-order/confidence/page-number metadata

## 1. 背景

`backend-architecture-todolist.md` 中 P3-2 仍有工业级 PDF/DOCX layout/table/TOC/reading-order、native/cloud OCR、OCR confidence 与真实渲染页码增强未完成。专家 subagent 复核后确认：不应一次性接入重型 layout/OCR 能力，应先补 parser 输出契约，让后续 provider 能安全承载阅读顺序、页码来源、confidence 和内容类型。

当前问题：

- `ParsedSection` 只有 `title / headingLevel / headingPath / content / pageNum`。
- `OcrFallbackResult` 只有 `status / reasonCode / text`。
- `IndexService` chunk metadata 只保存 parser、embedding/vector、chunking、heading 信息，无法保留短结构化 parser metadata。

## 2. 任务类型

RAG parser backend architecture / parser contract enhancement / indexing metadata hardening。

## 3. 需求

1. 为 parser section 增加短结构化 metadata：
   - `pageNumSource`
   - `readingOrderIndex`
   - `layoutConfidence`
   - `ocrConfidence`
   - `contentKind`
2. 为 OCR fallback result 增加可选 `confidence`，供后续 OCR provider 返回页级或文档级置信度。
3. 保持现有 parser provider 与测试 fixture 的旧构造器兼容，避免大范围无意义改动。
4. `ParsedDocument` 应保证 section 的 `readingOrderIndex` 有稳定顺序；已有 provider 不显式提供时按 section 顺序自动填充。
5. `IndexService` 应把上述短字段写入 chunk metadata JSON，不写 raw OCR 文本、provider response、stderr、路径、secret 或大块布局 payload。
6. 当前 PDFBox/POI/lightweight parser 不声明完成工业级 layout/table/TOC；只提供 metadata 承载和 best-effort 默认值。
7. 不改变公开 HTTP API、DTO、DB schema、依赖、VectorDB、retrieval、citation response。

## 4. 非目标

- 不接入 native/cloud OCR。
- 不新增 PDF/DOCX layout/table/TOC SDK。
- 不新增 `kb_doc_chunk` 字段；metadata 仍写入 `metadataJson`。
- 不改变 RAG query response 的 citation DTO。
- 不改变上传、索引任务、VectorDB adapter 合同。
- 不关闭 P3-2 工业 parser 父项。

## 5. 验收标准

- `ParsedSection` 新 metadata 默认值安全、兼容旧构造器。
- `ParsedDocument` 为未显式设置 reading order 的 section 填充稳定 1-based 顺序。
- OCR fallback 成功时可把 `ocrConfidence` 和 `contentKind=OCR_TEXT` 传入 section。
- `IndexService` chunk metadata 包含短字段：`pageNumSource`、`readingOrderIndex`、`contentKind`，有 confidence 时包含 `layoutConfidence` / `ocrConfidence`。
- Focused parser/index tests 与 adjacent parser tests 通过。
