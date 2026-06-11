# Context Pack：P3-2 DOCX table/TOC reading-order provider

## Current TASK

`docs/tasks/TASK-20260610-p3-2-docx-table-toc-reading-order-provider.md`

## Related Memory and Docs

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/skills/project-specific/rag-parser-boundary.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/harness/TEST_COMMANDS.md`
- `docs/subagents/runs/RUN-20260610-p3-2-docx-table-toc-reading-order-provider-review.md`

## Selected Skills

- `feature-development-workflow`
- `ai-learning-agent-development`
- `educational-rag-pipeline`
- `rag-parser-boundary`
- `dispatching-parallel-agents`
- `test-driven-development`

## Subagent Plan

并行专家已完成只读报告：

- RAG/parser 架构专家：设计 body order、table text、TOC skip、metadata 策略。
- 测试/验收专家：设计 RED、focused/adjacent/full 验证矩阵。

实现由主 Codex 单线程完成。

## Files Allowed To Modify

生产代码：

- `backend/src/main/java/com/learningos/rag/parser/PoiDocxDocumentFormatParser.java`
- `backend/src/main/java/com/learningos/rag/parser/DocumentParserService.java`

测试：

- `backend/src/test/java/com/learningos/rag/parser/RealParserProviderTest.java`
- `backend/src/test/java/com/learningos/rag/parser/DocumentParserServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`

文档：

- `docs/evidence/EVIDENCE-20260610-p3-2-docx-table-toc-reading-order-provider.md`
- `docs/acceptance/ACCEPT-20260610-p3-2-docx-table-toc-reading-order-provider.md`
- `docs/retrospectives/RETRO-20260610-p3-2-docx-table-toc-reading-order-provider.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/skills/project-specific/rag-parser-boundary.md`

## Files Not Allowed To Modify

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `backend/src/main/java/com/learningos/rag/api/**`
- `frontend/**`
- secret、credential、env 文件

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

## Current Boundary

本任务只推进 DOCX body order + table text + TOC skip。P3-2 父项仍保留 open，不关闭 PDF layout/table/TOC、native/cloud OCR、provider confidence、真实渲染页码、真实 VectorDB。
