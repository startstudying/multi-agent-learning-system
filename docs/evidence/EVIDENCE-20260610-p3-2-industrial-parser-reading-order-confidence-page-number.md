# EVIDENCE-20260610 P3-2 子任务：industrial parser reading-order/confidence/page-number metadata

## 1. 任务

P3-2 子任务：industrial parser reading-order/confidence/page-number metadata

## 2. 变更摘要

完成工业级 parser 后续增强的 metadata 承载层第一切片：

- `ParsedSection` 增加 `pageNumSource`、`readingOrderIndex`、`layoutConfidence`、`ocrConfidence`、`contentKind`。
- `ParsedSection` 保留旧 5 参数构造器。
- `ParsedDocument` 按 section 顺序自动补齐稳定 1-based `readingOrderIndex`。
- `OcrFallbackResult` 增加可选 `confidence`，并保留旧 3 参数构造器。
- OCR fallback 成功时，section metadata 使用 `pageNumSource=OCR_FALLBACK`、`contentKind=OCR_TEXT`，并传递 `ocrConfidence`。
- `ConfigurableOcrFallbackService` sanitize 后保留合法 confidence。
- `IndexService` chunk `metadataJson` 写入安全短字段：`pageNumSource`、`readingOrderIndex`、`contentKind`，并仅在非空时写入 `layoutConfidence` / `ocrConfidence`。

本切片没有实现工业级 layout/table/TOC、native/cloud OCR 或真实渲染页码映射。

## 3. 修改文件

生产代码：

- `backend/src/main/java/com/learningos/rag/parser/ParsedSection.java`
- `backend/src/main/java/com/learningos/rag/parser/ParsedDocument.java`
- `backend/src/main/java/com/learningos/rag/parser/OcrFallbackResult.java`
- `backend/src/main/java/com/learningos/rag/parser/DocumentParserService.java`
- `backend/src/main/java/com/learningos/rag/parser/PdfBoxDocumentFormatParser.java`
- `backend/src/main/java/com/learningos/rag/parser/ConfigurableOcrFallbackService.java`
- `backend/src/main/java/com/learningos/rag/application/IndexService.java`

测试：

- `backend/src/test/java/com/learningos/rag/parser/DocumentParserServiceTest.java`
- `backend/src/test/java/com/learningos/rag/parser/RealParserProviderTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`

文档：

- `docs/requirements/REQ-20260610-p3-2-industrial-parser-reading-order-confidence-page-number.md`
- `docs/specs/SPEC-20260610-p3-2-industrial-parser-reading-order-confidence-page-number.md`
- `docs/plans/PLAN-20260610-p3-2-industrial-parser-reading-order-confidence-page-number.md`
- `docs/tasks/TASK-20260610-p3-2-industrial-parser-reading-order-confidence-page-number.md`
- `docs/context/CONTEXT-20260610-p3-2-industrial-parser-reading-order-confidence-page-number.md`
- `docs/subagents/runs/RUN-20260610-p3-2-industrial-parser-reading-order-confidence-page-number-review.md`
- `docs/evidence/EVIDENCE-20260610-p3-2-industrial-parser-reading-order-confidence-page-number.md`
- `docs/acceptance/ACCEPT-20260610-p3-2-industrial-parser-reading-order-confidence-page-number.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 4. Verification

### RED

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentParserServiceTest,RealParserProviderTest,IndexServiceTest test
```

结果：

- `testCompile` 失败。
- 失败证明旧 contract 缺少：
  - `ParsedSection.readingOrderIndex()`
  - `ParsedSection.pageNumSource()`
  - `ParsedSection.contentKind()`
  - `ParsedSection.layoutConfidence()`
  - `ParsedSection.ocrConfidence()`
  - `OcrFallbackResult` 4 参数构造器

### Focused GREEN

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentParserServiceTest,RealParserProviderTest,IndexServiceTest test
```

最新结果：

- `Tests run: 43, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

### Adjacent

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentParserServiceTest,RealParserProviderTest,ProcessOcrFallbackProviderTest,ConfigurableOcrFallbackServiceTest,IndexServiceTest,IndexServiceParserFailureTest test
```

结果：

- `Tests run: 55, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

### Full backend 第一次

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

- `Tests run: 579, Failures: 0, Errors: 1, Skipped: 1`
- 失败测试：`SseProductionAuthStrategyTest.tutorStreamInProductionDoesNotInferAdminFromBearerSubjectName`
- 失败栈：`MockHttpServletResponse` 在异步 SSE 写 header 时出现 `ConcurrentModificationException`，触发 `ResponseBodyEmitter Failed to send`。
- 归因：失败发生在 `TutorController` / MockMvc SSE async 测试路径，不在本切片 parser metadata、OCR fallback、IndexService metadata 修改链路内。

### SSE isolated 复现

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=com.learningos.common.auth.SseProductionAuthStrategyTest$Production#tutorStreamInProductionDoesNotInferAdminFromBearerSubjectName test
```

结果：

- `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

### Full backend 重跑

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

- `Tests run: 579, Failures: 0, Errors: 0, Skipped: 1`
- `BUILD SUCCESS`

## 5. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | parser contract 在 `rag/parser`；`IndexService` 只消费 `ParsedSection` metadata 并写 chunk metadata。 |
| Frontend rules | PASS | 未改前端。 |
| Agent / RAG rules | PASS | 未改 RAG query/retrieval/citation API；仅增强 indexing metadata。 |
| Security | PASS | chunk metadata 只写短字段；不写 raw OCR text、provider response、stderr、路径、secret。 |
| API / Database | PASS | 未改 HTTP API、DTO、DB schema 或 migration。 |
| Dependencies | PASS | 未新增 Maven 依赖。 |

## 6. Acceptance

| Criteria | Verdict |
|---|---|
| RED tests prove old parser sections lack required metadata / metadata is not preserved into chunks | PASS |
| `ParsedSection` keeps old 5-arg constructor compatibility | PASS |
| `ParsedDocument` fills stable 1-based `readingOrderIndex` | PASS |
| `OcrFallbackResult` keeps old 3-arg constructor compatibility and supports optional confidence | PASS |
| OCR fallback success creates `OCR_TEXT` section metadata with optional confidence | PASS |
| `IndexService` chunk metadata includes safe short parser fields | PASS |
| Chunk metadata does not include `rawOcrText` / `providerResponse` | PASS |
| No API/DB/dependency/frontend changes | PASS |
| Focused / adjacent / full backend tests pass | PASS |

最终结论：PASS。该 M 切片完成 parser metadata contract foundation。

## 7. Accepted Limitations / Follow-up

- P3-2 工业 parser 父项仍保持 open。
- 本切片不实现 PDF/DOCX layout/table/TOC 算法。
- 本切片不接入 native/cloud OCR。
- `pageNumSource=PARSER_INFERRED` 不等于真实渲染页码。
- `readingOrderIndex` 当前是 section 级 1-based 顺序，不是复杂版面 reading-order reconstruction。
- 后续建议拆分：
  - PDF layout/table/TOC reading-order provider。
  - DOCX table/TOC reading-order provider。
  - native OCR provider confidence。
  - cloud OCR provider confidence。
  - rendered page label mapping。

