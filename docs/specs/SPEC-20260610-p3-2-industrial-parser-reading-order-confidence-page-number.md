# SPEC-20260610 P3-2 子任务：industrial parser reading-order/confidence/page-number metadata

## 1. 设计摘要

本切片只做 parser 输出契约增强：

```text
DocumentFormatParser / DocumentParserService
-> ParsedDocument
-> ParsedSection metadata
-> IndexService ChunkDraft
-> KbDocChunk.metadataJson
```

不新增依赖、不改 DB、不改 HTTP API。

## 2. Parser Metadata 字段

| Field | Type | 默认值 / 规则 | 说明 |
|---|---|---|---|
| `pageNumSource` | `String` | `NONE` when `pageNum == null`；否则 `PARSER_INFERRED` | 页码来源短枚举字符串，后续可扩展为 `PDF_PAGE_INDEX`、`DOCX_PAGE_BREAK`、`OCR_FALLBACK`、`PDF_PAGE_LABEL` |
| `readingOrderIndex` | `Integer` | `ParsedDocument` 自动按 section 顺序填 1-based | section 级稳定阅读顺序 |
| `layoutConfidence` | `Double` | `null` | layout/table/reading-order provider 后续可填 0..1 |
| `ocrConfidence` | `Double` | `null` | OCR provider 后续可填 0..1 |
| `contentKind` | `String` | `TEXT` | 后续可扩展 `OCR_TEXT`、`TABLE_TEXT`、`TOC_TEXT` |

confidence 规则：

- `null` 表示未提供。
- 非有限数、负数、超过 1 的值归一为空或安全收敛，不能抛出造成旧 parser 大面积失败。

## 3. 兼容策略

`ParsedSection` 保留旧 5 参数构造器：

```java
new ParsedSection(title, headingLevel, headingPath, content, pageNum)
```

旧构造器默认：

- `pageNumSource = pageNum == null ? "NONE" : "PARSER_INFERRED"`
- `readingOrderIndex = null`，由 `ParsedDocument` 自动填充
- `layoutConfidence = null`
- `ocrConfidence = null`
- `contentKind = "TEXT"`

`OcrFallbackResult` 保留旧 3 参数构造器，并新增 `confidence` 字段。

## 4. Index Metadata

`IndexService.metadataJson(...)` 增加：

- `pageNumSource`
- `readingOrderIndex`
- `contentKind`
- `layoutConfidence`（仅非空写入）
- `ocrConfidence`（仅非空写入）

不写入：

- raw OCR text
- provider raw response
- stderr/stdout 原文
- 文件路径、storage key、secret
- 大型 layout 坐标 payload

## 5. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | parser contract 在 `rag/parser`，索引 metadata 在 `IndexService`。 |
| Frontend rules | PASS | 不改前端。 |
| Agent / RAG rules | PASS | 不改 retrieval/citation API；RAG 仍由后端处理。 |
| Security | PASS | 不新增依赖，不存 raw provider/private payload。 |
| API / Database | PASS | 不改 HTTP API/DTO/DB schema。 |

## 6. 风险

- Java record 增字段会影响构造器；通过旧构造器兼容降低 diff。
- `metadataJson` 增加字段可能影响测试快照；应只断言新增短字段。
- 本切片不代表工业级 layout/table/TOC/OCR 已完成，todolist 仍需保留父项 open。
