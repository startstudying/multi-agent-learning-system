# SPEC - P3-2-E RAG Parser Layout / Page Hierarchy

## 1. 概述

本规格定义 RAG parser layout/page hierarchy 的无新增依赖最小增强。目标是在保持现有 `rag/parser` 边界、API、DB、frontend 不变的前提下，补齐简单 PDF 多页 best-effort 页码与 DOCX 行内布局文本分隔。

## 2. 追踪

- PRD：`docs/product/PRD-20260609-rag-parser-layout-hierarchy.md`
- REQ：`docs/requirements/REQ-20260609-rag-parser-layout-hierarchy.md`
- 专家报告：
  - `docs/subagents/runs/RUN-20260609-rag-parser-layout-hierarchy-agent-rag.md`
  - `docs/subagents/runs/RUN-20260609-rag-parser-layout-hierarchy-vector-dependency.md`
  - `docs/subagents/runs/RUN-20260609-rag-parser-layout-hierarchy-security.md`
  - `docs/subagents/runs/RUN-20260609-rag-parser-layout-hierarchy-integration.md`

## 3. 领域模型

沿用：

- `DocumentParser`
- `ParsedDocument`
- `ParsedSection`
- `DocumentParserService`
- `IndexService`

不新增数据库实体或公开 DTO。

## 4. Parser 行为

### 4.1 PDF

- 继续只抽取明确 text operators：
  - `(...) Tj`
  - `[(...) ...] TJ`
- 对简单 PDF page boundary 做 best-effort：
  - 识别 `/Type /Page` 或等价简单 marker 后，将后续文本归入递增页码。
  - 若无法识别 page boundary，则回退为单 section，`pageNum=1`。
- 无可抽取文本时返回空 section。
- 禁止 raw PDF bytes fallback。

### 4.2 DOCX

- 继续只读取 `word/document.xml`。
- `w:tab` 转为空格分隔。
- 非分页 `w:br` 转为空格/换行分隔。
- `w:lastRenderedPageBreak` 和 `w:br w:type="page"` 继续递增 best-effort `pageNum`。
- `Heading1`-`Heading6` heading stack 行为保持不变。

## 5. Index 行为

- `IndexService` 继续从 `ParsedSection.pageNum()` 写入 `KbDocChunk.pageNum`。
- `metadataJson` 继续包含 `parser`、`headingLevel`、`headingPath`、chunking/embedding/vector 短状态。
- 不写 raw vector、raw parser error、文件路径、storage key、原文片段。

## 6. API / DB / Frontend

- 不修改公开 API。
- 不新增或修改 DB migration。
- 不修改 frontend。

## 7. 错误处理

- 解析异常仍统一映射为 `DocumentParseException("DOCUMENT_PARSE_FAILED")`。
- 空内容仍由索引层映射为 `DOCUMENT_EMPTY_OR_UNAVAILABLE`。

## 8. 测试策略

```powershell
cd backend
mvn --% -Dtest=DocumentParserServiceTest test
mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest test
mvn --% -Dtest=DocumentParserServiceTest,IndexServiceTest,IndexServiceParserFailureTest,RagQueryServiceTest test
mvn test
```

## 9. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | parser 逻辑仍在 `rag/parser`；`IndexService` 只消费 `ParsedSection` |
| Frontend | PASS | 不修改 frontend |
| Agent/RAG | PASS | 不改变 retrieval/citation/trace 规则 |
| Security | PASS | 不新增依赖，不恢复 raw PDF fallback |
| API/DB | PASS | 不改公开 API / schema |

## 10. 非目标

- OCR fallback。
- 真实 PDF/DOCX SDK。
- 真实 VectorDB。
- 工业级页码/章节识别。

## 11. 实施与验证状态

- 状态：已完成。
- 完成日期：2026-06-09。
- 验证证据：`docs/evidence/EVIDENCE-20260609-rag-parser-layout-hierarchy.md`。
- 验收报告：`docs/acceptance/ACCEPT-20260609-rag-parser-layout-hierarchy.md`。
- 全量后端验证：`mvn test`，361 run, 0 failures, 0 errors, 1 skipped。
