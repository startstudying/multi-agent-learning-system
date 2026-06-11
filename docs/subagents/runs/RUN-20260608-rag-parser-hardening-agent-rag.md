# RUN-20260608-rag-parser-hardening-agent-rag

## Summary

当前 `DocumentParserService` 已形成统一 parser 边界，`IndexService` 只消费 `ParsedDocument` / `ParsedSection` 并负责 chunk、hash、metadata、任务状态。这符合 `rag-parser-boundary` 规则。

P3-2-C 建议保持无新增依赖边界，只做轻量健壮性加固：防止 PDF raw binary 被误索引，限制 DOCX zip/XML 读取，补齐 DOCX heading/page-break 的 best-effort 结构识别，并增加 parser/index 安全失败回归测试。

## 当前行为证据

- `rag/parser` 已独立于索引编排；`IndexService` 通过 `parserService.parse(document, bytes)` 消费解析结果。
- 当前支持 `MARKDOWN`、`TXT`、`PDF`、`DOCX` 四类 parser。
- Markdown 已识别 `#` 到 `######` heading，并维护 `headingPath`。
- TXT 清洗 control chars 与空白，空白文本返回空 section。
- PDF 当前仅抽取 `(...) Tj`，未命中时会 fallback 为整份 ISO-8859-1 raw 字符串，存在二进制垃圾索引风险。
- DOCX 当前只读取 `word/document.xml`，用 `<w:t>` 正则拼接文本；没有段落、heading style、page break、zip 解压上限。
- parser failure 已可通过 `DocumentParseException("DOCUMENT_PARSE_FAILED")` 进入安全错误码路径。

## Root Cause

根因不是缺少 parser 边界，而是当前无依赖 parser 尚未定义“不可可靠解析时不允许产生 chunk”的质量闸门。PDF raw fallback 会把不可解析内容伪装成文本；DOCX zip/XML 读取缺少资源上限；DOCX 轻量解析没有最小章节与分页结构。

## 推荐测试

- `DocumentParserServiceTest`
  - PDF 无文本对象时不返回 raw `%PDF` / object stream。
  - PDF `Tj` 与 `TJ` 文本数组仍能抽取有效文本。
  - DOCX `Heading1/Heading2` 生成 `headingPath`。
  - DOCX `<w:lastRenderedPageBreak/>` / `<w:br w:type="page"/>` 产生 best-effort `pageNum`。
  - DOCX 超大 `word/document.xml` 或超多 zip entries 映射为 `DOCUMENT_PARSE_FAILED`。
  - Markdown UTF-8 BOM 不破坏首个 heading。
  - TXT/Markdown 二进制垃圾不产生 section。
- `IndexServiceTest`
  - 无可抽取文本 PDF 不保存垃圾 chunk。
  - DOCX heading/pageNum 继续写入 chunk 与 metadata。
- `IndexServiceParserFailureTest`
  - parser cause 中包含路径、secret、原文时，task/document error 仍只保存 `DOCUMENT_PARSE_FAILED`。

## 推荐实现边界

- 只修改 parser 层与必要测试。
- `IndexService` 不新增格式解析逻辑，只消费 parser 输出。
- 不新增 Maven dependency。
- 不新增 DB schema、公开 API、frontend、真实 OCR、真实 PDF/DOCX SDK。
- 只能声明“无依赖轻量 parser hardening”，不能声明“复杂 PDF/DOCX/OCR 已完成”。

## 风险与非目标

### 风险

- 无依赖 PDF 解析无法可靠处理 compressed streams、字体编码、CMap、扫描件、真实页码。
- 无依赖 DOCX regex 解析无法完整覆盖 footnote、header/footer、text box、复杂表格、修订痕迹和样式继承。
- 引入 OCR/PDF/DOCX SDK 会扩大许可证、CVE、部署和资源隔离风险，必须另起依赖审查。

### 非目标

- 不做真实 OCR。
- 不接 Apache PDFBox、Apache POI、Tika、docx4j、iText、Tesseract 或云 OCR。
- 不新增 dependency/schema/API/frontend。
- 不改变 RAG query/citation 公共 API。
- 不保证真实 PDF 页码，只保留 best-effort page metadata。

