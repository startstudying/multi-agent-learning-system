# RUN-20260608-p3-remaining-rag-parser-ocr

## 1. 当前 parser 边界与已完成能力

### 当前边界

- `backend/src/main/java/com/learningos/rag/parser/` 已经形成独立 parser 边界：
  - `DocumentParserService`：按 `KbDocument.name/contentType` 选择 `MARKDOWN`、`TXT`、`PDF`、`DOCX`。
  - `ParsedDocument`：统一输出 parser 类型和 `ParsedSection` 列表。
  - `ParsedSection`：统一携带 `title`、`headingLevel`、`headingPath`、`content`、`pageNum`。
  - `DocumentParseException`：只暴露 `safeCode`，当前统一为 `DOCUMENT_PARSE_FAILED`。
- `IndexService` 只消费 `ParsedDocument`：
  - 在 `processIndexTask` 的 `PARSING` 阶段调用 `parserService.parse(document, bytes)`。
  - 在 `splitAndDeduplicate` 中按 section 内容切 token window。
  - 在 `toChunks` 中把 `pageNum` 落到 `KbDocChunk.pageNum`，把 `sectionTitle` 落到 `KbDocChunk.sectionTitle`。
  - 在 `metadataJson` 中写入 `parser`、`headingLevel`、`headingPath`、`chunkingStrategy`、`chunkTokenCount`、embedding/vector 状态。
- manual reindex 和 worker index 均复用 `IndexService.processIndexTask`，因此已经共享同一 parser path。

### 已完成能力

- Markdown：
  - 支持 `#` 到 `######` heading。
  - 维护 heading stack，输出 heading path。
  - 空 section 不落入 parsed sections。
  - 支持 BOM 清理。
- TXT：
  - UTF-8 严格解码。
  - 空文本返回空 sections。
  - NUL / malformed UTF-8 / 明显 binary/control garbage 通过 `DOCUMENT_PARSE_FAILED` 拒绝。
- PDF：
  - 当前是轻量文本抽取，不是完整 PDF parser。
  - 支持简单 `(...) Tj` 与 `[(...)(...)] TJ` 文本抽取。
  - 无 text object 的扫描型 PDF 不会把 raw `%PDF`、object stream、binary bytes 当作 chunk fallback。
  - 当前所有 PDF section 的 `pageNum` 固定为 `1`，没有真实页码识别。
  - 当前没有 OCR fallback。
- DOCX：
  - 不使用 `ZipInputStream` 遍历解压整包，而是读取 EOCD / central directory 后只定位 `word/document.xml`。
  - 限制 entry count、`document.xml` 解压后大小。
  - 拒绝 ZIP64、分卷、加密、central/local header mismatch、duplicate document XML、unsupported compression method 等风险结构。
  - 解析 `Heading1` 到 `Heading6`，维护 heading path。
  - 识别 `w:lastRenderedPageBreak` 与 `w:br w:type="page"`，做 best-effort page number。
  - 当前只读取 `word/document.xml`，没有读取 styles、numbering、headers/footers、tables/footnotes/endnotes/comments、relationships、alt text。

### 已有测试覆盖

- `DocumentParserServiceTest`
  - Markdown heading hierarchy。
  - TXT single section / blank。
  - PDF simple `Tj/TJ`。
  - PDF 无 text object 不 fallback raw bytes。
  - DOCX heading/page break。
  - DOCX page break count。
  - DOCX 非目标 entry 不解压。
  - DOCX oversized XML / malformed zip / too many entries / central-local mismatch。
  - text binary rejection。
- `IndexServiceTest`
  - Markdown chunk 清洗、token window、overlap、stable hash、heading hierarchy metadata。
  - TXT/PDF/DOCX 走同一 indexing path。
  - 扫描型 PDF 不落 raw binary chunk。
  - DOCX headingPath/pageNum 落到 chunk 与 metadata。
- `IndexServiceParserFailureTest`
  - parser raw exception message 不落 task/document，只保存 `DOCUMENT_PARSE_FAILED`。

## 2. 最小下一切片建议

### 建议优先切片：P3-2-H Parser Layout Metadata Hardening

优先做“不新增依赖”的 parser 增强，避免一次性引入 PDF/OCR 大依赖导致安全、许可证、部署体积、平台运行时复杂度同时扩大。

目标：

- 不宣称完成复杂 PDF/OCR。
- 先把现有轻量 parser 的 page/section metadata 做得更可解释、可测试、可回归。
- 保持 `IndexService` 不新增格式解析逻辑。

推荐内容：

