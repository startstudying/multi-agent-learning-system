# RUN-20260608-rag-parser-hardening-integration-review

## 结论

P3-2-C 采用 **无新增依赖 parser hardening** 路径。实现只触达 `rag/parser` 与必要 parser/index 测试，不引入真实 OCR、PDF/DOCX SDK、schema、API 或前端变更。

## 已整合报告

| 报告 | 角色 | 结论 |
|---|---|---|
| `docs/subagents/runs/RUN-20260608-rag-parser-hardening-agent-rag.md` | Agent/RAG Expert | parser 边界正确，需补 PDF raw fallback、DOCX heading/page-break、zip 资源上限 |
| `docs/subagents/runs/RUN-20260608-rag-parser-hardening-security-quality.md` | Security & Quality | 有条件通过；最终验收前必须补 ZIP/DOCX 防护、PDF 二进制拒绝、raw exception 回归 |

## 冲突与决策

| 议题 | 分歧 | 决策 |
|---|---|---|
| 无文本 PDF 行为 | 返回空 section 或 `DOCUMENT_PARSE_FAILED` | 返回空 section，由 `IndexService` 现有空结果路径写入 `DOCUMENT_EMPTY_OR_UNAVAILABLE`；不索引 raw bytes |
| DOCX 缺少 `word/document.xml` | 安全失败或空结果 | 保持空结果，由索引层统一判定无内容；malformed/oversized zip 走 `DOCUMENT_PARSE_FAILED` |
| DOCX pageNum | 保持 null 或 best-effort | 本切片改为 best-effort，默认从 1 开始，page break 后递增 |
| TXT/Markdown 二进制 | 空结果或 parse failure | 明显 binary/invalid UTF-8 走 `DOCUMENT_PARSE_FAILED`，避免伪装文本污染索引 |
| 是否修改 `IndexService` | 可加空解析错误语义 | 不修改生产 `IndexService`；现有空 draft 失败路径足够 |

## 最终范围

### 纳入

- PDF：移除 raw binary fallback，支持 `Tj` 与简单 `TJ` array 抽取。
- DOCX：限制 zip entry 数与 `word/document.xml` 最大读取字节数。
- DOCX：按 `<w:p>` 分段，识别 `Heading1` 到 `Heading6`，维护 heading path。
- DOCX：识别 page break 做 best-effort `pageNum`。
- TXT/Markdown：增加明显二进制/损坏 UTF-8 内容拒绝。
- 测试：RED/GREEN 覆盖 parser happy path、安全失败、index chunk/metadata 回写。

### 排除

- 真实 OCR。
- Apache PDFBox / POI / Tika / docx4j / iText / Tesseract / 云解析 SDK。
- 新依赖、新 schema、新 API、新前端。
- 完整复杂 PDF/DOCX 工业级解析。

## Architecture Drift

实施前检查：

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | parser 保持在 `rag/parser`，`IndexService` 只消费 parser 输出 |
| Frontend rules | PASS | 不触达前端 |
| Agent / RAG rules | PASS | 不改变 retrieval/citation/trace 规则 |
| Security | PASS | 不新增依赖；parser 输入按不可信数据处理 |
| API / Database | PASS | 不新增 endpoint，不新增 migration |

## Implementation Mode

单 Codex 实施。Subagent 仅做 L1 并行分析与安全审查，不并行改代码。

