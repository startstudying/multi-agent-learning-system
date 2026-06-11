# P3-2-E RAG Parser Layout / Page Hierarchy - Agent/RAG 专家报告

## 1. 当前实现证据

- `DocumentParserService` 已集中负责 Markdown/TXT/PDF/DOCX parser 边界。
- `ParsedSection` 当前输出 `title`、`headingLevel`、`headingPath`、`content`、`pageNum`。
- `IndexService` 已把 `pageNum`、`sectionTitle`、`headingPath` 写入 chunk 与 metadata。
- `RagQueryService` 已把 chunk 的 `pageNum` / `sectionTitle` 写入 source citation 与 query log。

## 2. 缺口

- PDF 当前仅用 `Tj` / `TJ` 正则抽取文本，PDF `pageNum` 对所有抽取内容固定为 `1`。
- DOCX 当前识别 `Heading1`-`Heading6` 与 page break，但 `w:tab`、非分页 `w:br` 等行内布局会丢失分隔语义。
- 当前不能宣称真实 PDF 页码、复杂 DOCX、OCR 或工业级 parser 已完成。

## 3. 最小切片建议

本轮执行 P3-2-E：无新增依赖 parser layout/page hierarchy 最小增强。

- PDF：支持简单 page object / page marker 的 best-effort 分页抽取，不能识别时保持保守行为。
- DOCX：补 `w:tab` 和非 page `w:br` 的文本分隔，避免 `<w:t>` 拼接粘连。
- Metadata：保留 `pageNum` / `headingPath`，可在后续独立切片增加更明确的 parser confidence 字段。
- OCR / PDFBox / POI / Tika / docx4j / Tesseract 不纳入本轮。

## 4. 测试建议

- `DocumentParserServiceTest`：
  - 简单多页 PDF 可产生 page 1 / page 2 sections。
  - PDF 扫描页或无文本页不产生 raw fallback。
  - DOCX `w:tab` / 非分页 `w:br` 保留文本分隔。
  - DOCX page break 仍递增 pageNum。
- `IndexServiceTest`：
  - PDF pageNum 能进入 chunk。

## 5. 风险

- 无依赖 PDF parser 天然只是 best-effort，不能处理压缩 stream、CMap、字体编码、复杂 page tree。
- 错误页码会影响 citation 可信度；文档必须明确本轮只是 best-effort。
- OCR 必须单独 dependency/security review。
