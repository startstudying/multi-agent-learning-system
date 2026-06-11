# RUN-20260608 后端 P3 生产化 RAG Parser Expert

## 角色

RAG Parser / Indexing Expert

## 当前事实

- `docs/planning/backend-architecture-todolist.md` 中 P3-2 的复杂 PDF/DOCX、OCR fallback、真实页码和章节层级识别仍是未完成项。
- 当前 `DocumentParserService` 是无依赖轻量 parser：
  - PDF 仅抽取简单 `Tj` / `TJ` 文本。
  - PDF 页码仍是 best-effort `1`。
  - DOCX 只读取 `word/document.xml`，并用 regex 识别段落、`Heading1-6`、page break。
  - OCR 未实现。
- 当前无依赖 hardening 明确排除了 PDFBox、POI、Tika、docx4j、iText、Tesseract、云 OCR。
- `IndexService` 已能消费 `ParsedSection.pageNum/headingPath` 并写入 chunk metadata。

## 建议

若坚持无新依赖，只能做 best-effort 页码/层级增强，不能诚实关闭完整 P3-2。

若要真实关闭 P3-2，需要后续依赖审查：

- PDF：优先评审 Apache PDFBox。
- DOCX：优先评审 Apache POI 或 docx4j。
- OCR：先定义 `OcrFallbackService`，真实 provider 单独审批。

## 当前总控决策

P3-2 暂不在当前实现切片修改；待 P3-4 安全前置和 P3-3 provider 边界完成后，再进入 parser/OCR dependency review。