1. PDF page boundary best-effort：
   - 识别简单 PDF page object / content stream boundary 的最小安全方案，至少避免所有 PDF chunk 永远 `pageNum=1`。
   - 仍只抽取明确文本操作符，不把 raw stream fallback 成正文。
   - 如果无法稳定识别页码，metadata 应明确 `pageDetection=BEST_EFFORT` 或在 parser 层内部测试覆盖失败回退为空页码/1 页，避免误导为真实页码。
2. PDF heading best-effort 非目标：
   - 当前无字体/坐标解析，不建议在无依赖阶段推断 heading hierarchy。
   - 可仅按文本中的明显标题模式做保守 section split，例如独立行、短文本、数字编号标题；但这容易误伤，建议放到后续独立切片，当前先做页码。
3. DOCX table/list/text coverage：
   - 当前 `w:t` 可抽取表格单元格内文本，因为表格内仍有 `w:p`，但测试应补明确覆盖。
   - 补 `w:tab`、`w:br` 普通换行、多个 `w:t` 合并语义的测试。
   - 补 heading paragraph 不生成空 section、连续 heading、heading 后跨页正文的测试。
4. metadata clarity：
   - 不改 schema 的前提下，继续通过 `KbDocChunk.metadataJson` 写 parser metadata。
   - 可增加 `pageDetection`、`sectionDetection`、`ocrStatus` 这类低风险字符串字段，但必须评估是否会影响既有断言。

### OCR fallback 建议

OCR 不建议并入下一个最小切片。OCR 需要新增依赖或外部服务，必须单独走 dependency review。

如果后续要做 OCR，建议单独切片：P3-2-I OCR Adapter Boundary。

依赖审查点：

- OCR 引擎形态：本地库、命令行进程、容器服务、云 OCR API。
- 许可证：Tesseract / Tess4J / PDFBox / image renderer / native binary 的许可证兼容性。
- CVE 与维护状态：解析类依赖属于高风险输入面。
- 运行时隔离：超时、最大页数、最大图片尺寸、最大输出字符数、并发限制。
- 隐私与数据出境：云 OCR 不得默认启用，课程资料可能包含敏感教学/学生内容。
- 错误脱敏：不得记录 raw OCR provider error、文件路径、storage key、页面图片 bytes、正文片段。
- 成本治理：OCR 是高 CPU/高延迟任务，需要 worker lease、retry、budget/ops alert 规划。

## 3. 推荐修改文件列表与禁止修改文件

### 推荐允许修改文件

最小无依赖切片：

- `backend/src/main/java/com/learningos/rag/parser/DocumentParserService.java`
- `backend/src/test/java/com/learningos/rag/parser/DocumentParserServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`
- 如需更新文档：
  - `docs/specs/SPEC-20260608-rag-parser-layout-metadata.md`
  - `docs/plans/PLAN-20260608-rag-parser-layout-metadata.md`
  - `docs/tasks/TASK-20260608-rag-parser-layout-metadata.md`
  - `docs/context/CONTEXT-20260608-rag-parser-layout-metadata.md`
  - `docs/evidence/EVIDENCE-20260608-rag-parser-layout-metadata.md`
  - `docs/acceptance/ACCEPT-20260608-rag-parser-layout-metadata.md`
  - `docs/memory/BACKEND_MEMORY.md`
  - `docs/memory/PROJECT_MEMORY.md`
  - `docs/changelog/CHANGELOG.md`

OCR 独立切片才允许考虑：

- 新增 `backend/src/main/java/com/learningos/rag/parser/OcrService.java` 或等价接口。
- 新增 noop/default OCR implementation。
- 新增 OCR dependency review：`docs/security/DEPENDENCY-20260608-rag-ocr-*.md`。

### 禁止修改文件

最小无依赖切片不应修改：

- `backend/pom.xml`
- `backend/src/main/resources/**`
- Flyway migration
- RAG query / citation API DTO
- Vector / embedding adapter
- `IndexService` 中的 chunk/hash/window 算法，除非只是为读取新增 parser metadata 做极小适配；优先不改。
- 权限、认证、课程 scope 相关代码。
- 前端代码。

OCR 切片未通过依赖审查前禁止修改：

- `backend/pom.xml`
- Dockerfile / docker-compose
- 生产配置中的外部 OCR endpoint/key。

## 4. 必须新增/调整的测试

### 无依赖 parser layout metadata 切片

`DocumentParserServiceTest` 必须新增：

- PDF 多页 best-effort：
  - 构造最小多页 PDF-like bytes，文本分别位于两个可识别 page/content 边界。
  - 期望 sections/chunks pageNum 不全部为 1。
  - 断言不包含 `BT`、`ET`、object id、stream raw bytes。
