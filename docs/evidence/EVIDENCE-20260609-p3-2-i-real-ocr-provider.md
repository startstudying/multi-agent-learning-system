# Evidence - P3-2-I Real OCR Provider

## 1. 范围

本证据覆盖 P3-2-I：在现有 `OcrFallbackProvider` SPI 后新增 process-based real OCR provider。

已完成：

- 新增 `ProcessOcrFallbackProvider`，通过 `ProcessBuilder(List<String>)` 启动外部命令。
- `RagParserOcrProperties` 扩展 `process.command` / `timeout` / `max-output-chars`。
- `PdfBoxDocumentFormatParser` 的 image-only fallback 可通过 process provider 成功返回 OCR 文本。
- 默认关闭仍生效，且未新增 Maven OCR 依赖。
- Spring 3.5 下 record 配置绑定与 provider 注入已通过显式构造器注解稳定下来。

未完成、也不得声明完成：

- 真实 OCR SDK/native/cloud provider。
- OCR confidence、页级质量、layout/table/TOC/reading-order 恢复。
- real VectorDB client。
- API / DB / frontend / retrieval / citation 合同变更。

## 2. 关键变更证据

代码：

- `backend/src/main/java/com/learningos/config/RagParserOcrProperties.java`
- `backend/src/main/java/com/learningos/rag/parser/ProcessOcrFallbackProvider.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-test.yml`

测试：

- `backend/src/test/java/com/learningos/rag/parser/ProcessOcrFallbackProviderTest.java`
- `backend/src/test/java/com/learningos/rag/parser/RealParserProviderTest.java`
- `backend/src/test/java/com/learningos/rag/parser/ConfigurableOcrFallbackServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceParserFailureTest.java`

文档：

- `docs/product/PRD-20260609-p3-2-i-real-ocr-provider.md`
- `docs/requirements/REQ-20260609-p3-2-i-real-ocr-provider.md`
- `docs/specs/SPEC-20260609-p3-2-i-real-ocr-provider.md`
- `docs/plans/PLAN-20260609-p3-2-i-real-ocr-provider.md`
- `docs/tasks/TASK-20260609-p3-2-i-real-ocr-provider.md`
- `docs/context/CONTEXT-20260609-p3-2-i-real-ocr-provider.md`
- `docs/security/DEPENDENCY-REVIEW-20260609-p3-2-i-real-ocr-provider.md`

Subagent 报告：

- `docs/subagents/runs/RUN-20260609-p3-2-i-real-ocr-provider-architecture.md`
- `docs/subagents/runs/RUN-20260609-p3-2-i-real-ocr-provider-security.md`

## 3. RED 证据

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ProcessOcrFallbackProviderTest,RealParserProviderTest test
```

结果：

- Exit code: `1`
- 失败阶段：`testCompile`
- 关键失败信号：
  - 缺少 `ProcessOcrFallbackProvider`
  - 缺少 `RagParserOcrProperties.ProcessProperties`

结论：RED 失败符合预期，证明测试覆盖的是本切片尚未实现的 OCR provider boundary。

## 4. Focused Verification

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ProcessOcrFallbackProviderTest,ConfigurableOcrFallbackServiceTest,RealParserProviderTest test
```

结果：

- Exit code: `0`
- `ProcessOcrFallbackProviderTest`: `6 run, 0 failures, 0 errors`
- `ConfigurableOcrFallbackServiceTest`: `5 run, 0 failures, 0 errors`
- `RealParserProviderTest`: `10 run, 0 failures, 0 errors`
- 合计：`21 run, 0 failures, 0 errors, 0 skipped`

覆盖点：

- command 缺失返回 `UNAVAILABLE / OCR_PROVIDER_UNAVAILABLE`。
- command 成功返回 stdout OCR 文本。
- command 非零退出、超时、stdout 过长都返回安全失败。
- stderr / path / secret 不进入 result。
- image-only PDF + process provider 成功会生成 OCR section。

## 5. Adjacent Verification

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentParserServiceTest,IndexServiceTest,IndexServiceParserFailureTest test
```

结果：

- Exit code: `0`
- 合计：`33 run, 0 failures, 0 errors, 0 skipped`

覆盖点：

- `DocumentParserService` parser registry 仍兼容。
- `IndexService` 仍只消费 `ParsedDocument/ParsedSection`。
- parser failure 仍落到安全错误码。
- OCR 配置边界未破坏 chunk/hash/index task 邻近路径。

## 6. Dependency Verification

命令：

```powershell
cd D:\多元agent\backend
mvn --% dependency:tree -Dincludes=net.sourceforge.tess4j:tess4j,net.java.dev.jna:jna,org.bytedeco:*,com.aliyun:*,com.google.cloud:google-cloud-vision,software.amazon.awssdk:textract
```

结果：

- Exit code: `0`
- 无 OCR/native/cloud OCR dependency tree 命中项。

结论：本切片未引入 Tess4J/JNA/Bytedeco/云 OCR SDK。

## 7. Compile Verification

命令：

```powershell
cd D:\多元agent\backend
mvn --% -DskipTests compile
```

结果：

- Exit code: `0`
- Build success。

## 8. Full Backend Verification

命令：

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

- Exit code: `0`
- 合计：`392 run, 0 failures, 0 errors, 1 skipped`

补充验证：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AgentTraceControllerTest test
```

结果：

- Exit code: `0`
- `AgentTraceControllerTest`: `2 run, 0 failures, 0 errors`
- 证明 Spring 上下文在 process OCR provider 引入后仍可正常装配。

## 9. Architecture Drift Check

| Check | Status | Evidence |
|---|---|---|
| Backend layering | PASS | OCR 配置与 provider selector 位于 backend config / `rag/parser` boundary |
| Frontend rules | PASS | 未修改 `frontend/**` |
| Agent / RAG rules | PASS | 未修改 retrieval/citation/trace/VectorDB 合同 |
| Security | PASS | 默认关闭、不新增依赖、stdout-only OCR 文本、safe reasonCode |
| API / Database | PASS | 未新增 API，未修改 DB migration |

## 10. 限制与风险

- 本切片只完成 process-based OCR fallback boundary，不提供真实 OCR 引擎能力。
- 真实 OCR provider 后续仍需单独做 dependency / security / runtime / privacy review。
- OCR confidence、layout/table/TOC/reading-order 仍属于独立切片。
- 本地工作区根目录不是 git repository，变更证据以文件清单、源码读取和 Maven 输出为准。
