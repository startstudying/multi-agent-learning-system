# Context Pack - P3-2-I Real OCR Provider

## 1. 当前任务

执行 P3-2-I：在现有 OCR fallback SPI 后新增 process-based real OCR provider。默认关闭，不新增 Maven OCR 依赖，不修改索引与查询合同。

## 2. 关联记忆

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`

## 3. 关联文档

- `docs/planning/backend-architecture-todolist.md`
- `docs/specs/SPEC-20260609-p3-2-f-parser-provider-boundary.md`
- `docs/specs/SPEC-20260609-p3-2-g-real-parser-sdk-provider.md`
- `docs/specs/SPEC-20260609-p3-2-h-configurable-ocr-fallback-provider.md`
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
| Architecture | `docs/subagents/runs/RUN-20260609-p3-2-i-real-ocr-provider-architecture.md` |
| Security / Dependency | `docs/subagents/runs/RUN-20260609-p3-2-i-real-ocr-provider-security.md` |

并行级别：L1 分析。实现模式：Main Codex 单任务 TDD。

## 6. 允许修改文件

- `backend/src/main/java/com/learningos/config/RagParserOcrProperties.java`
- `backend/src/main/java/com/learningos/rag/parser/ProcessOcrFallbackProvider.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-test.yml`
- `backend/src/test/java/com/learningos/rag/parser/ProcessOcrFallbackProviderTest.java`
- `backend/src/test/java/com/learningos/rag/parser/RealParserProviderTest.java`
- 本切片 workflow / security / evidence / acceptance / memory / changelog / retrospective / planning / skill 文档

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
mvn --% -Dtest=ProcessOcrFallbackProviderTest,ConfigurableOcrFallbackServiceTest,RealParserProviderTest test
mvn --% -Dtest=DocumentParserServiceTest,IndexServiceTest,IndexServiceParserFailureTest test
mvn --% dependency:tree -Dincludes=net.sourceforge.tess4j:tess4j,net.java.dev.jna:jna,org.bytedeco:*,com.aliyun:*,com.google.cloud:google-cloud-vision,software.amazon.awssdk:textract
mvn test
```

## 9. 当前任务边界

可以声明：

- process-based OCR fallback provider 接入完成。
- 默认关闭且不新增 Maven OCR dependency。
- image-only PDF 可通过显式配置的 process provider 产生 OCR section。

不得声明：

- OCR 工业级能力完成。
- 页级 OCR confidence / layout/table/TOC 完成。
- 真实 VectorDB 完成。
- P3-2 全部完成。
