# TASK - P3-2-G Real PDF/DOCX Parser SDK Provider

## 1. 追踪

- PLAN：`docs/plans/PLAN-20260609-p3-2-g-real-parser-sdk-provider.md`
- SPEC：`docs/specs/SPEC-20260609-p3-2-g-real-parser-sdk-provider.md`
- Context：`docs/context/CONTEXT-20260609-p3-2-g-real-parser-sdk-provider.md`
- 任务编号：`TASK-20260609-p3-2-g-real-parser-sdk-provider`

## 2. 目标

在 `rag/parser` 边界内新增真实 PDFBox / POI DOCX parser provider，使 Spring 上下文中的 `DocumentParserService` 使用真实 provider 覆盖默认 lightweight provider，同时保留无参构造的 lightweight 单元测试兼容能力。

## 3. 允许修改文件

- `backend/pom.xml`
- `backend/src/main/java/com/learningos/rag/parser/PdfBoxDocumentFormatParser.java`
- `backend/src/main/java/com/learningos/rag/parser/PoiDocxDocumentFormatParser.java`
- `backend/src/main/java/com/learningos/rag/parser/ParserTextSanitizer.java`
- `backend/src/main/java/com/learningos/rag/parser/ParserResourceLimits.java`
- `backend/src/test/java/com/learningos/rag/parser/RealParserProviderTest.java`
- 必要时小幅修改 `backend/src/test/java/com/learningos/rag/parser/DocumentParserServiceTest.java`
- 本切片相关 workflow / evidence / acceptance / memory / changelog / retrospective / planning 文档

## 4. 禁止修改文件

- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- `backend/src/main/java/com/learningos/rag/application/IndexService.java`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
- `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java`
- `backend/src/main/java/com/learningos/rag/application/VectorIndexAdapter.java`

## 5. 实施步骤

1. RED：新增 `RealParserProviderTest`，覆盖 PDFBox/POI 行为。
2. GREEN：新增 Maven 依赖。
3. GREEN：实现 `ParserTextSanitizer` 与 `ParserResourceLimits`。
4. GREEN：实现 `PdfBoxDocumentFormatParser`。
5. GREEN：实现 `PoiDocxDocumentFormatParser`。
6. 运行 focused/adjacent/dependency tree/full verification。
7. 更新 Evidence / Acceptance / Changelog / Memory / TODO / Retro。

## 6. Done Criteria

- [x] PRD/REQ/SPEC/PLAN/TASK/Context/Dependency Review 存在。
- [x] RED 已验证。
- [x] PDFBox provider 可提取真实多页 PDF 并保留页码。
- [x] POI provider 可提取真实 DOCX heading/page metadata。
- [x] 空 PDF 不 raw fallback。
- [x] 损坏/超限输入只暴露 `DOCUMENT_PARSE_FAILED`。
- [x] `IndexService` 未修改。
- [x] 不新增 DB/API/frontend 变更。
- [x] Focused / adjacent / full backend verification 完成。
- [x] Evidence/Acceptance/Changelog/Memory/TODO/Retro 已更新。
