# SPEC - P3-2-G Real PDF/DOCX Parser SDK Provider

## 1. 概述

本规格定义 P3-2-G：在 P3-2-F `DocumentFormatParser` provider boundary 后接入真实 PDFBox / POI DOCX provider。目标是在不修改索引编排层的前提下提升 PDF/DOCX 真实文档解析能力。

## 2. 领域对象

沿用：

- `DocumentFormatParser`
- `ParseInput`
- `ParsedDocument`
- `ParsedSection`
- `DocumentParseException`
- `OcrFallbackService`

新增：

- `PdfBoxDocumentFormatParser`
- `PoiDocxDocumentFormatParser`
- `ParserTextSanitizer`
- `ParserResourceLimits`

## 3. PDFBox provider

类：`PdfBoxDocumentFormatParser`

职责：

- `format()` 返回 `DocumentParser.PDF`。
- 从 `ParseInput.bytes()` 加载 PDF。
- 限制输入大小、最大页数和最大输出字符数。
- 使用 PDFBox 按页提取文本。
- 每个非空页输出一个 `ParsedSection(null, null, List.of(), content, pageNum)`。
- 文本为空时调用 `OcrFallbackService`；noop 状态下返回空 sections。

约束：

- 不解析 embedded files / attachments。
- 不输出 raw PDF bytes。
- 不记录底层 exception message。

## 4. POI DOCX provider

类：`PoiDocxDocumentFormatParser`

职责：

- `format()` 返回 `DocumentParser.DOCX`。
- 从 `ParseInput.bytes()` 构造 `XWPFDocument`。
- 限制输入大小、最大段落数和最大输出字符数。
- 识别 `Heading1` 到 `Heading6`，更新当前 heading 栈。
- heading 段落本身不生成空 section。
- 普通正文段落生成 `ParsedSection(currentTitle, currentHeadingLevel, currentHeadingPath, content, pageNum)`。
- 显式 page break 增加 `pageNum`；tab / 普通 line break 作为空格分隔。

约束：

- 不支持 `.doc` / `.docm`。
- 不引入 Tika 自动格式识别。
- 不访问 storage/repository/index task。

## 5. 资源限制

`ParserResourceLimits` 提供保守默认值：

| Limit | Value |
|---|---:|
| max input bytes | 20 MiB |
| max PDF pages | 500 |
| max DOCX paragraphs | 5000 |
| max extracted characters | 2,000,000 |

超限行为：抛出 `IllegalArgumentException`，由 `DocumentParserService` 映射为 `DOCUMENT_PARSE_FAILED`。

## 6. 依赖

新增 Maven 依赖：

- `org.apache.pdfbox:pdfbox:3.0.7`
- `org.apache.poi:poi-ooxml:5.5.1`

完整审查见：

- `docs/security/DEPENDENCY-REVIEW-20260609-p3-2-g-real-parser-sdk-provider.md`

## 7. API / DB / Frontend

- 不修改公开 API。
- 不新增或修改 DB migration。
- 不修改 frontend。
- 不修改 retrieval/citation/vector/embedding 合同。

## 8. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | parser SDK 位于 `rag/parser` |
| Frontend rules | PASS | 不改 frontend |
| Agent / RAG rules | PASS | 不改 retrieval/citation/trace |
| Security | PASS WITH CONDITIONS | 新增依赖已记录；provider 有资源限制 |
| API / Database | PASS | 不改 API/schema |

## 9. 测试策略

```powershell
cd backend
mvn --% -Dtest=DocumentParserServiceTest,RealParserProviderTest,NoopOcrFallbackServiceTest test
mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest test
mvn --% dependency:tree -Dincludes=org.apache.pdfbox:pdfbox,org.apache.poi:poi-ooxml
mvn test
```

## 10. 非目标

- OCR 生产能力。
- 工业级 PDF 版面重建、目录识别、表格结构恢复。
- 所有 DOCX 样式体系完整还原。
- PDFBox/POI 之外的 parser SDK。

