# TASK - P3-2-H Configurable OCR Fallback Provider

## 1. 追踪

- PLAN：`docs/plans/PLAN-20260609-p3-2-h-configurable-ocr-fallback-provider.md`
- SPEC：`docs/specs/SPEC-20260609-p3-2-h-configurable-ocr-fallback-provider.md`
- Context：`docs/context/CONTEXT-20260609-p3-2-h-configurable-ocr-fallback-provider.md`
- 任务编号：`TASK-20260609-p3-2-h-configurable-ocr-fallback-provider`

## 2. 目标

在 `rag/parser` boundary 内新增可配置 OCR fallback provider selector，使默认 OCR 显式关闭，启用但 provider 不存在时返回稳定 unavailable 状态，fake provider 可验证成功路径，provider 异常安全收敛。

## 3. 允许修改文件

- `backend/src/main/java/com/learningos/LearningOsApplication.java`
- `backend/src/main/java/com/learningos/config/RagParserOcrProperties.java`
- `backend/src/main/java/com/learningos/rag/parser/OcrFallbackProvider.java`
- `backend/src/main/java/com/learningos/rag/parser/ConfigurableOcrFallbackService.java`
- `backend/src/main/java/com/learningos/rag/parser/NoopOcrFallbackService.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-test.yml`
- `backend/src/test/java/com/learningos/rag/parser/ConfigurableOcrFallbackServiceTest.java`
- `backend/src/test/java/com/learningos/rag/parser/RealParserProviderTest.java`
- 本切片 workflow / evidence / acceptance / memory / changelog / retrospective / planning 文档

## 4. 禁止修改文件

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- `backend/src/main/java/com/learningos/rag/application/IndexService.java`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
- `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java`
- `backend/src/main/java/com/learningos/rag/application/VectorIndexAdapter.java`

## 5. 实施步骤

1. RED：新增 `ConfigurableOcrFallbackServiceTest` 和 PDF fallback success/failure tests。
2. GREEN：新增 `RagParserOcrProperties`。
3. GREEN：新增 `OcrFallbackProvider` SPI。
4. GREEN：新增 `ConfigurableOcrFallbackService`。
5. GREEN：调整 `NoopOcrFallbackService`，保留直接实例化 disabled 行为，避免未来 Spring bean 歧义。
6. GREEN：注册 `RagParserOcrProperties` 并更新 `application.yml` / `application-test.yml`。
7. 运行 focused / adjacent / full verification。
8. 更新 Evidence / Acceptance / Changelog / Memory / TODO / Retro。

## 6. Done Criteria

- [x] PRD/REQ/SPEC/PLAN/TASK/Context/Dependency/Subagent docs 存在。
- [x] RED 已验证。
- [x] 默认 disabled 返回 `DISABLED / OCR_DISABLED`。
- [x] enabled + missing provider 返回 `UNAVAILABLE / OCR_PROVIDER_UNAVAILABLE`。
- [x] fake provider success 可让 image-only PDF 生成 OCR section。
- [x] provider exception 返回 `FAILED / OCR_PROVIDER_FAILED` 且不泄漏 raw secret/path。
- [x] 不新增依赖、不修改 `pom.xml`。
- [x] `IndexService` 未修改。
- [x] Focused / adjacent / full backend verification 完成。
- [x] Evidence/Acceptance/Changelog/Memory/TODO/Retro 更新。
