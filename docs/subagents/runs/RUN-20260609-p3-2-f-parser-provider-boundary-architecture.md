# Subagent Report - P3-2-F Parser Provider Boundary Architecture

## 1. 结论

当前 `DocumentParserService -> IndexService` 大边界基本健康：`IndexService` 只调用 parser 输出并消费 `ParsedDocument/ParsedSection`，负责 chunk、hash、metadata 和任务状态；格式解析逻辑仍集中在 `rag/parser`。

本切片应只补齐“可替换 parser provider 边界 + OCR fallback contract + noop OCR”，不接入真实 PDFBox、POI、Tess4J、OCR，也不修改 schema、API 或 frontend。

## 2. 当前边界审查

结论：有条件健康。

证据：

- `IndexService` 通过构造函数注入 `DocumentParserService`，没有内嵌 PDF/DOCX/TXT/Markdown 解析逻辑。
- 索引执行只在解析阶段调用 `parserService.parse(document, bytes)`，随后消费 `ParsedDocument.parser()` 和 `ParsedSection` 做 chunk。
- chunk 持久化仍由 `IndexService` 负责 `pageNum`、`sectionTitle`、`chunkHash`、`metadataJson`。
- parser 安全错误已收敛到 `DocumentParseException.safeCode()`，索引任务不保存 raw exception。
- 当前不足是 `DocumentParserService` 内部仍用 `switch (parser)` 直接分发四种格式，未来接真实 SDK/OCR 会继续膨胀该类。

## 3. 最小可实施架构

### 3.1 `ParseInput`

新增 `backend/src/main/java/com/learningos/rag/parser/ParseInput.java`。

字段建议：

- `documentId`
- `name`
- `contentType`
- `sizeBytes`
- `bytes`

约束：

- 不携带 `storageBucket` / `storageKey`。
- 不携带原始上传路径或对象存储路径。
- `bytes` 对外只通过只读副本暴露。

### 3.2 `DocumentFormatParser`

新增 `backend/src/main/java/com/learningos/rag/parser/DocumentFormatParser.java`。

接口职责：

- `DocumentParser format()`
- `ParsedDocument parse(ParseInput input)`

`DocumentParserService` 改为：

- 选择格式。
- 根据 `DocumentParser` 找到 provider。
- 调用 provider。
- 统一异常映射为 `DOCUMENT_PARSE_FAILED`。

### 3.3 轻量 provider

拆分当前无依赖实现：

- `MarkdownDocumentFormatParser`
- `TextDocumentFormatParser`
- `PdfLightweightDocumentFormatParser`
- `DocxLightweightDocumentFormatParser`

行为必须等价保留：

- Markdown heading hierarchy。
- TXT single section / blank skip。
- PDF simple `Tj` / `TJ` extraction、simple page marker。
- DOCX `word/document.xml`、heading、page break、tab/non-page break。

### 3.4 OCR fallback contract

新增：

- `OcrFallbackService`
- `NoopOcrFallbackService`
- `OcrFallbackResult`

当前行为：

- noop OCR 固定返回 `DISABLED`。
- scanned/image-only PDF 不产生 section。
- 索引层继续将空解析结果处理为 `DOCUMENT_EMPTY_OR_UNAVAILABLE`。
- 禁止 raw PDF bytes fallback。

## 4. 禁止或延后项

本切片禁止：

- 不引入 PDFBox、Apache POI、Tess4J、Tika、docx4j、iText、真实 OCR 引擎或外部 OCR 服务。
- 不修改 `backend/pom.xml`。
- 不新增 DB migration。
- 不修改公开 API。
- 不修改 frontend。
- 不修改 RAG query、citation、retrieval、VectorDB 边界。
- 不从 GitHub 复制代码。

## 5. 允许修改文件建议

代码：

- `backend/src/main/java/com/learningos/rag/parser/DocumentParserService.java`
- `backend/src/main/java/com/learningos/rag/parser/ParseInput.java`
- `backend/src/main/java/com/learningos/rag/parser/DocumentFormatParser.java`
- `backend/src/main/java/com/learningos/rag/parser/*DocumentFormatParser.java`
- `backend/src/main/java/com/learningos/rag/parser/OcrFallbackService.java`
- `backend/src/main/java/com/learningos/rag/parser/OcrFallbackResult.java`
- `backend/src/main/java/com/learningos/rag/parser/NoopOcrFallbackService.java`

测试：

- `backend/src/test/java/com/learningos/rag/parser/DocumentParserServiceTest.java`
- `backend/src/test/java/com/learningos/rag/parser/NoopOcrFallbackServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceParserFailureTest.java`

## 6. 风险与验收标准

风险：

- provider 拆分若改变 parser selection，可能影响 TXT/PDF/DOCX metadata。
- OCR contract 若返回文本但状态不明确，容易被误认为真实 OCR 成功。
- provider 异常若绕过 `DocumentParseException.safeCode()`，会破坏安全错误映射。

验收标准：

- `IndexService` 仍只消费 `ParsedDocument/ParsedSection`，不新增格式解析代码。
- `DocumentParserService` 从 `switch` 实现转为 provider 聚合。
- noop OCR 对 scanned PDF 不产生 chunk。
- parser failure 仍只写 `DOCUMENT_PARSE_FAILED`。
- 不新增依赖、不改 API/DB/frontend。
- focused、adjacent、full backend Maven 验证通过。
