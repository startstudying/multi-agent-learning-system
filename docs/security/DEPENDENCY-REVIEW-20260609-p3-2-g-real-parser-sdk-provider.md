# Dependency Review - P3-2-G Real PDF/DOCX Parser SDK Provider

## 1. Dependencies

| Field | Value |
|---|---|
| Name | `org.apache.pdfbox:pdfbox` |
| Version | `3.0.7` |
| Package Manager | Maven |
| Added By | `TASK-20260609-p3-2-g-real-parser-sdk-provider` |

| Field | Value |
|---|---|
| Name | `org.apache.poi:poi-ooxml` |
| Version | `5.5.1` |
| Package Manager | Maven |
| Added By | `TASK-20260609-p3-2-g-real-parser-sdk-provider` |

## 2. Justification

当前 lightweight PDF/DOCX parser 只能覆盖简单文本对象与 `word/document.xml` best-effort 解析。P3-2-G 需要真实 SDK provider：

- PDFBox 用于真实 PDF 文本提取与页级解析。
- POI `poi-ooxml` 用于真实 DOCX 段落、run、style、page break 读取。

## 3. Official Source Verification

- PDFBox 官网下载页显示 `3.0.7` 为 PDFBox 3.0.x 当前 release；Maven metadata 显示 latest/release 为 `3.0.7`。
- POI 官网下载页显示 `5.5.1` 为 latest stable release；Maven metadata 显示 latest/release 为 `5.5.1`。
- 两个项目均由 Apache Software Foundation 发布，许可证为 Apache License 2.0。

References:

- https://pdfbox.apache.org/download.html
- https://pdfbox.apache.org/security.html
- https://repo.maven.apache.org/maven2/org/apache/pdfbox/pdfbox/maven-metadata.xml
- https://poi.apache.org/download.html
- https://poi.apache.org/security.html
- https://repo.maven.apache.org/maven2/org/apache/poi/poi-ooxml/maven-metadata.xml

## 4. Alternatives Considered

| Alternative | Pros | Cons | Decision |
|---|---|---|---|
| PDFBox + POI | Apache 官方、成熟、直接覆盖 PDF/DOCX | 增加不可信文件解析攻击面 | Adopt |
| Apache Tika | 格式识别全面 | 依赖面过宽，非本切片目标 | Reject |
| docx4j | DOCX 结构强 | 本切片只需文本/heading，依赖更重 | Reject |
| iText | PDF 能力强 | 许可证/商业合规复杂 | Reject |
| Tess4J/OCR service | 可覆盖扫描件 | native/runtime/隐私/费用/密钥风险需独立设计 | Defer |

## 5. License

| Dependency | License | Compatible |
|---|---|---|
| PDFBox | Apache License 2.0 | Yes |
| POI | Apache License 2.0 | Yes |

## 6. Security

- [x] Trusted publisher：Apache Software Foundation。
- [x] Maintained actively：PDFBox 3.0.x 和 POI 5.x 仍在发布。
- [x] No secrets required。
- [x] PDFBox advisory considered：官方安全页列出的 2026 path traversal 问题影响 examples module；本切片不引入 examples/tools，不处理 embedded files，也不复制示例代码。
- [x] POI secure processing guidance considered：POI 明确提示不可信 Office 文件可能导致高 CPU/内存/异常；本切片加入输入、段落、页数、输出字符限制。
- [ ] Full SCA：本地未运行独立 SCA 工具；以官方安全页、Maven metadata、dependency tree 和 focused/full tests 作为本切片证据。

## 6.1 Dependency Tree Evidence

命令：

```powershell
cd D:\多元agent\backend
mvn --% dependency:tree -Dincludes=org.apache.pdfbox:pdfbox,org.apache.poi:poi-ooxml
mvn --% dependency:tree -Dincludes=commons-logging:commons-logging
```

结果：

- `org.apache.pdfbox:pdfbox:jar:3.0.7:compile`
- `org.apache.poi:poi-ooxml:jar:5.5.1:compile`
- `commons-logging:commons-logging` 无 dependency tree 条目。

说明：`backend/pom.xml` 对 PDFBox dependency 排除了 `commons-logging`，避免与 Spring JCL 冲突。

## 7. Required Runtime Safeguards

- 最大输入大小：20 MiB。
- 最大 PDF 页数：500。
- 最大 DOCX 段落数：5000。
- 最大提取字符：2,000,000。
- 失败只暴露 `DOCUMENT_PARSE_FAILED`。
- 不持久化 raw parser exception、storage path/key、API key、原文片段。

## 8. Impact

- Bundle size：新增 PDFBox、POI 与其传递依赖。
- Transitive dependencies：需通过 `mvn dependency:tree` 记录。
- Runtime：只在 RAG document parser provider 内使用，不外呼网络，不需要密钥。

## 9. Approval

| Role | Date | Status |
|---|---|---|
| Architect Expert | 2026-06-09 | APPROVED WITH CONDITIONS |
| Main Codex | 2026-06-09 | APPROVED WITH CONDITIONS |

## 10. Conditions

1. 只在 `rag/parser` provider 内使用 PDFBox/POI。
2. 不引入 OCR/Tika/docx4j/iText/Tess4J。
3. 不修改 `IndexService`。
4. 保持 safe error mapping 与资源限制。
