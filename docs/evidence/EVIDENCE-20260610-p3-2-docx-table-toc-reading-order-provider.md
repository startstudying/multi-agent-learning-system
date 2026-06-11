# P3-2 子任务：DOCX table/TOC reading-order provider Evidence

日期：2026-06-10

## 变更摘要

本切片完成 DOCX parser 的 body-order table/TOC 增强：

- `PoiDocxDocumentFormatParser` 从仅遍历 `XWPFDocument#getParagraphs()` 改为按 `getBodyElements()` 处理 `XWPFParagraph` 与 `XWPFTable`。
- `DocumentParserService` 的 lightweight DOCX fallback 从仅匹配 paragraph 扩展为按 XML body element 顺序处理 `<w:p>` 与 `<w:tbl>`。
- table 输出为独立 `ParsedSection`，内容格式为 `cell | cell; row | row`，`contentKind=TABLE_TEXT`。
- TOC-like paragraph 默认跳过，不进入 RAG chunk。
- `IndexService` 未改；继续通过现有 metadata path 写入 `contentKind` / `readingOrderIndex` / `pageNumSource`。

## RED 证据

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=RealParserProviderTest,DocumentParserServiceTest,IndexServiceTest test
```

结果：

```text
Tests run: 46, Failures: 3, Errors: 0, Skipped: 0
BUILD FAILURE
```

关键失败：

- `RealParserProviderTest.poiDocxProviderPreservesBodyOrderForTablesAndSkipsTocParagraphs`
  - 当前 POI provider 输出 `TOC Course RAG .... 1`，并丢失 table section。
- `DocumentParserServiceTest.docxParsesTablesInBodyOrderAndSkipsTocParagraphsBestEffort`
  - lightweight parser 输出 7 个 TEXT section，把 TOC 和每个 table cell 都当普通正文。
- `IndexServiceTest.processDocxTablePreservesReadingOrderAndTableContentKindMetadata`
  - chunk 内容包含 TOC，table cell 被拆散，缺少 `TABLE_TEXT` chunk。

## Focused GREEN

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=RealParserProviderTest,DocumentParserServiceTest,IndexServiceTest test
```

结果：

```text
Tests run: 46, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

覆盖：

- POI DOCX provider table body order。
- POI DOCX provider TOC skip。
- POI DOCX provider `contentKind=TABLE_TEXT`。
- lightweight XML DOCX table body order。
- lightweight XML DOCX TOC skip。
- IndexService chunk metadata propagation。

## Adjacent Verification

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentParserServiceTest,RealParserProviderTest,ProcessOcrFallbackProviderTest,ConfigurableOcrFallbackServiceTest,IndexServiceTest,IndexServiceParserFailureTest test
```

结果：

```text
Tests run: 58, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

覆盖：

- parser core
- real parser provider
- OCR fallback boundary
- indexing metadata
- parser failure handling

## Full Backend Verification

命令：

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

```text
Tests run: 582, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

## 架构漂移检查

| Check | Status | Evidence |
|---|---|---|
| Backend layering | PASS | 仅修改 `rag/parser` provider 与测试；`IndexService` 未新增格式解析逻辑 |
| Frontend rules | PASS | 未修改 frontend |
| Agent / RAG rules | PASS | parser boundary 内增强；RAG query/citation contract 未改 |
| Security | PASS | 未新增依赖；未写 raw XML/provider payload/path/secret |
| API / Database | PASS | 未修改 REST API、DTO、DB migration |

## 限制与剩余风险

- 本切片不是完整工业级 DOCX layout engine。
- 不处理多栏、复杂合并单元格、嵌套表格完整结构、页眉页脚、脚注尾注、图片 OCR。
- `pageNumSource=PARSER_INFERRED` 仍不是真实渲染页码。
- P3-2 父项仍 open：PDF layout/table/TOC、native/cloud OCR、provider confidence pipeline、真实渲染页码、真实 VectorDB adapter 仍待后续子任务。
