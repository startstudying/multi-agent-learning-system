# RAG Parser Boundary

## 2026-06-10 P3-2 DOCX Table/TOC Reading-Order Provider

- DOCX table/TOC enhancements must stay inside `rag/parser`.
- The POI provider should use `XWPFDocument#getBodyElements()` for body-order traversal instead of paragraph-only iteration.
- The lightweight fallback should parse `word/document.xml` body order and distinguish `<w:p>` from `<w:tbl>`.
- Tables may be emitted as independent `ParsedSection` values with `contentKind=TABLE_TEXT`; do not describe this as full structured table reconstruction.
- TOC-like paragraphs should be skipped by default to avoid indexing table-of-contents noise.
- Never write raw XML, field-code XML, provider payloads, file paths, storage keys, secrets, or large layout payloads into parser output or chunk metadata.
- This child slice does not close the industrial layout/OCR/VectorDB parent item; keep PDF layout/table/TOC provider, native/cloud OCR, provider confidence pipeline, true rendered page labels, and real VectorDB as follow-up work.

## 2026-06-10 P3-2 Parser Metadata Contract Foundation

- Parser metadata foundation must stay inside `ParsedSection` / `ParsedDocument` / `OcrFallbackResult` and the existing `IndexService` chunk metadata path.
- Use short, safe fields only: `pageNumSource`, `readingOrderIndex`, `contentKind`, `layoutConfidence`, and `ocrConfidence`.
- `readingOrderIndex` is section-level and 1-based; do not describe it as complex layout reading-order reconstruction.
- `pageNumSource=PARSER_INFERRED` is not a rendered page label; true rendered page mapping must be a separate task.
- OCR fallback success may map to `pageNumSource=OCR_FALLBACK` and `contentKind=OCR_TEXT`; confidence is optional and must be normalized to `null` when invalid.
- Chunk `metadataJson` may include short confidence fields only when non-null.
- Never write `rawOcrText`, provider raw responses, stderr/stdout dumps, file paths, storage keys, secrets, or large layout coordinate payloads into chunk metadata.
- Metadata contract slices do not close the industrial layout/table/TOC/native-cloud OCR parent item; provider algorithms require separate specs and dependency/security review when applicable.

## 2026-06-09 P3-2-I Process-Based Real OCR Provider

- Process-based OCR provider must stay behind `rag/parser` and the existing `OcrFallbackService` boundary.
- Use `learning-os.rag.parser.ocr.process.command` / `timeout` / `max-output-chars` for runtime process configuration; the default stays disabled.
- `command` missing returns `UNAVAILABLE / OCR_PROVIDER_UNAVAILABLE`.
- `ProcessBuilder(List<String>)` is required; no `cmd /c`, PowerShell, shell string, or command concatenation.
- PDF bytes should go through stdin; stdout is the only OCR text source; stderr must be consumed but never exposed.
- Timeout and max-output limits must be enforced; oversized stdout or runtime failure should map to safe OCR failure.
- If a `@ConfigurationProperties` record gains overload constructors, make Spring binding explicit to avoid constructor-selection ambiguity in Boot 3.5.
- This slice does not add real OCR SDK dependencies or change `IndexService`, retrieval, citation, or vector code.

## 2026-06-09 P3-2-H Configurable OCR Fallback Provider

- OCR fallback provider selection must stay behind `rag/parser` and the existing `OcrFallbackService` boundary.
- Use `learning-os.rag.parser.ocr.enabled` and `learning-os.rag.parser.ocr.provider` for runtime selection; the default stays disabled (`false` / `none`).
- `enabled=false` or provider `none` returns `DISABLED / OCR_DISABLED`.
- `enabled=true` with no matching provider returns `UNAVAILABLE / OCR_PROVIDER_UNAVAILABLE`.
- Provider exceptions map to `FAILED / OCR_PROVIDER_FAILED` and must not leak raw path, secret, or stack details.
- Spring should expose only one `OcrFallbackService` bean for the configurable service; OCR SDK/native/cloud implementations should register through `OcrFallbackProvider`.
- This slice does not add real OCR SDK dependencies or change `IndexService`, retrieval, citation, or vector code.

## 使用场景

当实现或审查 RAG 文档上传、解析、chunk、索引 worker、OCR fallback、PDF/DOCX 增强时使用。

