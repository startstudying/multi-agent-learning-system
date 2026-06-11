# RUN-20260608 backend TODO P3-2 RAG Parser Expert

## 结论

P3-2 当前已经具备安全轻量 parser 边界、chunk 生产 metadata、embedding/vector adapter 和 hybrid retrieval 基础，但复杂 PDF/DOCX、OCR fallback、真实页码与完整章节层级仍未完成。

## 关键证据

- `DocumentParserService` 是当前 `rag/parser` 边界，统一输出 `ParsedDocument` / `ParsedSection`。
- PDF 当前仅轻量抽取 `Tj` / `TJ` 文本对象，页码固定为 `1`，没有 page tree、字体编码、压缩 stream、OCR。
- DOCX 当前只读取 `word/document.xml`，heading/page break 是 best-effort。
- `IndexService` 已把 parser、heading、pageNum、chunk metadata 写入 chunk 生产链路。
- RAG citation DTO 和 query log 已暴露 `pageNum` / `sectionTitle`，因此页码来源可信度会影响 citation 可信度。

## 推荐

下一步 P3-2 最小切片应是“parser 能力分级与页码/章节来源 metadata”，不新增依赖，不声称真实 OCR/PDF 生产解析完成。

建议后续新增 metadata：

- `parserCapability`
- `pageSource`
- `headingSource`

真实 PDF/DOCX/OCR 依赖必须另行走 dependency review。
