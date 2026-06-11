# Context Pack - P3-2-G Real PDF/DOCX Parser SDK Provider

## 1. 当前任务

执行 P3-2-G：在 P3-2-F provider boundary 后接入真实 Apache PDFBox / Apache POI DOCX provider。

## 2. 关联记忆

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`

## 3. 关联文档

- `docs/planning/backend-architecture-todolist.md`
- `docs/specs/SPEC-20260609-p3-2-f-parser-provider-boundary.md`
- `docs/skills/project-specific/rag-parser-boundary.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`

## 4. Selected Skills

- `feature-development-workflow`
- `educational-rag-pipeline`
- `rag-parser-boundary`
- `dependency-review`
- `security-review`
- `test-driven-development`
- `architecture-drift-check`
- `verification-before-completion`

## 5. Subagent Plan

| Expert | Output |
|---|---|
| Architect / Parfit | `docs/subagents/runs/RUN-20260609-p3-2-g-real-parser-sdk-provider-architecture.md` |
| Dependency / Security | `docs/subagents/runs/RUN-20260609-p3-2-g-real-parser-sdk-provider-dependency.md` |
| Test | `docs/subagents/runs/RUN-20260609-p3-2-g-real-parser-sdk-provider-test.md` |
| Integration | `docs/subagents/runs/RUN-20260609-p3-2-g-real-parser-sdk-provider-integration.md` |

并行级别：L1/L2 分析设计。实现模式：Main Codex 单任务 TDD。

## 6. 允许修改文件

- `backend/pom.xml`
- `backend/src/main/java/com/learningos/rag/parser/**`
- `backend/src/test/java/com/learningos/rag/parser/**`
- 本切片 workflow / subagent / security / evidence / acceptance / memory / changelog / retrospective / planning 文档

## 7. 禁止修改文件

- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- `backend/src/main/java/com/learningos/rag/application/IndexService.java`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
- `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java`
- `backend/src/main/java/com/learningos/rag/application/VectorIndexAdapter.java`

## 8. 测试命令

```powershell
cd backend
mvn --% -Dtest=DocumentParserServiceTest,RealParserProviderTest,NoopOcrFallbackServiceTest test
mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest test
mvn --% dependency:tree -Dincludes=org.apache.pdfbox:pdfbox,org.apache.poi:poi-ooxml
mvn test
```

## 9. 当前任务边界

可以声明：

- 真实 PDFBox/POI provider 最小接入完成。
- RAG parser SDK 接入前置能力完成。

不得声明：

- OCR 生产能力完成。
- 工业级 PDF 版面/目录/表格结构恢复完成。
- P3-2 全部复杂 PDF/DOCX/OCR 项完成。

