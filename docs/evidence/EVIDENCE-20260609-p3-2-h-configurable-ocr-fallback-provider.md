# Evidence - P3-2-H Configurable OCR Fallback Provider

## 1. 范围

本证据覆盖 P3-2-H：在已有 `OcrFallbackService` 合同后补齐可配置 OCR fallback provider boundary。

已完成：

- 新增 `learning-os.rag.parser.ocr.enabled` 与 `learning-os.rag.parser.ocr.provider` 配置。
- 新增 `RagParserOcrProperties`。
- 新增 `OcrFallbackProvider` SPI。
- 新增 `ConfigurableOcrFallbackService`，作为 Spring context 中的唯一默认 `OcrFallbackService`。
- `NoopOcrFallbackService` 保留直接实例化兼容，但不再作为 Spring `@Service` 避免未来多 bean 歧义。
- PDFBox image-only fallback 可通过 fake provider 验证 success path。
- provider exception 被收敛为 `FAILED / OCR_PROVIDER_FAILED / ""`，不透传 raw secret/path/text。

未完成、也不得声明完成：

- 真实 OCR SDK/native/cloud provider。
- Tess4J / Tesseract / JNA / OpenCV / cloud OCR 依赖。
- 工业级 PDF/DOCX layout/table/TOC/reading-order。
- real VectorDB client。
- API / DB / frontend / retrieval / citation 合同变更。

## 2. 关键变更证据

代码：

- `backend/src/main/java/com/learningos/config/RagParserOcrProperties.java`
- `backend/src/main/java/com/learningos/rag/parser/OcrFallbackProvider.java`
- `backend/src/main/java/com/learningos/rag/parser/ConfigurableOcrFallbackService.java`
- `backend/src/main/java/com/learningos/rag/parser/NoopOcrFallbackService.java`
- `backend/src/main/java/com/learningos/LearningOsApplication.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-test.yml`

测试：

- `backend/src/test/java/com/learningos/rag/parser/ConfigurableOcrFallbackServiceTest.java`
- `backend/src/test/java/com/learningos/rag/parser/RealParserProviderTest.java`
- `backend/src/test/java/com/learningos/rag/parser/NoopOcrFallbackServiceTest.java`
- `backend/src/test/java/com/learningos/rag/parser/DocumentParserServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceParserFailureTest.java`

文档：

- `docs/product/PRD-20260609-p3-2-h-configurable-ocr-fallback-provider.md`
- `docs/requirements/REQ-20260609-p3-2-h-configurable-ocr-fallback-provider.md`
- `docs/specs/SPEC-20260609-p3-2-h-configurable-ocr-fallback-provider.md`
- `docs/plans/PLAN-20260609-p3-2-h-configurable-ocr-fallback-provider.md`
- `docs/tasks/TASK-20260609-p3-2-h-configurable-ocr-fallback-provider.md`
- `docs/context/CONTEXT-20260609-p3-2-h-configurable-ocr-fallback-provider.md`
- `docs/security/DEPENDENCY-REVIEW-20260609-p3-2-h-configurable-ocr-fallback-provider.md`

Subagent 报告：

- `docs/subagents/runs/RUN-20260609-p3-2-h-configurable-ocr-fallback-provider-architecture.md`
- `docs/subagents/runs/RUN-20260609-p3-2-h-configurable-ocr-fallback-provider-security.md`

## 3. RED 证据

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ConfigurableOcrFallbackServiceTest,RealParserProviderTest test
```

结果：

- Exit code: `1`
- 失败阶段：`testCompile`
- 关键失败信号：
  - 缺少 `RagParserOcrProperties`
  - 缺少 `ConfigurableOcrFallbackService`
  - 缺少 `OcrFallbackProvider`

结论：RED 失败原因符合预期，证明新增测试覆盖的是本切片尚未实现的配置边界行为。

## 4. Focused Verification

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ConfigurableOcrFallbackServiceTest,NoopOcrFallbackServiceTest,RealParserProviderTest test
```

