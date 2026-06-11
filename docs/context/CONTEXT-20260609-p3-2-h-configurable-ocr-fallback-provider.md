# Context Pack - P3-2-H Configurable OCR Fallback Provider

## 1. 当前任务

执行 P3-2-H：在已有 OCR fallback contract 后补齐 configurable provider boundary。默认关闭 OCR，不新增真实 OCR 依赖。

## 2. 关联记忆

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`

## 3. 关联文档

- `docs/planning/backend-architecture-todolist.md`
- `docs/specs/SPEC-20260609-p3-2-f-parser-provider-boundary.md`
- `docs/specs/SPEC-20260609-p3-2-g-real-parser-sdk-provider.md`
- `docs/skills/project-specific/rag-parser-boundary.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`

## 4. Selected Skills

- `feature-development-workflow`
- `educational-rag-pipeline`
- `rag-parser-boundary`
- `security-review`
- `dependency-review`
- `test-driven-development`
- `architecture-drift-check`
- `verification-before-completion`
- `Confidence Check`

## 5. Subagent Plan

| Expert | Output |
|---|---|
| Architecture / Helmholtz | `docs/subagents/runs/RUN-20260609-p3-2-h-configurable-ocr-fallback-provider-architecture.md` |
| Security / Peirce | `docs/subagents/runs/RUN-20260609-p3-2-h-configurable-ocr-fallback-provider-security.md` |

并行级别：L1 分析。实现模式：Main Codex 单任务 TDD。

## 6. 允许修改文件

- `backend/src/main/java/com/learningos/LearningOsApplication.java`
- `backend/src/main/java/com/learningos/config/RagParserOcrProperties.java`
- `backend/src/main/java/com/learningos/rag/parser/OcrFallbackProvider.java`
- `backend/src/main/java/com/learningos/rag/parser/ConfigurableOcrFallbackService.java`
- `backend/src/main/java/com/learningos/rag/parser/NoopOcrFallbackService.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-test.yml`
- `backend/src/test/java/com/learningos/rag/parser/ConfigurableOcrFallbackServiceTest.java`
- `backend/src/test/java/com/learningos/rag/parser/RealParserProviderTest.java`
- 本切片 workflow / security / evidence / acceptance / memory / changelog / retrospective / planning 文档

## 7. 禁止修改文件

- `backend/pom.xml`
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
mvn --% -Dtest=ConfigurableOcrFallbackServiceTest,NoopOcrFallbackServiceTest,RealParserProviderTest test
mvn --% -Dtest=DocumentParserServiceTest,IndexServiceTest,IndexServiceParserFailureTest test
mvn test
```

## 9. 当前任务边界

可以声明：

- OCR fallback provider configuration boundary 完成。
- 默认关闭与 provider unavailable/failure/success fake path 已验证。

不得声明：

- 真实 OCR 生产能力完成。
- Tess4J/native/cloud OCR 已接入。
- 工业级 PDF/DOCX layout/table/TOC 完成。
- P3-2 全部完成。

