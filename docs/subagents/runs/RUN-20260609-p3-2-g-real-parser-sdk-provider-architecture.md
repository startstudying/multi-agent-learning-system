# Subagent Run - P3-2-G Real Parser SDK Provider Architecture

## 1. 角色与范围

- 专家：Parfit / Architect（gpt-5.5）。
- 范围：P3-2-G 真实 PDF/DOCX parser SDK provider 设计。
- 模式：L1/L2 并行分析与设计；不直接改代码。

## 2. 结论

最小合规方案是在 `rag/parser` 边界内新增真实 parser provider：

- `PdfBoxDocumentFormatParser` 覆盖 `DocumentParser.PDF`。
- `PoiDocxDocumentFormatParser` 覆盖 `DocumentParser.DOCX`。
- 两者均实现现有 `DocumentFormatParser`，通过 Spring bean 注入后覆盖 `DocumentParserService` 的 lightweight 默认 provider。
- `IndexService` 继续只消费 `ParsedDocument/ParsedSection`，不新增格式解析逻辑。

## 3. 推荐切片范围

纳入：

- `backend/pom.xml` 新增 Apache PDFBox 与 Apache POI `poi-ooxml`。
- 新增 PDFBox provider：真实 PDF 文本提取、按页输出 `pageNum`、空文本走 noop OCR 后仍为空。
- 新增 POI DOCX provider：真实 DOCX 段落/Heading1-6/显式分页/制表与换行分隔。
- 新增 parser 资源限制：最大输入大小、最大页数、最大段落数、最大输出字符数。
- focused / adjacent / full backend verification。

不纳入：

- OCR 生产能力。
- Tika / docx4j / iText / Tess4J。
- API、DB、frontend、retrieval、citation、VectorDB 变更。

## 4. 主要风险

- 新增依赖扩大不可信文件解析攻击面。
- PDFBox 3.0.7 官方安全页列出 examples module path traversal 问题；本切片只使用 core `pdfbox` 文本提取，不引入 examples/tools，也不复制示例代码。
- POI 官方安全指南明确不可信 Office 文件可能触发高 CPU/内存/异常；本切片必须限制输入大小、段落数与输出长度，并保持广义异常安全映射。
- 任何底层异常都不得进入 task/document error message，只能映射到 `DOCUMENT_PARSE_FAILED`。

## 5. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | provider 保持在 `rag/parser` |
| Frontend rules | PASS | 不改 frontend |
| Agent / RAG rules | PASS | 不改 retrieval/citation/trace |
| Security | PASS WITH CONDITIONS | 新增依赖需 dependency review 与资源限制 |
| API / Database | PASS | 不改 API / schema |