- PDF 扫描页无 text：
  - 仍返回空 sections 或最终 indexing failed 为 `DOCUMENT_EMPTY_OR_UNAVAILABLE`。
  - 不触发 OCR，除非 OCR 切片明确启用。
- PDF 混合页：
  - 第 1 页有 text，第 2 页无 text，第 3 页有 text。
  - 期望只产生有 text 的 sections，页码保留 1/3 或安全 fallback，不能把第 2 页 raw bytes 落库。
- DOCX table text：
  - 表格内 `w:tbl/w:tr/w:tc/w:p/w:r/w:t` 文本可被解析。
- DOCX ordinary line break / tab：
  - `w:tab`、非 page `w:br` 不应粘连成难读文本。
- DOCX heading edge cases：
  - 连续 heading 不生成空正文 section。
  - heading 后跨 page break 的正文 pageNum 正确递增。
  - Heading1 -> Heading3 时 headingPath 行为明确且稳定。

`IndexServiceTest` 必须新增/调整：

- PDF pageNum 从 parser 输出落到 `KbDocChunk.pageNum`。
- PDF parser metadata 中 `parser=PDF`，且如果新增 `pageDetection`，断言 metadata 值。
- DOCX table/list/page metadata 经过 indexing 后仍保留 sectionTitle、headingPath、pageNum。
- stable hash 不因新增 metadata 字段变化而改变。当前 hash 输入是 document id/version + headingPath + content；新增 metadata 不应进入 hash。

`IndexServiceParserFailureTest` 保持：

- parser exception 仍只落 `DOCUMENT_PARSE_FAILED`。
- 不记录 raw PDF bytes、DOCX XML、storage key、文件路径、secret-like 字符串。

### OCR 独立切片测试

仅 OCR 切片需要：

- OCR disabled 默认路径：扫描型 PDF 仍失败为空，不调用 OCR。
- OCR enabled fake adapter：扫描型 PDF 可产生 text sections。
- OCR timeout：保存固定安全错误码，不落 raw provider error。
- OCR size/page limit：超过限制失败或跳过，不能拖垮 worker lease。
- OCR no-source / low-confidence：metadata 记录 `ocrStatus`、`confidence` 范围，但不把图片 bytes 或 provider payload 入库。

## 5. 风险与验收标准

### 风险

- PDF 无依赖解析天然脆弱：完整 PDF text extraction 需要处理 stream compression、font encoding、ToUnicode CMap、页面资源继承、旋转、坐标排序。当前正则方案只能覆盖合成和极简单文本操作符。
- 误标页码比无页码更危险：RAG citation 展示错误页码会降低可信度，应在 metadata 中标注 best-effort 或保持保守。
- DOCX 正则 XML 解析可维护性有限：当前只读 `document.xml` 并正则 paragraph/text/style，复杂 WordML 结构可能丢失列表编号、脚注、批注、文本框、图片 alt text。
- OCR 是高风险输入处理面：新增依赖、native binary、外部服务、成本和隐私风险都需要单独治理。
- 不应为了 parser 增强破坏已完成的 P3-2 chunk token window、overlap、stable hash、heading hierarchy、embedding/vector noop 边界。

### 验收标准

最小无依赖切片完成条件：

- `DocumentParserService` 仍是唯一格式解析入口。
- `IndexService` 不出现 PDF/DOCX 解析细节。
- PDF 简单多页文本能输出可区分 pageNum，或明确记录 best-effort/fallback 语义。
- 扫描型 PDF 不会把 raw bytes/object stream 作为正文。
- DOCX table/line-break/heading edge cases 有测试覆盖。
- chunk metadata 保留 parser、headingLevel、headingPath、chunkingStrategy、embedding/vector 状态；如新增 metadata 字段，必须有断言。
- stable chunk hash、token window、40 token overlap 不回退。
- parser failure 仍只保存安全错误码。
- 不新增依赖、不改 `pom.xml`。

OCR 切片完成条件：

- 有 dependency review。
- OCR 默认 disabled 或 noop。
- OCR adapter 有超时、页数、文件大小、输出长度限制。
- OCR 错误和日志脱敏。
- 有 fake adapter 测试，不依赖真实外部服务。

## 6. 建议切片名

推荐下一个最小切片：

**P3-2-H RAG Parser Layout Metadata Hardening**

后续独立切片：

**P3-2-I OCR Adapter Boundary**

不建议把复杂 PDF、复杂 DOCX、OCR 和依赖接入合并到一个切片。下一步应先完成无新依赖的 layout/page/section metadata hardening，再根据 dependency review 决定 OCR adapter。
