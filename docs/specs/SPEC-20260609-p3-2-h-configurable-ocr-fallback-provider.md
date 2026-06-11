# SPEC - P3-2-H Configurable OCR Fallback Provider

## 1. 概述

本规格定义 P3-2-H：在已有 `rag/parser` OCR fallback contract 后，补齐可配置 OCR fallback provider boundary。该切片只实现配置、provider 选择、状态归一与安全失败，不接入真实 OCR SDK。

## 2. 配置

新增 Spring Boot 配置：

```yaml
learning-os:
  rag:
    parser:
      ocr:
        enabled: ${RAG_PARSER_OCR_ENABLED:false}
        provider: ${RAG_PARSER_OCR_PROVIDER:none}
```

建议配置类：

- `RagParserOcrProperties`
- prefix：`learning-os.rag.parser.ocr`
- 字段：
  - `enabled: boolean`
  - `provider: String`

默认值：

- `enabled=false`
- `provider=none`

## 3. OCR provider 扩展点

新增轻量 provider SPI：

```java
public interface OcrFallbackProvider {
    String provider();
    OcrFallbackResult extractText(ParseInput input);
}
```

说明：

- 真实 OCR provider 后续实现该接口，而不是直接注册多个 `OcrFallbackService`。
- `ConfigurableOcrFallbackService` 是 Spring context 中对外的 `OcrFallbackService`，负责按配置选择 provider。
- `NoopOcrFallbackService` 保留直接实例化兼容，但不作为未来真实 provider 选择机制。

## 4. 行为模型

| 场景 | 条件 | 输出 |
|---|---|---|
| 默认关闭 | `enabled=false` | `DISABLED / OCR_DISABLED / ""` |
| 启用但 provider 缺失 | `enabled=true` 且 `provider` 为空、`none` 或没有匹配 provider | `UNAVAILABLE / OCR_PROVIDER_UNAVAILABLE / ""` |
| provider 成功 | provider 返回 `SUCCEEDED` 且 text 非空 | 透出安全 reasonCode 与 text |
| provider 失败 | provider 抛异常或返回 null | `FAILED / OCR_PROVIDER_FAILED / ""` |
| provider reasonCode 不安全 | reasonCode 含非 `[A-Z0-9_]` 或超长 | 使用默认安全码 |

## 5. PDF fallback 行为

`PdfBoxDocumentFormatParser` 已有行为保持：

- PDFBox 抽取不到文本时调用 `OcrFallbackService`。
- OCR `SUCCEEDED` 且 text 非空时生成 `ParsedSection(pageNum=1)`。
- OCR disabled/unavailable/failed 时返回空 sections。
- OCR text 超过 `ParserResourceLimits.MAX_EXTRACTED_CHARS` 时抛出 `IllegalArgumentException`，由 `DocumentParserService` 映射为 `DOCUMENT_PARSE_FAILED`。

## 6. 边界约束

- 不修改 `IndexService`。
- 不修改 API。
- 不修改 DB migration。
- 不修改 frontend。
- 不修改 retrieval/citation/VectorDB/embedding。
- 不新增 Maven 依赖。
- 不持久化 raw provider exception、OCR 原文、路径、storage key、API key。

## 7. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 配置与 provider 均在 backend parser boundary |
| Frontend rules | PASS | 不改 frontend |
| Agent / RAG rules | PASS | 不改 retrieval/citation/trace |
| Security | PASS | 不新增依赖，默认关闭，safe reasonCode |
| API / Database | PASS | 不改 API/schema |

## 8. 测试策略

```powershell
cd backend
mvn --% -Dtest=ConfigurableOcrFallbackServiceTest,NoopOcrFallbackServiceTest,RealParserProviderTest test
mvn --% -Dtest=DocumentParserServiceTest,IndexServiceTest,IndexServiceParserFailureTest test
mvn test
```

## 9. 非目标

- 真实 OCR。
- OCR native/cloud/service dependency。
- OCR page-level confidence、layout/table/TOC。
- RAG citation schema 变更。
- P3-2 全部完成声明。

