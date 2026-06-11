# TASK - P3-2-F Parser Provider Boundary + OCR Fallback Contract

## 1. 追踪

- PLAN：`docs/plans/PLAN-20260609-p3-2-f-parser-provider-boundary.md`
- SPEC：`docs/specs/SPEC-20260609-p3-2-f-parser-provider-boundary.md`
- Evidence：`docs/evidence/EVIDENCE-20260609-p3-2-f-parser-provider-boundary.md`
- Acceptance：`docs/acceptance/ACCEPT-20260609-p3-2-f-parser-provider-boundary.md`
- 任务编号：TASK-20260609-p3-2-f-parser-provider-boundary

## 2. 目标

在不新增依赖、schema、API、frontend 的前提下，把 RAG parser 从单体 `DocumentParserService` 重构为 provider boundary，并新增 disabled/noop OCR fallback contract。

## 3. 允许修改文件

- `backend/src/main/java/com/learningos/rag/parser/DocumentParserService.java`
- `backend/src/main/java/com/learningos/rag/parser/ParseInput.java`
- `backend/src/main/java/com/learningos/rag/parser/DocumentFormatParser.java`
- `backend/src/main/java/com/learningos/rag/parser/OcrFallbackService.java`
- `backend/src/main/java/com/learningos/rag/parser/OcrFallbackResult.java`
- `backend/src/main/java/com/learningos/rag/parser/NoopOcrFallbackService.java`
- `backend/src/test/java/com/learningos/rag/parser/DocumentParserServiceTest.java`
- `backend/src/test/java/com/learningos/rag/parser/NoopOcrFallbackServiceTest.java`
- 本任务相关 workflow / subagent / security / evidence / acceptance / memory / changelog / retrospective / planning 文档。

## 4. 禁止修改文件

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java`
- `backend/src/main/java/com/learningos/rag/application/VectorIndexAdapter.java`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`

## 5. 实施步骤

1. 创建 workflow docs、dependency review、Context Pack。
2. RED：新增 provider registry、noop OCR、safe provider failure 测试。
3. GREEN：新增 contract/provider/noop OCR，并重构 `DocumentParserService`。
4. 验证 focused / adjacent / full tests。
5. 更新 Evidence / Acceptance / Changelog / Memory / TODO / Retro。

## 6. 完成标准

- [x] PRD/REQ/SPEC/PLAN/TASK/Context 已存在。
- [x] RED tests 已验证失败原因正确。
- [x] `ParseInput` 已存在且不携带 storage/path。
- [x] `DocumentFormatParser` 已存在。
- [x] `DocumentParserService` 使用 provider registry。
- [x] Markdown/TXT/PDF/DOCX 轻量 provider 已以内部默认 provider 形式拆分，外部 bean 可覆盖。
- [x] noop OCR contract 已存在且 disabled/no text。
- [x] image-only PDF 不产生 section。
- [x] provider failure 映射为 `DOCUMENT_PARSE_FAILED`。
- [x] 不新增 dependency/schema/API/frontend。
- [x] Evidence/Acceptance/Changelog/Memory/TODO/Retro 已更新。

## 7. 验证结果

- RED：`mvn --% -Dtest=DocumentParserServiceTest,NoopOcrFallbackServiceTest test` 初次失败于 `testCompile`，缺少 contract/provider/noop OCR，符合预期。
- Focused：`mvn --% -Dtest=DocumentParserServiceTest,NoopOcrFallbackServiceTest test`，`19 run, 0 failures, 0 errors`。
- Adjacent：`mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest test`，`15 run, 0 failures, 0 errors`。
- Extended adjacent：`mvn --% -Dtest=DocumentParserServiceTest,NoopOcrFallbackServiceTest,IndexServiceTest,IndexServiceParserFailureTest,RagQueryServiceTest test`，`49 run, 0 failures, 0 errors`。
- Full backend：`mvn test`，`371 run, 0 failures, 0 errors, 1 skipped`。

## 8. 状态

| 字段 | 值 |
|---|---|
| 状态 | 完成 |
| 负责人 | Main Codex |
| 开始日期 | 2026-06-09 |
| 完成日期 | 2026-06-09 |

## 9. 非目标确认

- 真实 PDFBox / POI parser 未接入。
- Tess4J/native OCR 或外部 OCR 服务未接入。
- 复杂 PDF/DOCX 工业级页码、目录、章节层级识别仍是后续 P3-2 任务。