结果：

- Exit code: `0`
- `ConfigurableOcrFallbackServiceTest`: `5 run, 0 failures, 0 errors`
- `NoopOcrFallbackServiceTest`: `1 run, 0 failures, 0 errors`
- `RealParserProviderTest`: `9 run, 0 failures, 0 errors`
- 合计：`15 run, 0 failures, 0 errors, 0 skipped`
- Finished at: `2026-06-09T11:13:47+08:00`

覆盖点：

- 默认 disabled 不调用 provider。
- enabled + missing provider 返回 `UNAVAILABLE / OCR_PROVIDER_UNAVAILABLE`。
- enabled + matching fake provider 成功返回 OCR 文本。
- provider exception 返回 `FAILED / OCR_PROVIDER_FAILED`，不泄漏 raw secret/path/text。
- unsafe reasonCode 被归一为固定安全码。
- image-only PDF 可使用 fake OCR 文本生成 section。
- image-only PDF 遇到 provider failure 保持空 sections。
- 超长 OCR text 仍映射为 safe parser failure。

## 5. Adjacent Verification

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentParserServiceTest,IndexServiceTest,IndexServiceParserFailureTest test
```

结果：

- Exit code: `0`
- 合计：`33 run, 0 failures, 0 errors, 0 skipped`
- Finished at: `2026-06-09T11:14:39+08:00`

覆盖点：

- `DocumentParserService` parser registry 仍兼容。
- `IndexService` 仍只消费 `ParsedDocument/ParsedSection`。
- parser failure 仍落到安全错误码。
- chunk/hash/index task 邻近路径未被 OCR 配置边界破坏。

## 6. Dependency Verification

命令：

```powershell
cd D:\多元agent\backend
mvn --% dependency:tree -Dincludes=net.sourceforge.tess4j:tess4j,net.java.dev.jna:jna,org.bytedeco:*,com.aliyun:*,com.google.cloud:google-cloud-vision,software.amazon.awssdk:textract
```

结果：

- Exit code: `0`
- 无匹配 OCR/native/cloud OCR dependency tree 条目。
- Finished at: `2026-06-09T11:15:23+08:00`

结论：本切片未引入 Tess4J/JNA/Bytedeco/云 OCR SDK 等目标 OCR 依赖。

## 7. Compile Verification

命令：

```powershell
cd D:\多元agent\backend
mvn --% -DskipTests compile
```

结果：

- Exit code: `0`
- Build success。
- Finished at: `2026-06-09T11:15:22+08:00`

## 8. Full Backend Verification

命令：

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

- Exit code: `0`
- 合计：`385 run, 0 failures, 0 errors, 1 skipped`
- Finished at: `2026-06-09T11:17:59+08:00`

备注：测试日志中仍有 Mockito dynamic agent 的 JDK 未来兼容 warning，属于既有测试工具链提示，不是本切片引入的失败。

## 9. Architecture Drift Check

| Check | Status | Evidence |
|---|---|---|
| Backend layering | PASS | OCR 配置与 provider selector 位于 backend config / `rag/parser` boundary |
| Frontend rules | PASS | 未修改 `frontend/**` |
| Agent / RAG rules | PASS | 未修改 retrieval/citation/trace/VectorDB 合同 |
| Security | PASS | 默认关闭、不新增依赖、provider error 固定码收敛 |
| API / Database | PASS | 未新增 API，未修改 DB migration |

## 10. 限制与风险

- 本切片只完成可配置 OCR fallback boundary，不提供真实 OCR 能力。
- 真实 OCR provider 后续必须单独做 dependency/security/runtime/privacy review。
- 当前 `OcrFallbackResult` 仍无页码、置信度、latency 等结构化元数据；如后续需要可观测性，应作为独立切片设计。
- 本地工作区根目录不是 git repository，无法用 `git status/diff` 作为变更范围证据；本证据以文件清单、源码读取和 Maven 输出为准。