## 核心模式

1. 文档解析必须通过 `rag/parser` 边界进入，不把格式解析逻辑直接写进 `IndexService`。
2. `IndexService` 只负责索引编排、任务状态、chunk/hash/metadata 和持久化。
3. parser 输出统一为 section 结构，至少包含 `parser`、`title`、`headingLevel`、`headingPath`、`content`、`pageNum`。
4. manual reindex 和 worker index 必须共用同一 parser service。
5. parser 失败必须映射为安全错误码，例如 `DOCUMENT_PARSE_FAILED`，不能向任务错误信息暴露原始异常、文件路径、storage key 或原文。
6. 新增 parser/OCR 依赖前必须走 dependency review，并单独更新 SPEC / PLAN / TASK。
7. PDF 无法抽取明确 text operator 时必须返回空结果或安全失败，禁止把 raw PDF bytes 作为 chunk 文本 fallback。
8. DOCX 必须只读取 `word/document.xml`；统计 entry 数时优先解析 EOCD / central directory，不为统计而遍历并解压非目标 entry body。
9. DOCX parser 必须限制 entry count 和 `word/document.xml` 解压后大小，并拒绝 unsupported ZIP 结构（ZIP64、分卷、加密、central/local header mismatch 等）。
10. TXT/Markdown 必须对 malformed UTF-8、NUL、明显 binary/control garbage 安全失败，避免伪装文本污染 RAG chunk。
11. 真实 PDF/DOCX SDK provider 必须仍然作为 `DocumentFormatParser` 放在 `rag/parser` boundary 内，不得把 PDFBox/POI 调用点扩散到 `IndexService`、retrieval、citation 或 controller。
12. 新 parser SDK 必须配套资源限制：输入大小、PDF 页数、DOCX 段落数、最大提取字符数；超限统一映射为安全 parser 错误码。
13. PDFBox/POI 依赖只能在 dependency review 明确版本、许可证、维护状态、安全页和 transitive dependency 风险后新增；如出现 `commons-logging` 与 Spring JCL 冲突，应优先排除 transitive `commons-logging`。
14. 真实 SDK provider 完成后也不能声明 OCR 或工业级 layout 完成；OCR、表格、目录、复杂阅读顺序恢复必须作为独立切片验收。

## 测试建议

- `DocumentParserServiceTest` 覆盖 Markdown heading hierarchy、TXT single section、PDF page fallback、DOCX text extraction 和空内容。
- PDF 测试必须覆盖无 text object 不产生 raw `%PDF` / object stream chunk，以及 `Tj` / 简单 `TJ` array 抽取。
- DOCX 测试必须覆盖 malformed zip、entry count limit、`word/document.xml` size limit、document.xml 后续 entries 超限、非目标 entry body 不被 inflate、central/local header name mismatch。
- DOCX heading/page metadata 测试应覆盖 `Heading1`-`Heading6`、同段多个 page break、heading paragraph 不单独生成空 section。
- TXT/Markdown 测试必须覆盖 BOM、NUL、malformed UTF-8、明显 binary/control garbage。
- `ProcessOcrFallbackProviderTest` 应覆盖 command 缺失、stdout success、non-zero exit、timeout、stdout limit、安全不泄露。
- `IndexServiceTest` 覆盖 chunk/hash/metadata 行为不回退。
- `IndexTaskWorkerSchedulerTest` 覆盖 worker 路径继续使用同一索引服务。
- parser 失败路径必须有 service-level 回归，断言安全错误码写入 task/document。

## 反模式

- 在 `IndexService` 里继续堆格式解析细节。
- 因为接入 OCR 或复杂 parser 而顺手修改 RAG query/citation 公共 API。
- 解析失败直接保存 raw exception message。
- 为了解析质量一次性引入多套大型依赖而没有安全/许可证/维护状态审查。
- 用 shell 字符串执行 OCR command，或者把 stderr / path / secret / raw OCR 结果写入返回值。
- 用 `ZipInputStream#getNextEntry()` 遍历整包来统计 DOCX entries，导致非目标 entry 被隐式解压。
- 把“无依赖轻量 parser hardening”描述成“复杂 PDF/DOCX/OCR 已完成”。
