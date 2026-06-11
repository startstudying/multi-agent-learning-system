# P3-2 子任务：DOCX table/TOC reading-order provider 专家并行报告

日期：2026-06-10

## 任务定位

本报告汇总两个既有专家 subagent 的并行只读分析：

- RAG/parser 架构专家：`019eb08d-b823-7d53-858b-d2a4f7f3dc8a`
- 测试/验收专家：`019eb08d-6e19-7370-99da-764ad17b9e62`

新建 subagent 因线程上限失败，按项目规则复用既有专家线程。两个专家均未修改文件。

## 结论

任务大小建议为 **M**。

原因：

- 范围集中在 RAG parser 模块，但会改变 DOCX table/TOC/paragraph 的可见解析行为。
- 不新增依赖，继续使用已有 Apache POI。
- 不改 REST API、DTO、DB schema、frontend、VectorDB、retrieval/citation 合同。
- 需要 REQ / SPEC / PLAN / TASK / CONTEXT，并通过 focused、adjacent、full backend 验证。

## 集成设计

采用保守增强：

1. POI DOCX provider 从仅遍历 `document.getParagraphs()` 改为按 `document.getBodyElements()` 遍历。
2. `XWPFParagraph` 继续沿用现有 heading/page-break 逻辑。
3. `XWPFTable` 作为独立 `ParsedSection` 输出，沿用当前 heading context。
4. table 文本使用确定性纯文本格式：单元格用 ` | ` 分隔，行用 `; ` 分隔。
5. table section 的 `contentKind` 为 `TABLE_TEXT`。
6. TOC-like paragraph 默认跳过，不写入 section，避免目录污染检索。
7. `pageNumSource` 继续使用 `PARSER_INFERRED`；不声称真实渲染页码。
8. `layoutConfidence` / `ocrConfidence` 保持 `null`，避免伪造复杂 layout 置信度。

## 建议允许修改文件

生产代码：

- `backend/src/main/java/com/learningos/rag/parser/PoiDocxDocumentFormatParser.java`
- `backend/src/main/java/com/learningos/rag/parser/DocumentParserService.java`

测试：

- `backend/src/test/java/com/learningos/rag/parser/RealParserProviderTest.java`
- `backend/src/test/java/com/learningos/rag/parser/DocumentParserServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java`

文档：

- `docs/requirements/REQ-20260610-p3-2-docx-table-toc-reading-order-provider.md`
- `docs/specs/SPEC-20260610-p3-2-docx-table-toc-reading-order-provider.md`
- `docs/plans/PLAN-20260610-p3-2-docx-table-toc-reading-order-provider.md`
- `docs/tasks/TASK-20260610-p3-2-docx-table-toc-reading-order-provider.md`
- `docs/context/CONTEXT-20260610-p3-2-docx-table-toc-reading-order-provider.md`
- `docs/evidence/EVIDENCE-20260610-p3-2-docx-table-toc-reading-order-provider.md`
- `docs/acceptance/ACCEPT-20260610-p3-2-docx-table-toc-reading-order-provider.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/skills/project-specific/rag-parser-boundary.md`

## 验收标准

- DOCX provider 按 body 顺序输出 paragraph 与 table sections。
- table 内容可检索，行列顺序稳定，metadata 中 `contentKind=TABLE_TEXT`。
- TOC-like paragraph 不生成 chunk，不污染检索。
- heading context 能延续到 table section。
- `readingOrderIndex` 反映 paragraph/table 输出顺序。
- IndexService chunk metadata 传播 `contentKind` / `readingOrderIndex` / `pageNumSource`。
- 不新增依赖、不改 API/DB/frontend。
- raw XML、provider payload、OCR 原文、文件路径、secret 不进入 chunk content 或 metadata。

## 推荐验证

RED / focused：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=RealParserProviderTest,DocumentParserServiceTest,IndexServiceTest test
```

Adjacent：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentParserServiceTest,RealParserProviderTest,ProcessOcrFallbackProviderTest,ConfigurableOcrFallbackServiceTest,IndexServiceTest,IndexServiceParserFailureTest test
```

Full：

```powershell
cd D:\多元agent\backend
mvn test
```

## 风险

- 不能把本切片表述为“工业级 DOCX layout 完成”；它只是 body order + table text + TOC skip 的受控增强。
- 不处理多栏、复杂合并单元格、嵌套表格、真实渲染页码、页眉页脚、脚注、图片 OCR。
- 不应引入 Tika/docx4j/商业 SDK 或修改 RAG query/citation API。
