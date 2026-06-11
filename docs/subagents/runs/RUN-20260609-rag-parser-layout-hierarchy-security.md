# P3-2-E RAG Parser Layout / Page Hierarchy - Security & Quality 摘要

## 1. 安全边界

- parser 输入仍按不可信文件处理。
- 新增能力不得恢复 PDF raw bytes fallback。
- DOCX 仍只读取 `word/document.xml`，不得解压非目标 entry body。
- parser failure 仍必须映射为 `DOCUMENT_PARSE_FAILED`，不得持久化 raw exception、路径、storage key、原文片段或 secret。

## 2. 不应做的事

- 不新增 PDFBox / POI / Tika / docx4j / Tesseract / OCR SDK。
- 不修改 `backend/pom.xml`。
- 不修改 DB migration、公开 API、frontend。
- 不把 best-effort 页码描述为真实页码。

## 3. 必测用例

- PDF 多页 best-effort 页码。
- PDF 无文本仍返回空 section。
- DOCX `w:tab` 与非分页 `w:br` 保留分隔。
- DOCX page break 行为不回退。
- IndexService 仍只写安全 metadata 与 safe error。

## 4. Dependency review

本切片无新增依赖，不需要 dependency review。OCR 或真实 parser SDK 后续必须单独审查。
