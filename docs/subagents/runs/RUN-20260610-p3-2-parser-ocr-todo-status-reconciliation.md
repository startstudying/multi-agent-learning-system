# RUN-20260610 P3-2 parser/OCR TODO 状态对账专家审查

## Scope

只读审计 `docs/planning/backend-architecture-todolist.md` 中 P3-2 未勾选项“补齐复杂 PDF/DOCX、OCR fallback、真实页码和章节层级识别”是否已由 P3-2-E/F/G/H/I 现有切片部分或全部完成。

## Expert

| Agent | Role | Verdict |
|---|---|---|
| `019ead74-b751-7e31-931a-f22ab66f0878` | Agent/RAG Architect | CONDITIONAL |

## Findings

- P3-2-E/F/G/H/I 已完成最小生产化能力：
  - `DocumentParserService` provider registry 和 `OcrFallbackService` boundary。
  - PDFBox provider 按页提取 PDF 文本并保留 `pageNum`。
  - POI DOCX provider 提取 Heading1-6、page break、tab/line break 和 `headingPath`。
  - OCR fallback 支持 disabled-by-default 配置、provider selection、安全状态码。
  - Process-based OCR provider 可在显式 command 配置后通过 stdin/stdout 提供 image-only PDF OCR fallback。
  - `IndexService` 已把 `pageNum` / `headingPath` 写入 chunk 和 metadata。
- 未完成的工业级增强：
  - PDF layout/table/TOC/reading-order reconstruction。
  - DOCX full style/header/footer/footnote/textbox/table semantics。
  - Native/cloud OCR provider、OCR confidence、页级质量和 OCR metadata。
  - DOCX true rendered page numbers / complete rendered layout。

## Integration Decision

接受专家 `CONDITIONAL` 结论：

- 将 P3-2 TODO 勾选为“最小生产化能力已完成”。
- 新增独立未勾选后续项，避免把工业级文档智能解析误判为完成。
- 不修改后端代码、schema、API、dependency 或 frontend。
