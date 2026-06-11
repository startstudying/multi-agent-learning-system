# CONTEXT-20260610 P3-2 子任务：industrial parser reading-order/confidence/page-number metadata

## Related Memory and Docs

- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/skills/project-specific/rag-parser-boundary.md`
- `docs/subagents/runs/RUN-20260610-p3-2-industrial-parser-open-item-plan.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`

## Selected Skills

- `feature-development-workflow`
- `educational-rag-pipeline`
- `rag-parser-boundary`
- `test-driven-development`
- `security-review`
- `Confidence Check`

## Subagent Plan

- RAG/parser expert L1 analysis completed and recommends this first M slice.
- Implementation stays single-threaded because parser contract changes are shared.

## Files Allowed to Modify

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

## Files Not Allowed to Modify

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- RAG query DTO/API response files
- VectorDB adapter files
- Controller files

## Test Commands

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentParserServiceTest,RealParserProviderTest,IndexServiceTest test
```

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentParserServiceTest,RealParserProviderTest,ProcessOcrFallbackProviderTest,ConfigurableOcrFallbackServiceTest,IndexServiceTest,IndexServiceParserFailureTest test
```

```powershell
cd D:\多元agent\backend
mvn test
```

## Current Task Boundary

只扩 parser metadata contract 与 chunk metadata 白名单。不把本切片描述成工业级 OCR/layout/table/TOC 已完成。
