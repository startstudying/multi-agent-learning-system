# Subagent Run - P3-2-G Real Parser SDK Provider Dependency Review

## 1. 依赖结论

允许在本切片新增两个 Apache 官方依赖：

| Dependency | Version | Purpose |
|---|---:|---|
| `org.apache.pdfbox:pdfbox` | `3.0.7` | PDF 文本提取与页级 section 输出 |
| `org.apache.poi:poi-ooxml` | `5.5.1` | DOCX 段落、run、样式与 OOXML 读取 |

## 2. 官方来源核对

- PDFBox 官网下载页显示 PDFBox `3.0.7` 为 3.0.x 当前发布版本，Maven metadata 也显示 latest/release 为 `3.0.7`。
- POI 官网下载页显示 Apache POI `5.5.1` 为 latest stable release，Maven metadata 也显示 latest/release 为 `5.5.1`。
- 两个项目均为 Apache Software Foundation 项目，许可证为 Apache License 2.0。

## 3. 安全约束

- 不引入 PDFBox examples/tools，不复制 `ExtractEmbeddedFiles` 示例代码。
- 不处理 PDF embedded files / attachments，只做文本提取。
- 不引入 OCR、native tessdata、外部 OCR 服务或密钥。
- 不引入 Tika 自动识别，避免扩大解析格式面。
- 所有 parser failure 通过 `DocumentParserService` 映射为 `DOCUMENT_PARSE_FAILED`。
- Provider 输入只来自 `ParseInput.bytes()`，不接收 storage bucket/key/path。
- 解析前执行最大文件大小限制；解析中限制页数、段落数、输出字符数。

## 4. 替代方案

| Alternative | Decision | Reason |
|---|---|---|
| Apache Tika | Rejected | 解析面过宽，依赖和安全边界过大 |
| docx4j | Rejected | 本切片只需 DOCX 文本/heading，POI 更小更常用 |
| iText | Rejected | 许可证/商用合规复杂，不适合作为最小切片 |
| Tess4J / OCR service | Deferred | 需要 native/runtime/隐私/费用/超时单独设计 |
| 继续 lightweight parser | Rejected for this slice | 无法满足真实 PDF/DOCX SDK 接入目标 |

## 5. 验证要求

- `mvn --% -Dtest=DocumentParserServiceTest,RealParserProviderTest,NoopOcrFallbackServiceTest test`
- `mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest test`
- `mvn dependency:tree -Dincludes=org.apache.pdfbox:pdfbox,org.apache.poi:poi-ooxml`
- `mvn test`

