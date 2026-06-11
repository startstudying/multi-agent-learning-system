# SPEC - P3-2-F Parser Provider Boundary + OCR Fallback Contract

## Implementation Status

- Status: Done.
- Completed at: 2026-06-09.
- Evidence: `docs/evidence/EVIDENCE-20260609-p3-2-f-parser-provider-boundary.md`.
- Acceptance: `docs/acceptance/ACCEPT-20260609-p3-2-f-parser-provider-boundary.md`.
- Lightweight providers are implemented as internal default providers in `DocumentParserService`; external `DocumentFormatParser` beans can override or extend later.
- Full backend verification: `mvn test`, `371 run, 0 failures, 0 errors, 1 skipped`.

## 1. 概述

本规格定义 RAG document parser 的 provider boundary 和 OCR fallback contract。目标是在不新增依赖、不改变 API/DB/frontend 的前提下，将 `DocumentParserService` 从单体解析类重构为 provider registry，让后续真实 PDF/DOCX/OCR 能以独立切片接入。

## 2. 追踪

- PRD：`docs/product/PRD-20260609-p3-2-f-parser-provider-boundary.md`
- REQ：`docs/requirements/REQ-20260609-p3-2-f-parser-provider-boundary.md`
- 架构报告：`docs/subagents/runs/RUN-20260609-p3-2-f-parser-provider-boundary-architecture.md`
- 测试报告：`docs/subagents/runs/RUN-20260609-p3-2-f-parser-provider-boundary-test.md`
- 依赖报告：`docs/subagents/runs/RUN-20260609-p3-2-f-parser-provider-boundary-dependency.md`
- 集成报告：`docs/subagents/runs/RUN-20260609-p3-2-f-parser-provider-boundary-integration.md`

## 3. 领域模型

沿用：

- `DocumentParser`
- `ParsedDocument`
- `ParsedSection`
- `DocumentParseException`

新增：

- `ParseInput`
- `DocumentFormatParser`
- `OcrFallbackService`
- `OcrFallbackResult`
- `NoopOcrFallbackService`

## 4. `ParseInput`

`ParseInput` 是 provider 的统一输入。

字段：

- `Long documentId`
- `String name`
- `String contentType`
- `long sizeBytes`
- `byte[] bytes`

约束：

- 构造时复制 `bytes`。
- 访问 `bytes()` 时返回副本。
- 不包含 storage bucket、storage key、文件路径、上传路径。

## 5. `DocumentFormatParser`

接口：

```java
public interface DocumentFormatParser {
    DocumentParser format();
    ParsedDocument parse(ParseInput input);
}
```

约束：

- provider 只负责格式解析。
- provider 不访问 repository、storage、index task、chunk persistence。
- provider 不写 task 状态。
- provider 不保存 raw parser error。

## 6. `DocumentParserService`

职责：

- 根据文档名和 contentType 选择 `DocumentParser`。
- 从 provider registry 找到对应 `DocumentFormatParser`。
- 调用 provider。
- 统一处理异常。

错误处理：

- `DocumentParseException` 原样抛出。
- `IOException`、`IllegalArgumentException`、其他 provider runtime failure 映射为 `DocumentParseException("DOCUMENT_PARSE_FAILED")`。
- 未注册 provider 映射为 `DOCUMENT_PARSE_FAILED`。

兼容性：

- 保留 `parse(KbDocument document, byte[] bytes)` 公开方法。
- 保留无参构造以兼容现有单元测试。
- Spring 上下文可通过 provider bean 列表注入。

## 7. 轻量 provider

| Provider | format | 行为 |
|---|---|---|
| `MarkdownDocumentFormatParser` | `MARKDOWN` | heading-aware sections |
| `TextDocumentFormatParser` | `TXT` | single section，blank skip，binary reject |
| `PdfLightweightDocumentFormatParser` | `PDF` | simple `Tj` / `TJ` text，simple `/Type /Page` best-effort |
| `DocxLightweightDocumentFormatParser` | `DOCX` | `word/document.xml` only，heading/page/tab/break，ZIP safety checks |

所有 provider 输出仍为 `ParsedDocument/ParsedSection`。

## 8. OCR fallback contract

`OcrFallbackService`：

```java
public interface OcrFallbackService {
    OcrFallbackResult extractText(ParseInput input);
}
```

`OcrFallbackResult`：

- `Status status`
- `String reasonCode`
- `String text`

状态：

- `DISABLED`
- `UNAVAILABLE`
- `SUCCEEDED`
- `FAILED`

当前 noop 行为：

- `status = DISABLED`
- `reasonCode = OCR_DISABLED`
- `text = ""`

PDF provider 可以调用 OCR contract，但在 noop 状态下不得产生 section。

## 9. Index 行为

- `IndexService` 不新增格式解析逻辑。
- `IndexService` 继续只消费 `ParsedDocument/ParsedSection`。
- 空 sections 继续由索引层映射为 `DOCUMENT_EMPTY_OR_UNAVAILABLE`。
- parser failure 继续写 `DOCUMENT_PARSE_FAILED`。

## 10. API / DB / Frontend

- 不修改公开 API。
- 不新增或修改 DB migration。
- 不修改 frontend。
- 不修改 vector/retrieval/citation contract。

## 11. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | parser provider 仍在 `rag/parser`，索引层不解析格式 |
| Frontend rules | PASS | 不修改 frontend |
| Agent / RAG rules | PASS | 不改 retrieval/citation/trace |
| Security | PASS | 不新增依赖，不恢复 raw PDF fallback |
| API / Database | PASS | 不改 API / schema |

## 12. 测试策略

```powershell
cd backend
mvn --% -Dtest=DocumentParserServiceTest,NoopOcrFallbackServiceTest test
mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest test
mvn --% -Dtest=DocumentParserServiceTest,NoopOcrFallbackServiceTest,IndexServiceTest,IndexServiceParserFailureTest,RagQueryServiceTest test
mvn test
```

## 13. 非目标

- 真实 OCR。
- 真实 PDFBox / POI / Tika / docx4j / iText。
- 真实 VectorDB。
- 工业级页码/章节识别。
