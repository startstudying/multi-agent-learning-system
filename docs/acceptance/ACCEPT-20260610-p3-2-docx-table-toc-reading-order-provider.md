# P3-2 子任务：DOCX table/TOC reading-order provider Acceptance

日期：2026-06-10

## Verdict

Accepted.

## Acceptance Criteria

| Criteria | Status | Evidence |
|---|---|---|
| DOCX table 按 body order 进入 section | PASS | `RealParserProviderTest` 与 `DocumentParserServiceTest` 新增回归通过 |
| TOC-like paragraph 被跳过 | PASS | focused GREEN 覆盖 POI provider 与 lightweight fallback |
| table section/chunk metadata 为 `contentKind=TABLE_TEXT` | PASS | `IndexServiceTest.processDocxTablePreservesReadingOrderAndTableContentKindMetadata` |
| `readingOrderIndex` 与 section/chunk 顺序一致 | PASS | focused GREEN 覆盖 1/2/3 顺序断言 |
| `pageNumSource=PARSER_INFERRED`，不声称真实渲染页码 | PASS | metadata 断言通过；文档明确限制 |
| metadata 不包含 raw XML、provider response、raw OCR、路径或 secret | PASS | `IndexServiceTest` 断言 metadata 不含 `providerResponse` / `rawOcrText` / raw table XML |
| 不新增依赖、不改 API/DB/frontend | PASS | 生产代码仅改 `rag/parser`；`pom.xml`、migration、frontend 未改 |
| full backend tests 通过 | PASS | `mvn test`: `582 run, 0 failures, 0 errors, 1 skipped` |

## Accepted Limitation

本切片只完成 DOCX body-order table text 与 TOC skip。P3-2 父项不关闭，剩余 PDF layout/table/TOC、native/cloud OCR、provider confidence、真实渲染页码与真实 VectorDB adapter 继续作为后续子任务。
