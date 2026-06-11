# P3-2 子任务：DOCX table/TOC reading-order provider 技术规格

## 范围

本规格限定在 RAG parser/indexing 边界内：

- DOCX parser 输出 `ParsedDocument` / `ParsedSection`
- IndexService 复用已有 chunk metadata 写入路径

不涉及 API、DB、frontend、retrieval、citation、VectorDB、OCR provider。

## 现状

`PoiDocxDocumentFormatParser` 当前通过 `XWPFDocument#getParagraphs()` 遍历段落。该方式无法表达 DOCX body 中 paragraph 与 table 的相对顺序，也不会把主文档表格作为独立 section 输出。

轻量 fallback parser 当前通过 XML paragraph regex 解析 `<w:p>`，同样不会处理 `<w:tbl>`。

## 目标行为

### Body order

POI provider 使用：

```java
document.getBodyElements()
```

按 `IBodyElement` 顺序处理：

- `XWPFParagraph`：走原有 paragraph 逻辑
- `XWPFTable`：输出 table section

轻量 fallback parser 以 XML body element 顺序识别 `<w:p>` 与 `<w:tbl>`。

### Paragraph

保持现有行为：

- `Heading1` 至 `Heading6` 更新 heading stack。
- heading paragraph 不生成空 section。
- 普通段落输出 `contentKind=TEXT`。
- 显式 page break 继续递增 best-effort `pageNum`。

### Table

table 输出为一个独立 `ParsedSection`：

- `title` / `headingLevel` / `headingPath` 使用当前 heading context。
- `pageNum` 使用当前 best-effort page number。
- `pageNumSource=PARSER_INFERRED`。
- `contentKind=TABLE_TEXT`。
- `layoutConfidence=null`。
- `ocrConfidence=null`。

table 文本格式：

```text
cell_1_1 | cell_1_2; cell_2_1 | cell_2_2
```

规则：

- 单元格文本必须清洗空白和控制字符。
- 空单元格可跳过；整行空则跳过。
- 不写 raw XML、样式、坐标、provider response。

### TOC

TOC-like paragraph 默认跳过：

- POI provider 可基于 style `TOC*` 或 field instruction 中的 `TOC` 判断。
- 轻量 parser 可基于 `<w:pStyle w:val="TOC...">` 或 `<w:instrText>` 中的 `TOC` 判断。
- 跳过 TOC 不更新 heading stack，不生成 section。

## 安全与治理

- 不新增依赖，因此无需 dependency review。
- parser failure 仍映射为 `DOCUMENT_PARSE_FAILED`。
- chunk metadata 只允许短字段：`pageNumSource`、`readingOrderIndex`、`contentKind`、`layoutConfidence`、`ocrConfidence`。
- 禁止写入 raw OCR、raw XML、provider response、stderr/stdout、文件路径、storage key、secret。

## 架构漂移检查

预期无架构漂移：

- Controller 未改。
- Service 分层未改。
- Agent/Tool 未改。
- API/DB/frontend 未改。
- RAG parser 仍在 `rag/parser` boundary 内。

## 测试规格

Focused：

- `RealParserProviderTest` 覆盖 POI DOCX table body order、TOC skip、`TABLE_TEXT` metadata。
- `DocumentParserServiceTest` 覆盖轻量 DOCX XML table body order 与 TOC skip。
- `IndexServiceTest` 覆盖 chunk metadata 传播 `TABLE_TEXT` / `readingOrderIndex` / `pageNumSource`。

Adjacent：

- parser、OCR fallback、index failure 相关测试。

Full：

- `mvn test`
