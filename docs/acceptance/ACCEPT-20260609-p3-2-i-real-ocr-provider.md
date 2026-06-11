# Acceptance - P3-2-I Real OCR Provider

## 1. 验收结论

状态：Accepted。

P3-2-I 已完成 process-based real OCR provider boundary 的最小实现：OCR 默认关闭，`provider=process` 且 command 显式配置时可以把外部命令 stdout 作为 OCR 文本送回 parser，command 缺失 / 失败 / 超时都会稳定收敛为安全状态码。

本切片没有新增 OCR/native/cloud 依赖，没有修改 `IndexService`、API、DB migration、frontend、retrieval/citation/VectorDB，也没有实现工业级 OCR 能力。

## 2. 验收清单

| 项 | 结果 | 证据 |
|---|---|---|
| PRD/REQ/SPEC/PLAN/TASK/Context/Dependency 存在 | PASS | `docs/*/20260609-p3-2-i-*` |
| Subagent 报告存在 | PASS | `docs/subagents/runs/RUN-20260609-p3-2-i-*` |
| RED 已验证 | PASS | `testCompile` 因缺少目标类失败 |
| `RagParserOcrProperties` 扩展完成 | PASS | `backend/src/main/java/com/learningos/config/RagParserOcrProperties.java` |
| `ProcessOcrFallbackProvider` 新增 | PASS | `backend/src/main/java/com/learningos/rag/parser/ProcessOcrFallbackProvider.java` |
| 默认 disabled | PASS | `ConfigurableOcrFallbackServiceTest` |
| command missing | PASS | `ProcessOcrFallbackProviderTest#commandMissingReturnsUnavailable` |
| command success | PASS | `ProcessOcrFallbackProviderTest#commandSuccessReturnsStdoutText` |
| command failure / timeout | PASS | `ProcessOcrFallbackProviderTest#nonZeroExitReturnsSafeFailureWithoutStderrLeakage` / `#timeoutReturnsSafeFailure` |
| stderr / path / secret 不泄露 | PASS | `ProcessOcrFallbackProviderTest#nonZeroExitReturnsSafeFailureWithoutStderrLeakage` |
| image-only PDF + process provider success | PASS | `RealParserProviderTest#pdfBoxProviderUsesProcessOcrProviderForImageOnlyPdf` |
| image-only PDF + OCR failure 仍为空 | PASS | `RealParserProviderTest#pdfBoxProviderKeepsImageOnlyPdfEmptyWhenConfiguredOcrProviderFails` |
| 不新增 OCR 依赖 | PASS | dependency tree 无 Tess4J/JNA/云 OCR SDK 命中 |
| `IndexService` 未修改 | PASS | Context Pack 禁止修改；adjacent tests 通过 |
| focused verification | PASS | `21 run, 0 failures, 0 errors` |
| adjacent verification | PASS | `33 run, 0 failures, 0 errors` |
| full backend verification | PASS | `392 run, 0 failures, 0 errors, 1 skipped` |

## 3. 验证命令

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=ProcessOcrFallbackProviderTest,ConfigurableOcrFallbackServiceTest,RealParserProviderTest test
mvn --% -Dtest=DocumentParserServiceTest,IndexServiceTest,IndexServiceParserFailureTest test
mvn --% dependency:tree -Dincludes=net.sourceforge.tess4j:tess4j,net.java.dev.jna:jna,org.bytedeco:*,com.aliyun:*,com.google.cloud:google-cloud-vision,software.amazon.awssdk:textract
mvn --% -DskipTests compile
mvn test
```

结果：

- Focused：`21 run, 0 failures, 0 errors, 0 skipped`
- Adjacent：`33 run, 0 failures, 0 errors, 0 skipped`
- Dependency tree：无 OCR/native/cloud OCR SDK 命中项
- Compile：build success
- Full backend：`392 run, 0 failures, 0 errors, 1 skipped`

## 4. 明确非验收范围

以下内容仍未完成，不能因本切片被误标为完成：

- 真实 OCR SDK/native/cloud provider。
- OCR confidence / quality / page metadata。
- 工业级 PDF/DOCX layout/table/TOC/reading-order 恢复。
- real VectorDB client。
- P3-2 全部完成。

## 5. 后续建议

继续 P3-2 时建议二选一：

1. 真实 OCR provider 切片：先做 dependency / security / runtime / privacy review，再接入具体 provider。
2. 工业级 PDF/DOCX layout 切片：先定义 layout metadata contract，再选择 parser strategy。
