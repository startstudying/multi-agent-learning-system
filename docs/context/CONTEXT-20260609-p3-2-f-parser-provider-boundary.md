# Context Pack - P3-2-F Parser Provider Boundary + OCR Fallback Contract

## Completion Status

- Status: Done.
- Completed at: 2026-06-09.
- Implementation: `DocumentParserService` now dispatches through provider registry; `ParseInput`, `DocumentFormatParser`, `OcrFallbackService`, `OcrFallbackResult`, and `NoopOcrFallbackService` were added.
- Lightweight providers are internal defaults in `DocumentParserService`; external `DocumentFormatParser` beans can override or extend later.
- Verification: focused `19/19`, adjacent `15/15`, extended adjacent `49/49`, full backend `371 run, 0 failures, 0 errors, 1 skipped`.
- Boundary: real PDFBox/POI/Tess4J/OCR and industrial-grade PDF/DOCX page/section recognition remain future work.

## 当前任务

执行 P3-2-F：为 RAG parser 补齐 provider boundary 和 disabled/noop OCR fallback contract。

本任务只做后端 parser 内部边界重构，不做真实 OCR、真实 PDF/DOCX SDK、VectorDB、API、DB 或 frontend 变更。

## 关联记忆

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`

## 关联文档

- PRD：`docs/product/PRD-20260609-p3-2-f-parser-provider-boundary.md`
- REQ：`docs/requirements/REQ-20260609-p3-2-f-parser-provider-boundary.md`
- SPEC：`docs/specs/SPEC-20260609-p3-2-f-parser-provider-boundary.md`
- PLAN：`docs/plans/PLAN-20260609-p3-2-f-parser-provider-boundary.md`
- TASK：`docs/tasks/TASK-20260609-p3-2-f-parser-provider-boundary.md`
- Parser skill：`docs/skills/project-specific/rag-parser-boundary.md`
- Architecture baseline：`docs/architecture/ARCHITECTURE_BASELINE.md`
- Drift checklist：`docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`

## Selected Skills

- `feature-development-workflow`
- `educational-rag-pipeline`
- `rag-parser-boundary`
- `test-driven-development`
- `security-review`
- `dependency-review`
- `verification-before-completion`

## Subagent Plan

| 专家 | 报告 |
|---|---|
| Architecture | `docs/subagents/runs/RUN-20260609-p3-2-f-parser-provider-boundary-architecture.md` |
| Test | `docs/subagents/runs/RUN-20260609-p3-2-f-parser-provider-boundary-test.md` |
| Dependency | `docs/subagents/runs/RUN-20260609-p3-2-f-parser-provider-boundary-dependency.md` |
| Integration | `docs/subagents/runs/RUN-20260609-p3-2-f-parser-provider-boundary-integration.md` |

并行级别：L1 并行分析。实现模式：Main Codex 单任务串行 TDD。

## 允许修改文件

- `backend/src/main/java/com/learningos/rag/parser/DocumentParserService.java`
- `backend/src/main/java/com/learningos/rag/parser/ParseInput.java`
- `backend/src/main/java/com/learningos/rag/parser/DocumentFormatParser.java`
- `backend/src/main/java/com/learningos/rag/parser/*DocumentFormatParser.java`
- `backend/src/main/java/com/learningos/rag/parser/OcrFallbackService.java`
- `backend/src/main/java/com/learningos/rag/parser/OcrFallbackResult.java`
- `backend/src/main/java/com/learningos/rag/parser/NoopOcrFallbackService.java`
- `backend/src/test/java/com/learningos/rag/parser/DocumentParserServiceTest.java`
- `backend/src/test/java/com/learningos/rag/parser/NoopOcrFallbackServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceParserFailureTest.java`
- 本任务相关 workflow / subagent / security / evidence / acceptance / memory / changelog / retrospective / planning 文档。

## 禁止修改文件

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java`
- `backend/src/main/java/com/learningos/rag/application/VectorIndexAdapter.java`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`

## 测试命令

```powershell
cd backend
mvn --% -Dtest=DocumentParserServiceTest,NoopOcrFallbackServiceTest test
mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest test
mvn --% -Dtest=DocumentParserServiceTest,NoopOcrFallbackServiceTest,IndexServiceTest,IndexServiceParserFailureTest,RagQueryServiceTest test
mvn test
```

## 当前任务边界

只允许声明：

- parser provider boundary 完成。
- OCR fallback contract/noop 完成。
- 既有轻量 parser 行为未回退。

不得声明：

- 复杂 PDF/DOCX 工业级解析完成。
- OCR 生产能力完成。
- PDFBox/POI/Tess4J 已接入。

## 架构漂移检查

实施前：

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | parser 边界仍在 `rag/parser` |
| Frontend rules | PASS | 不改 frontend |
| Agent / RAG rules | PASS | 不改 retrieval/citation/trace |
| Security | PASS | 不新增依赖，不恢复 raw fallback |
| API / Database | PASS | 不改 API / schema |
