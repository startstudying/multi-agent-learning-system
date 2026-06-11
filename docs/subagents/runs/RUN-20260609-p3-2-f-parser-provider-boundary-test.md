# Subagent Report - P3-2-F Parser Provider Boundary Test Plan

## 1. 结论

本切片是 parser 内部边界重构，测试重点不是证明真实 PDF/DOCX/OCR 能力，而是证明：

- provider 分发存在且不会改变既有格式行为。
- noop OCR contract 存在且不会把 image-only PDF 伪装成可索引文本。
- parser failure 仍走安全错误码。
- `IndexService` 与 parser 边界不漂移。

## 2. 最小 RED 测试清单

| 优先级 | 测试 | 目标行为 | 预期 RED 信号 |
|---|---|---|---|
| P0 | `serviceDelegatesPdfToRegisteredProvider` | `DocumentParserService` 通过 `DocumentFormatParser` registry 调用 PDF provider | 当前无 `DocumentFormatParser` / 构造注入，编译失败 |
| P0 | `noopOcrFallbackIsDisabledAndReturnsNoText` | noop OCR 返回 `DISABLED` 且无文本 | 当前无 `OcrFallbackService` / `OcrFallbackResult`，编译失败 |
| P0 | `pdfReturnsEmptySectionsForImageOnlyPdfUntilOcrAdapterExists` | image-only PDF 不 raw fallback，不通过 noop OCR 产生 section | 若重构误返回 raw bytes 或 OCR 文本则失败 |
| P1 | `providerFailureIsMappedToSafeParserCode` | provider 抛出非安全异常时，service 映射为 `DOCUMENT_PARSE_FAILED` | 当前没有 provider 注入路径，编译失败 |
| P1 | `existingParserBehaviorsRemainStable` | Markdown/TXT/PDF/DOCX 既有行为不回退 | focused parser 回归失败 |

## 3. 不应新增的测试

本切片不应新增：

- 真实 PDFBox 页面抽取测试。
- Apache POI DOCX 复杂结构测试。
- Tess4J/native OCR 测试。
- 外部 OCR HTTP 调用测试。
- 新 API/schema/frontend 测试。

这些会把任务范围推向真实 parser/OCR 接入，违反本切片边界。

## 4. 验证命令建议

```powershell
cd backend
mvn --% -Dtest=DocumentParserServiceTest,NoopOcrFallbackServiceTest test
mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest test
mvn --% -Dtest=DocumentParserServiceTest,NoopOcrFallbackServiceTest,IndexServiceTest,IndexServiceParserFailureTest,RagQueryServiceTest test
mvn test
```

## 5. 验收关注点

- RED 必须先失败，失败原因应为缺少 contract 或 provider 分发，而不是测试拼写错误。
- GREEN 后既有 parser 测试数量不减少。
- full backend 测试通过后才能声明切片完成。
