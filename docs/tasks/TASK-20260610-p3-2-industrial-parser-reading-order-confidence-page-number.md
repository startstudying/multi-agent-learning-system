# TASK-20260610 P3-2 子任务：industrial parser reading-order/confidence/page-number metadata

## 目标

为后续工业级 PDF/DOCX layout/table/TOC、native/cloud OCR、真实页码映射提供安全 parser metadata 承载层。本切片只扩展 parser contract 与 chunk metadata，不接新 provider。

## Scope

### In Scope

- `ParsedSection` 增加：
  - `pageNumSource`
  - `readingOrderIndex`
  - `layoutConfidence`
  - `ocrConfidence`
  - `contentKind`
- `ParsedDocument` 自动填充 1-based `readingOrderIndex`。
- `OcrFallbackResult` 增加可选 `confidence`。
- OCR fallback 成功时 section metadata 使用 `contentKind=OCR_TEXT` / `pageNumSource=OCR_FALLBACK`。
- `IndexService` 把短字段写入 chunk `metadataJson`。
- Parser / Index focused tests。

### Out of Scope

- native/cloud OCR。
- PDF/DOCX table/TOC/layout 算法。
- 新 Maven 依赖。
- HTTP API/DTO/DB schema 变更。
- VectorDB / retrieval / citation response 变更。

## Allowed Files

- `backend/src/main/java/com/learningos/rag/parser/ParsedSection.java`
- `backend/src/main/java/com/learningos/rag/parser/ParsedDocument.java`
- `backend/src/main/java/com/learningos/rag/parser/OcrFallbackResult.java`
- `backend/src/main/java/com/learningos/rag/parser/DocumentParserService.java`
- `backend/src/main/java/com/learningos/rag/parser/PdfBoxDocumentFormatParser.java`
- `backend/src/main/java/com/learningos/rag/parser/ConfigurableOcrFallbackService.java`
- `backend/src/main/java/com/learningos/rag/application/IndexService.java`
- `backend/src/test/java/com/learningos/rag/parser/DocumentParserServiceTest.java`
- `backend/src/test/java/com/learningos/rag/parser/RealParserProviderTest.java`
- `backend/src/test/java/com/learningos/rag/parser/ConfigurableOcrFallbackServiceTest.java`
- `backend/src/test/java/com/learningos/rag/parser/ProcessOcrFallbackProviderTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`
- 本任务相关 docs。

## Disallowed Files

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- RAG query DTO/API response files
- VectorDB adapter files
- Object storage / controller files

## Test Commands

Focused:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentParserServiceTest,RealParserProviderTest,IndexServiceTest test
```

Adjacent:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentParserServiceTest,RealParserProviderTest,ProcessOcrFallbackProviderTest,ConfigurableOcrFallbackServiceTest,IndexServiceTest,IndexServiceParserFailureTest test
```

Full:

```powershell
cd D:\多元agent\backend
mvn test
```

## Acceptance Criteria

- [x] RED tests prove current parser sections lack required metadata / metadata is not preserved into chunks.
- [x] `ParsedSection` keeps old 5-arg constructor compatibility.
- [x] `ParsedDocument` fills stable 1-based `readingOrderIndex`.
- [x] `OcrFallbackResult` keeps old 3-arg constructor compatibility and supports optional confidence.
- [x] OCR fallback success creates `OCR_TEXT` section metadata with optional confidence.
- [x] `IndexService` chunk metadata includes safe short parser fields.
- [x] No API/DB/dependency/frontend changes.
- [x] Focused / adjacent / full backend tests pass or limitations are documented.
