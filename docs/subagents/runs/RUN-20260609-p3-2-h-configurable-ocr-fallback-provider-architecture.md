# Subagent Run - P3-2-H Configurable OCR Fallback Provider Architecture

## Summary

P3-2-H 归类为 RAG parser 配置边界增强 / 最小后端配置切片，不是“真实 OCR 接入”任务。现有 `OcrFallbackService` / `OcrFallbackResult` / `NoopOcrFallbackService` / `PdfBoxDocumentFormatParser` 已有 OCR fallback 合同和 noop 行为，但缺少显式 Spring Boot 配置边界，且未来新增真实 `OcrFallbackService` bean 时存在多 bean 注入歧义风险。

建议最小切片只补配置与 provider 选择边界：默认 `none/noop`，启用但无 provider 时返回 `UNAVAILABLE`，失败返回 `FAILED` 且不让 raw provider 异常污染索引错误，不新增 Tess4J/native/cloud OCR SDK。

## 1. 任务分类与 Subagents

- 任务类型：RAG parser provider boundary / configurable fallback provider。
- 执行焦点：只定位 parser fallback 层，不改 chunk、embedding、VectorDB、query/citation、API、DB。
- Subagents：Level 1 并行分析即可。因为涉及 RAG，启用 RAG/parser expert；实现面只影响 backend `rag/parser` 与配置测试，不需要并行实现。

## 2. 现有代码边界与可修改文件建议

当前边界：

- `OcrFallbackService` 只有 `extractText(ParseInput input)`，边界足够小。
- `OcrFallbackResult` 已有 `DISABLED`、`UNAVAILABLE`、`SUCCEEDED`、`FAILED` 四态。
- `NoopOcrFallbackService` 当前固定返回 `DISABLED / OCR_DISABLED / ""`。
- `DocumentParserService` 通过 `DocumentFormatParser` registry 选择 parser provider，外部 provider 可覆盖默认 lightweight provider。
- `PdfBoxDocumentFormatParser` 在 PDFBox 提取不到文本时调用 OCR fallback；成功且有文本才生成 section，否则返回空文档。

建议允许修改：

- `backend/src/main/java/com/learningos/rag/parser/**`
- `backend/src/main/java/com/learningos/config/**`
- `backend/src/main/java/com/learningos/LearningOsApplication.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-test.yml`
- `backend/src/test/java/com/learningos/rag/parser/**`
- 本切片 workflow / security / evidence / acceptance / memory / changelog / retrospective / planning 文档

禁止修改：

- `backend/src/main/java/com/learningos/rag/application/IndexService.java`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
- `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java`
- `backend/src/main/java/com/learningos/rag/application/VectorIndexAdapter.java`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- `backend/pom.xml`

## 3. 最小行为模型

| 状态 | 条件 | `OcrFallbackResult` | parser 输出 |
|---|---|---|---|
| disabled | 默认配置关闭 | `DISABLED / OCR_DISABLED / ""` | 空 sections |
| enabled unavailable | 配置启用但 provider 不存在或未配置 | `UNAVAILABLE / OCR_PROVIDER_UNAVAILABLE / ""` | 空 sections |
| failure | provider 执行异常、超时、返回非法结果 | `FAILED / OCR_PROVIDER_FAILED / ""` | 空 sections，不泄漏 raw 异常 |
| success | provider 返回非空文本 | `SUCCEEDED / <safe_code> / text` | 生成 OCR section |

## 4. 配置命名建议

```yaml
learning-os:
  rag:
    parser:
      ocr:
        enabled: ${RAG_PARSER_OCR_ENABLED:false}
        provider: ${RAG_PARSER_OCR_PROVIDER:none}
```

推荐 provider：

- `none`：默认 disabled/no provider。
- `custom` / 未来具体 provider id：仅在项目内已有 provider bean 且完成独立 review 后使用。

## 5. TDD 测试建议

- 默认关闭：扫描 PDF 无文本返回空 sections，`OcrFallbackService` 返回 `DISABLED / OCR_DISABLED`。
- 启用但 provider 缺失：返回 `UNAVAILABLE / OCR_PROVIDER_UNAVAILABLE`。
- provider 成功：fake provider 返回短文本后，PDF image-only fallback 生成 section。
- provider 失败：fake provider 抛出包含 secret/path/raw text 的异常后，返回 `FAILED / OCR_PROVIDER_FAILED / ""`。
- 超长 OCR 文本：仍由 `PdfBoxDocumentFormatParser` 映射为 `DOCUMENT_PARSE_FAILED`。
- adjacent：`IndexServiceTest` / `IndexServiceParserFailureTest` 证明索引编排不变。

## 6. 风险与不可做事项

风险：

- 当前 noop 是无条件 `@Service`，未来真实 provider 若同样注册为 `OcrFallbackService`，构造注入可能失败；建议通过配置选择单一 delegated provider。
- `DocumentParserService` 和 `PdfBoxDocumentFormatParser` 都持有 OCR fallback；配置设计应优先服务真实 PDFBox provider。
- `OcrFallbackResult` 当前无页码、置信度、latency 等元数据，本切片不扩大模型。

不可做：

- 不引入 Tess4J/native OCR/cloud OCR SDK。
- 不修改 `pom.xml`。
- 不改 DB schema、API、frontend。
- 不让 OCR provider 访问 Mapper/Repository。
- 不把 raw PDF/image bytes 当作文本 fallback。
- 不把 raw provider exception、文件路径、storage key、API key 写入 task/document error。
- 不声明“真实 OCR 已完成”。

