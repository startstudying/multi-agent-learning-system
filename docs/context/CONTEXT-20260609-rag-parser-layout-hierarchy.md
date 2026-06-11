# Context Pack - P3-2-E RAG Parser Layout / Page Hierarchy

## 当前任务

执行 P3-2-E：无新增依赖增强 RAG parser layout/page hierarchy，聚焦简单 PDF 多页 best-effort 页码和 DOCX `w:tab` / 非分页 `w:br` 文本分隔。

## 关联记忆

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`

## 关联文档

- PRD：`docs/product/PRD-20260609-rag-parser-layout-hierarchy.md`
- REQ：`docs/requirements/REQ-20260609-rag-parser-layout-hierarchy.md`
- SPEC：`docs/specs/SPEC-20260609-rag-parser-layout-hierarchy.md`
- PLAN：`docs/plans/PLAN-20260609-rag-parser-layout-hierarchy.md`
- TASK：`docs/tasks/TASK-20260609-rag-parser-layout-hierarchy.md`
- Parser skill：`docs/skills/project-specific/rag-parser-boundary.md`
- Architecture baseline：`docs/architecture/ARCHITECTURE_BASELINE.md`
- Drift checklist：`docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`

## Selected Skills

- `feature-development-workflow`
- `educational-rag-pipeline`
- `rag-parser-boundary`
- `test-driven-development`
- `security-review`
- `verification-before-completion`

## Subagent Plan

| 专家 | 报告 |
|---|---|
| Agent/RAG Expert | `docs/subagents/runs/RUN-20260609-rag-parser-layout-hierarchy-agent-rag.md` |
| Dependency/VectorDB Expert | `docs/subagents/runs/RUN-20260609-rag-parser-layout-hierarchy-vector-dependency.md` |
| Security & Quality | `docs/subagents/runs/RUN-20260609-rag-parser-layout-hierarchy-security.md` |
| Integration Reviewer | `docs/subagents/runs/RUN-20260609-rag-parser-layout-hierarchy-integration.md` |

并行级别：L1 并行分析。实现模式：Main Codex 串行 TDD。

## 允许修改文件

- `backend/src/main/java/com/learningos/rag/parser/DocumentParserService.java`
- `backend/src/test/java/com/learningos/rag/parser/DocumentParserServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`
- 本任务相关 workflow / subagent / evidence / acceptance / memory / changelog / retrospective / planning 文档。

## 禁止修改文件

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java`
- `backend/src/main/java/com/learningos/rag/application/VectorIndexAdapter.java`
- `backend/src/main/java/com/learningos/rag/application/ChunkService.java`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`

## 测试命令

```powershell
cd backend
mvn --% -Dtest=DocumentParserServiceTest test
mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest test
mvn --% -Dtest=DocumentParserServiceTest,IndexServiceTest,IndexServiceParserFailureTest,RagQueryServiceTest test
mvn test
```

## 当前任务边界

本次只完成无新增依赖 parser layout/page hierarchy 最小增强。OCR、真实 parser SDK、真实 VectorDB、正式 RBAC/Spring Security 不在本任务范围。

## 完成状态

- 状态：完成。
- 完成日期：2026-06-09。
- 代码实现：`DocumentParserService` 已完成 PDF simple page marker best-effort 分段、DOCX 同段 page break 分段、DOCX tab / non-page break 分隔。
- 验证：focused、adjacent、full backend Maven 测试均通过；详见 `docs/evidence/EVIDENCE-20260609-rag-parser-layout-hierarchy.md`。
- 后续：复杂 PDF/DOCX SDK、OCR fallback、真实 VectorDB 和 P3-4 权限矩阵仍保留为独立任务。
