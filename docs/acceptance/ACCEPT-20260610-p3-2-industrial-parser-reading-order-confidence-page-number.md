# ACCEPT-20260610 P3-2 子任务：industrial parser reading-order/confidence/page-number metadata

## Acceptance Summary

Status: ACCEPTED.

本 M 切片已完成 parser metadata contract foundation：parser section、OCR fallback result 与 index chunk metadata 具备承载 `pageNumSource`、`readingOrderIndex`、`contentKind`、`layoutConfidence`、`ocrConfidence` 的安全短字段能力。

该验收不表示 P3-2 工业级 PDF/DOCX layout/table/TOC、native/cloud OCR 或真实渲染页码能力已完成。

## Acceptance Criteria

| Criteria | Status | Evidence |
|---|---|---|
| RED 证明旧 parser contract 缺少 metadata 字段 | PASS | focused RED `testCompile` 失败，缺 `ParsedSection` metadata getters 与 `OcrFallbackResult` 4 参数构造器。 |
| `ParsedSection` 保留旧 5 参数构造器 | PASS | focused/adjacent/full backend 均编译并测试通过。 |
| `ParsedDocument` 自动填充 1-based `readingOrderIndex` | PASS | `DocumentParserServiceTest.parsedDocumentAddsStableReadingOrderAndSafeDefaultSectionMetadata` 覆盖。 |
| `OcrFallbackResult` 保留旧 3 参数构造器并支持 confidence | PASS | focused/adjacent/full backend 均通过；OCR fallback provider 旧调用兼容。 |
| OCR fallback success 写入 `OCR_FALLBACK / OCR_TEXT / ocrConfidence` | PASS | `RealParserProviderTest` 覆盖。 |
| `IndexService` chunk metadata 写入安全短字段 | PASS | `IndexServiceTest` 覆盖 `pageNumSource / readingOrderIndex / contentKind`。 |
| 不写 raw OCR/provider payload | PASS | `IndexServiceTest` 断言不包含 `rawOcrText / providerResponse`；实现也未写入 stderr/path/secret。 |
| 不改 API/DTO/DB/schema/dependency/frontend | PASS | 变更文件限制在 parser、IndexService、测试和文档；`backend/pom.xml`、migration、frontend 未改。 |
| 专家 subagent 复核完成 | PASS | `RUN-20260610-p3-2-industrial-parser-reading-order-confidence-page-number-review.md`。 |

## Verification

- RED: `mvn --% -Dtest=DocumentParserServiceTest,RealParserProviderTest,IndexServiceTest test` -> `testCompile` failure for missing metadata contract.
- Focused: `mvn --% -Dtest=DocumentParserServiceTest,RealParserProviderTest,IndexServiceTest test` -> `43 run, 0 failures, 0 errors, 0 skipped`.
- Adjacent: `mvn --% -Dtest=DocumentParserServiceTest,RealParserProviderTest,ProcessOcrFallbackProviderTest,ConfigurableOcrFallbackServiceTest,IndexServiceTest,IndexServiceParserFailureTest test` -> `55 run, 0 failures, 0 errors, 0 skipped`.
- Full backend first run: `mvn test` -> `579 run, 0 failures, 1 error, 1 skipped`; error isolated to MockMvc SSE async `ConcurrentModificationException`, unrelated to parser metadata.
- SSE isolated rerun: `mvn --% -Dtest=com.learningos.common.auth.SseProductionAuthStrategyTest$Production#tutorStreamInProductionDoesNotInferAdminFromBearerSubjectName test` -> `1 run, 0 failures, 0 errors, 0 skipped`.
- Full backend rerun: `mvn test` -> `579 run, 0 failures, 0 errors, 1 skipped`.

## Accepted Limitations / Follow-up

- P3-2 父项不关闭。
- 当前 reading order 是 section 顺序 metadata，不是复杂版面 reconstruction。
- 当前 confidence 是承载字段与 OCR fallback 传递，不是 native/cloud OCR quality pipeline。
- 真实 rendered page label、PDF/DOCX table/TOC/layout、native/cloud OCR provider 继续作为后续 P3-2 子任务推进。

