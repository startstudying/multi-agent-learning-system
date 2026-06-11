# SPEC - P3-2-C RAG 无依赖 Parser 加固

## 1. 概述

本规格定义 RAG 文档 parser 的无依赖安全加固。目标是在不新增依赖、不改变 API/schema/frontend 的前提下，提升 PDF/DOCX/TXT/Markdown 轻量解析的安全性和结构化输出质量。

## 2. 追踪

- PRD：`docs/product/PRD-20260608-rag-parser-hardening.md`
- REQ：`docs/requirements/REQ-20260608-rag-parser-hardening.md`

## 3. 领域模型

沿用现有模型：

- `DocumentParser`
- `ParsedDocument`
- `ParsedSection`
- `DocumentParserService`
- `DocumentParseException`

不新增数据库实体，不新增 API DTO。

## 4. API 契约

本切片不新增或修改公开 API。

## 5. 前端交互

不触达前端。

## 6. 后端流程

```text
IndexService.processIndexTask
-> storageService.read(...)
-> DocumentParserService.parse(document, bytes)
-> ParsedDocument.sections()
-> IndexService.splitAndDeduplicate(...)
-> toChunks(...)
-> chunk metadata 持久化 parser / headingPath / headingLevel / pageNum
```

### 6.1 PDF parser

- 输入 bytes 用 ISO-8859-1 转为轻量 raw view，仅用于查找 PDF text operators。
- 支持：
  - `(...) Tj`
  - `[(...) (...) ...] TJ`
- 对 PDF escaped chars 做最小解码：
  - `\\(` -> `(`
  - `\\)` -> `)`
  - `\\\\` -> `\`
- 若无法抽取有效文本，返回空 section。
- 禁止把 raw PDF 内容作为文本 fallback。
- `pageNum` 仍为 best-effort，当前默认为 `1`；如后续支持 page marker，可独立扩展。

### 6.2 DOCX parser

- 只读取 `word/document.xml`。
- 先解析 ZIP EOCD 与 central directory 统计 entry 数并定位 `word/document.xml`，再按 local header 读取目标 entry。
- 不遍历或解压非目标 entry body，避免为统计 entry 数隐式 inflate 其他 entry。
- 校验 central directory 与 local header 的 `word/document.xml` 名称一致；不支持 ZIP64、分卷 ZIP、加密 ZIP 或非 `stored/deflated` 方法。
- 限制：
  - 最大 zip entry 数：实现常量定义。
  - `word/document.xml` 最大解压读取字节数：实现常量定义。
- 使用 regex 做轻量解析，不引入 XML parser 或 DOCX SDK。
- 按 `<w:p>` 分段。
- 识别 `<w:pStyle ... w:val="HeadingN">`，`N` 范围为 1 到 6。
- heading paragraph 只更新 heading stack，不单独产生内容 section。
- 非 heading paragraph 产生 `ParsedSection`，继承当前 heading metadata。
- 识别 `<w:lastRenderedPageBreak/>` 与 `<w:br w:type="page"/>`，对后续内容递增 best-effort `pageNum`。
- XML entity 使用现有 `unescapeXml` 规则。

### 6.3 TXT / Markdown parser

- UTF-8 解码。
- 若明显包含二进制垃圾或损坏 UTF-8 replacement char，抛出 `DocumentParseException("DOCUMENT_PARSE_FAILED")`。
- Markdown 支持 UTF-8 BOM 清理，确保首个 heading 可识别。
- 保持空内容不产生 section 的行为。

## 7. Agent 工作流

不涉及 Agent 编排变更。

## 8. RAG 工作流

```text
Document upload
-> parse
-> chunk
-> embed/vector boundary
-> retrieval
-> citation
```

本切片只修改 parse 层；不改变 retrieval、citation、embedding、vector adapter。

## 9. 数据库变更

无数据库变更。

## 10. 状态流转

沿用 `IndexService` 现有状态：

```text
PARSING -> CHUNKING -> EMBEDDING -> VECTOR_UPSERT -> INDEXED
PARSING/CHUNKING -> FAILED
```

错误码：

| 错误码 | 说明 | 触发条件 |
|---|---|---|
| `DOCUMENT_PARSE_FAILED` | parser 安全失败 | DOCX malformed/超限、TXT/Markdown 二进制、parser 预期格式错误 |
| `DOCUMENT_EMPTY_OR_UNAVAILABLE` | 无可索引内容 | PDF 无可抽取文本、DOCX 无正文、空文本 |

## 11. 错误处理

- parser 预期格式错误统一用 `DocumentParseException("DOCUMENT_PARSE_FAILED")`。
- `IndexService` 不持久化 raw cause；task/document error 只写 safe code。
- 不记录 bucket、storage key、文件路径、原文片段、secret。

## 12. 权限规则

不改变权限规则。RAG KB 读写权限、document detail anti-enumeration、query strict `kbIds` 均保持原状。

## 13. Trace / 日志

不新增 trace 字段。索引任务错误信息继续使用安全错误码。

## 14. 测试策略

- `DocumentParserServiceTest`
  - RED/GREEN 覆盖 PDF raw fallback 移除。
  - RED/GREEN 覆盖 PDF `TJ` array。
  - RED/GREEN 覆盖 DOCX heading/page break。
  - RED/GREEN 覆盖 DOCX zip/XML 上限。
  - RED/GREEN 覆盖 TXT/Markdown binary 拒绝。
- `IndexServiceTest`
  - 验证无文本 PDF 不生成 chunk。
  - 验证 DOCX pageNum/headingPath/metadata 回写。
- `IndexServiceParserFailureTest`
  - 验证 sensitive cause 不泄露。
- 全量 `mvn test`。

## 15. 验收清单

- [x] PRD / REQ / SPEC / PLAN / TASK / Context Pack 已创建
- [x] PDF 无文本不索引 raw bytes
- [x] PDF `Tj` / `TJ` 有测试覆盖
- [x] DOCX heading/page break 有测试覆盖
- [x] DOCX zip/XML 上限有测试覆盖
- [x] DOCX 只读取 `word/document.xml`，不解压非目标 entry body
- [x] TXT/Markdown 二进制拒绝有测试覆盖
- [x] parser failure 不泄露 raw exception
- [x] 不新增 dependency/schema/API/frontend
- [x] focused/adjacent/full tests 已运行并记录到 Evidence

## 16. 完成记录

- 完成日期：2026-06-08
- Evidence：`docs/evidence/EVIDENCE-20260608-rag-parser-hardening.md`
- Acceptance：`docs/acceptance/ACCEPT-20260608-rag-parser-hardening.md`
- Code Review：Feynman reviewer 最终结论 `APPROVE`，无 Critical/Important/Minor 遗留项。
