# REQ - P3-2-G Real PDF/DOCX Parser SDK Provider

## 1. 功能需求

| ID | Requirement |
|---|---|
| R1 | 系统应新增 `PdfBoxDocumentFormatParser`，实现 `DocumentFormatParser` 并返回 `DocumentParser.PDF` |
| R2 | PDF provider 应使用 PDFBox 从 `ParseInput.bytes()` 提取文本 |
| R3 | PDF provider 应按页输出 `ParsedSection`，`pageNum` 从 1 开始 |
| R4 | PDF 无文本时不得 fallback 为 raw bytes；noop OCR 下返回空 sections |
| R5 | 系统应新增 `PoiDocxDocumentFormatParser`，实现 `DocumentFormatParser` 并返回 `DocumentParser.DOCX` |
| R6 | DOCX provider 应使用 Apache POI 读取 DOCX，并输出 `ParsedSection` |
| R7 | DOCX provider 应识别 Heading1-6，维护 `title/headingLevel/headingPath` |
| R8 | DOCX provider 应识别显式 page break，使后续正文 `pageNum` 递增 |
| R9 | DOCX provider 应将 tab 和普通 line break 作为文本分隔 |
| R10 | provider 解析失败、损坏、超限应经 service 映射为 `DOCUMENT_PARSE_FAILED` |

## 2. 非功能需求

| ID | Requirement |
|---|---|
| N1 | Provider 不得访问 repository/storage/index task/chunk persistence |
| N2 | Provider 不得接收或记录 storage bucket/key/path |
| N3 | 不新增 API、DB migration、frontend 变更 |
| N4 | 新增依赖必须记录 dependency review |
| N5 | 解析必须限制最大输入大小、页数、段落数和输出字符数 |
| N6 | 错误信息不得暴露 raw parser exception、文件路径、API key、原文片段 |

## 3. 验收需求

- Focused parser tests 通过。
- Adjacent index parser tests 通过。
- Dependency tree 可生成并记录。
- Full backend `mvn test` 通过或明确说明阻塞。

