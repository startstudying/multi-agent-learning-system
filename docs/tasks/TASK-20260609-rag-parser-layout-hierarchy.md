# TASK - P3-2-E RAG Parser Layout / Page Hierarchy

## 1. 追踪

- PLAN：`docs/plans/PLAN-20260609-rag-parser-layout-hierarchy.md`
- SPEC：`docs/specs/SPEC-20260609-rag-parser-layout-hierarchy.md`
- 任务编号：TASK-20260609-rag-parser-layout-hierarchy

## 2. 目标

在不新增依赖、schema、API、frontend 的前提下，为 RAG parser 补齐简单 PDF 多页 best-effort 页码和 DOCX 行内布局分隔。

## 3. 允许修改文件

- `backend/src/main/java/com/learningos/rag/parser/DocumentParserService.java`
- `backend/src/test/java/com/learningos/rag/parser/DocumentParserServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`
- `docs/product/PRD-20260609-rag-parser-layout-hierarchy.md`
- `docs/requirements/REQ-20260609-rag-parser-layout-hierarchy.md`
- `docs/specs/SPEC-20260609-rag-parser-layout-hierarchy.md`
- `docs/plans/PLAN-20260609-rag-parser-layout-hierarchy.md`
- `docs/tasks/TASK-20260609-rag-parser-layout-hierarchy.md`
- `docs/context/CONTEXT-20260609-rag-parser-layout-hierarchy.md`
- `docs/subagents/runs/RUN-20260609-rag-parser-layout-hierarchy-*.md`
- `docs/evidence/EVIDENCE-20260609-rag-parser-layout-hierarchy.md`
- `docs/acceptance/ACCEPT-20260609-rag-parser-layout-hierarchy.md`
- `docs/retrospectives/RETRO-20260609-rag-parser-layout-hierarchy.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 4. 禁止修改文件

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java`
- `backend/src/main/java/com/learningos/rag/application/VectorIndexAdapter.java`
- `backend/src/main/java/com/learningos/rag/application/ChunkService.java`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`

## 5. 实施步骤

1. 创建 workflow docs 与 Context Pack。
2. RED：新增 PDF simple multi-page、DOCX tab/line break、IndexService page metadata 测试。
3. GREEN：最小修改 `DocumentParserService`。
4. 运行 focused / adjacent / full tests。
5. 更新 Evidence / Acceptance / Changelog / Memory / TODO / Retro。

## 6. 测试命令

```powershell
cd backend
mvn --% -Dtest=DocumentParserServiceTest test
mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest test
mvn --% -Dtest=DocumentParserServiceTest,IndexServiceTest,IndexServiceParserFailureTest,RagQueryServiceTest test
mvn test
```

## 7. 完成标准

- [x] PRD/REQ/SPEC/PLAN/TASK/Context 已存在。
- [x] RED tests 已验证失败原因正确。
- [x] PDF simple multi-page 输出不同 pageNum。
- [x] PDF 无文本 raw fallback 不回退。
- [x] DOCX tab/line break 分隔通过。
- [x] DOCX page break 行为不回退。
- [x] IndexService page metadata 回归通过。
- [x] 不新增 dependency/schema/API/frontend。
- [x] Evidence/Acceptance/Changelog/Memory/TODO/Retro 已更新。

## 8. 状态

| 字段 | 值 |
|---|---|
| 状态 | 完成 |
| 负责人 | Main Codex |
| 开始日期 | 2026-06-09 |
| 完成日期 | 2026-06-09 |

## 9. 验证结果

- `mvn --% -Dtest=DocumentParserServiceTest test`：15 run, 0 failures, 0 errors。
- `mvn --% -Dtest=IndexServiceTest#processSimpleMultiPagePdfPreservesPageNumbersInChunks test`：1 run, 0 failures, 0 errors。
- `mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest test`：15 run, 0 failures, 0 errors。
- `mvn --% -Dtest=DocumentParserServiceTest,IndexServiceTest,IndexServiceParserFailureTest,RagQueryServiceTest test`：45 run, 0 failures, 0 errors。
- `mvn test`：361 run, 0 failures, 0 errors, 1 skipped。
