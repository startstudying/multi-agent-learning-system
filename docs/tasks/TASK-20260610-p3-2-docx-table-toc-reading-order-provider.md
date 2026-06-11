# P3-2 子任务：DOCX table/TOC reading-order provider

## Goal

让 DOCX parser 能按 body order 输出 paragraph 与 table，并跳过 TOC-like paragraph，确保 table chunk 以 `TABLE_TEXT` metadata 进入索引。

## Scope

包含：

- POI DOCX provider body order 增强。
- lightweight DOCX fallback table/TOC 顺序增强。
- table section `contentKind=TABLE_TEXT`。
- focused / adjacent / full backend 测试。
- Evidence / Acceptance / Changelog / Memory / TODO 更新。

不包含：

- 真实渲染页码。
- native/cloud OCR。
- PDF table/layout provider。
- VectorDB adapter。
- API/DB/frontend/dependency 变更。

## Context Pack

Standalone context pack:

`docs/context/CONTEXT-20260610-p3-2-docx-table-toc-reading-order-provider.md`

## Allowed Files

生产代码：

- `backend/src/main/java/com/learningos/rag/parser/PoiDocxDocumentFormatParser.java`
- `backend/src/main/java/com/learningos/rag/parser/DocumentParserService.java`

测试：

- `backend/src/test/java/com/learningos/rag/parser/RealParserProviderTest.java`
- `backend/src/test/java/com/learningos/rag/parser/DocumentParserServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`

文档：

- `docs/subagents/runs/RUN-20260610-p3-2-docx-table-toc-reading-order-provider-review.md`
- `docs/requirements/REQ-20260610-p3-2-docx-table-toc-reading-order-provider.md`
- `docs/specs/SPEC-20260610-p3-2-docx-table-toc-reading-order-provider.md`
- `docs/plans/PLAN-20260610-p3-2-docx-table-toc-reading-order-provider.md`
- `docs/tasks/TASK-20260610-p3-2-docx-table-toc-reading-order-provider.md`
- `docs/context/CONTEXT-20260610-p3-2-docx-table-toc-reading-order-provider.md`
- `docs/evidence/EVIDENCE-20260610-p3-2-docx-table-toc-reading-order-provider.md`
- `docs/acceptance/ACCEPT-20260610-p3-2-docx-table-toc-reading-order-provider.md`
- `docs/retrospectives/RETRO-20260610-p3-2-docx-table-toc-reading-order-provider.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/skills/project-specific/rag-parser-boundary.md`

## Disallowed Files

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `backend/src/main/java/com/learningos/rag/api/**`
- `frontend/**`
- secret、env、credential 文件

## TDD Steps

- [x] RED: POI provider table/TOC/body order.
- [x] RED: lightweight DOCX XML table/TOC/body order.
- [x] RED: IndexService metadata propagation.
- [x] Run focused command and record expected failures.
- [x] Implement POI provider.
- [x] Implement lightweight fallback.
- [x] Run focused command and record GREEN.
- [x] Run adjacent command.
- [x] Run full backend command.
- [x] Create Evidence / Acceptance.
- [x] Update Changelog / Memory / TODO / skill.

## Test Commands

Focused：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=RealParserProviderTest,DocumentParserServiceTest,IndexServiceTest test
```

Adjacent：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentParserServiceTest,RealParserProviderTest,ProcessOcrFallbackProviderTest,ConfigurableOcrFallbackServiceTest,IndexServiceTest,IndexServiceParserFailureTest test
```

Full：

```powershell
cd D:\多元agent\backend
mvn test
```

## Verification Results

- RED focused: `46 run, 3 failures, 0 errors, 0 skipped`.
- Focused GREEN: `46 run, 0 failures, 0 errors, 0 skipped`.
- Adjacent: `58 run, 0 failures, 0 errors, 0 skipped`.
- Full backend: `582 run, 0 failures, 0 errors, 1 skipped`.

## Acceptance Criteria

- [x] DOCX table 按 body order 进入 section。
- [x] TOC-like paragraph 被跳过。
- [x] table section/chunk metadata 为 `contentKind=TABLE_TEXT`。
- [x] `readingOrderIndex` 与 section/chunk 顺序一致。
- [x] `pageNumSource=PARSER_INFERRED`，不声称真实渲染页码。
- [x] metadata 不包含 raw XML、provider response、raw OCR、路径或 secret。
- [x] full backend tests 通过。
