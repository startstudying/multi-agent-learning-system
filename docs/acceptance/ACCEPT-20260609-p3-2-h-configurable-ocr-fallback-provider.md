# Acceptance - P3-2-H Configurable OCR Fallback Provider

## 1. 验收结论

状态：Accepted。

P3-2-H 已完成可配置 OCR fallback provider boundary 的最小实现：OCR 默认关闭，启用但 provider 缺失时返回稳定 unavailable 状态，fake provider 可验证 image-only PDF fallback 成功路径，provider 异常会被安全收敛为固定失败码。

本切片没有新增 OCR/native/cloud 依赖，没有修改 `IndexService`、API、DB migration、frontend、retrieval/citation/VectorDB，也没有实现真实 OCR 生产能力。

## 2. 验收清单

| 项 | 结果 | 证据 |
|---|---|---|
| PRD/REQ/SPEC/PLAN/TASK/Context 存在 | PASS | `docs/*/20260609-p3-2-h-*` |
| Dependency review 存在 | PASS | `docs/security/DEPENDENCY-REVIEW-20260609-p3-2-h-configurable-ocr-fallback-provider.md` |
| Subagent 报告存在 | PASS | `docs/subagents/runs/RUN-20260609-p3-2-h-*` |
| RED 已验证 | PASS | `testCompile` 因缺少目标类失败 |
| `RagParserOcrProperties` 新增 | PASS | `backend/src/main/java/com/learningos/config/RagParserOcrProperties.java` |
| `OcrFallbackProvider` SPI 新增 | PASS | `backend/src/main/java/com/learningos/rag/parser/OcrFallbackProvider.java` |
| `ConfigurableOcrFallbackService` 新增 | PASS | `backend/src/main/java/com/learningos/rag/parser/ConfigurableOcrFallbackService.java` |
| 默认 disabled | PASS | `ConfigurableOcrFallbackServiceTest#disabledConfigurationReturnsOcrDisabledWithoutCallingProvider` |
| enabled + missing provider | PASS | `ConfigurableOcrFallbackServiceTest#enabledConfigurationWithoutMatchingProviderReturnsUnavailable` |
| matching fake provider success | PASS | `ConfigurableOcrFallbackServiceTest#enabledConfigurationDelegatesToMatchingProvider` |
| provider exception safe failure | PASS | `ConfigurableOcrFallbackServiceTest#providerExceptionReturnsSafeFailureWithoutRawMessage` |
| unsafe reasonCode normalization | PASS | `ConfigurableOcrFallbackServiceTest#unsafeProviderReasonCodeIsNormalized` |
| image-only PDF + fake OCR success | PASS | `RealParserProviderTest#pdfBoxProviderUsesConfiguredOcrFallbackTextForImageOnlyPdf` |
| image-only PDF + OCR failure stays empty | PASS | `RealParserProviderTest#pdfBoxProviderKeepsImageOnlyPdfEmptyWhenConfiguredOcrProviderFails` |
| 不新增 OCR 依赖 | PASS | dependency tree 对 Tess4J/JNA/云 OCR SDK 无匹配输出 |
| `IndexService` 未修改 | PASS | Context Pack 禁止修改；adjacent tests 通过 |
| focused verification | PASS | `15 run, 0 failures, 0 errors` |
| adjacent verification | PASS | `33 run, 0 failures, 0 errors` |
| full backend verification | PASS | `385 run, 0 failures, 0 errors, 1 skipped` |

## 3. 验证命令

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ConfigurableOcrFallbackServiceTest,NoopOcrFallbackServiceTest,RealParserProviderTest test
mvn --% -Dtest=DocumentParserServiceTest,IndexServiceTest,IndexServiceParserFailureTest test
mvn --% dependency:tree -Dincludes=net.sourceforge.tess4j:tess4j,net.java.dev.jna:jna,org.bytedeco:*,com.aliyun:*,com.google.cloud:google-cloud-vision,software.amazon.awssdk:textract
mvn --% -DskipTests compile
mvn test
```

结果：

- Focused：`15 run, 0 failures, 0 errors, 0 skipped`
- Adjacent：`33 run, 0 failures, 0 errors, 0 skipped`
- Dependency tree：无 Tess4J/JNA/Bytedeco/云 OCR SDK 匹配条目
- Compile：build success
- Full backend：`385 run, 0 failures, 0 errors, 1 skipped`

## 4. 明确非验收范围

以下内容仍未完成，不能因本切片被误标为完成：

- 真实 OCR fallback provider。
- Tess4J/Tesseract/native/cloud OCR 接入。
- OCR 页码、置信度、版面结构、表格、目录、阅读顺序恢复。
- real VectorDB client。
- P3-2 全部完成。
- P3-4 broader class/course 与 formal OAuth2/JWK/Spring Security。

## 5. 后续建议

继续 P3-2 时建议二选一：

1. 真实 OCR provider 切片：先做 dependency/security/runtime/privacy review，再接入具体 provider。
2. 工业级 PDF/DOCX layout/table/TOC 切片：先定义 layout metadata contract，再选择 parser strategy。

