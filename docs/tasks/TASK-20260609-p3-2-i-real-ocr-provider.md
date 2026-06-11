# TASK - P3-2-I Real OCR Provider

## 1. 追踪

- PLAN：`docs/plans/PLAN-20260609-p3-2-i-real-ocr-provider.md`
- SPEC：`docs/specs/SPEC-20260609-p3-2-i-real-ocr-provider.md`
- Context：`docs/context/CONTEXT-20260609-p3-2-i-real-ocr-provider.md`
- 任务编号：`TASK-20260609-p3-2-i-real-ocr-provider`

## 2. 目标

在 `rag/parser` boundary 内新增 process-based OCR provider，使 `learning-os.rag.parser.ocr.provider=process` 且 command 显式配置时，扫描型 PDF 可以通过外部命令输出文本进入 OCR fallback；默认关闭且不新增 Maven OCR 依赖。

## 3. 允许修改文件

- `backend/src/main/java/com/learningos/config/RagParserOcrProperties.java`
- `backend/src/main/java/com/learningos/rag/parser/ProcessOcrFallbackProvider.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-test.yml`
- `backend/src/test/java/com/learningos/rag/parser/ProcessOcrFallbackProviderTest.java`
- `backend/src/test/java/com/learningos/rag/parser/RealParserProviderTest.java`
- 本切片 workflow / security / evidence / acceptance / memory / changelog / retrospective / planning / skill 文档

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

1. RED：新增 `ProcessOcrFallbackProviderTest` 覆盖 command 缺失、成功、失败、超时、安全不泄露。
2. RED：新增/扩展 `RealParserProviderTest` 覆盖 image-only PDF + process provider 成功路径。
3. GREEN：扩展 `RagParserOcrProperties` 增加 process command/timeout/max-output 配置。
4. GREEN：新增 `ProcessOcrFallbackProvider implements OcrFallbackProvider`。
5. GREEN：更新 `application.yml` / `application-test.yml` 默认值。
6. 运行 focused / adjacent / dependency / full verification。
7. 更新 Evidence / Acceptance / Changelog / Memory / TODO / Retro / Skill。

## 6. Done Criteria

- [x] PRD/REQ/SPEC/PLAN/TASK/Context/Dependency/Subagent docs 存在。
- [x] RED 已验证。
- [x] 默认 disabled 不调用 process provider。
- [x] command missing 返回 `UNAVAILABLE / OCR_PROVIDER_UNAVAILABLE`。
- [x] command success 返回 OCR text。
- [x] command failure/timeout 返回 `FAILED / OCR_PROVIDER_FAILED`。
- [x] stderr/path/secret 不泄露。
- [x] image-only PDF + process provider success 生成 OCR section。
- [x] 不新增 Maven dependency、不修改 `pom.xml`。
- [x] `IndexService` 未修改。
- [x] Focused / adjacent / dependency / full backend verification 完成。
- [x] Evidence/Acceptance/Changelog/Memory/TODO/Retro/Skill 更新。

## 7. 验证摘要

- 初始 RED：`ProcessOcrFallbackProvider` 与 `RagParserOcrProperties.ProcessProperties` 缺失，`testCompile` 失败。
- 最终验证：focused `21/21`、adjacent `33/33`、dependency tree 无 OCR SDK 命中、compile success、full backend `392 run, 0 failures, 0 errors, 1 skipped`。
