# P3-2 子任务：DOCX table/TOC reading-order provider 需求说明

## 背景

`backend-architecture-todolist.md` 中 P3-2 仍有一项未完成：工业级 PDF/DOCX layout/table/TOC/reading-order、native/cloud OCR、OCR confidence 与真实渲染页码增强。

前置切片已经完成 parser metadata contract foundation：`ParsedSection`、`ParsedDocument`、`OcrFallbackResult` 和 `IndexService.metadataJson` 已支持 `pageNumSource`、`readingOrderIndex`、`contentKind`、`layoutConfidence`、`ocrConfidence`。

本切片继续推进 DOCX 方向，但不把整个 P3-2 父项一次性关闭。

## 目标

增强 DOCX 解析，使 DOCX body 中的 paragraph 与 table 能按读取顺序进入 parser section，并避免 TOC-like paragraph 污染 RAG chunk。

## 用户价值

- 课程资料中的表格内容可以被索引和检索。
- table chunk 保留与正文相邻的读取顺序，后续 RAG 引用更接近文档结构。
- Word 目录不再作为正文重复进入检索候选，降低检索噪声。

## 功能需求

1. DOCX provider 必须按 body order 遍历 paragraph 与 table。
2. 普通 paragraph 保持现有 heading、page break、separator 行为。
3. table 必须输出为独立 section，使用 `contentKind=TABLE_TEXT`。
4. table 文本必须稳定、可检索，并保留行列顺序。
5. table section 必须沿用当前 heading context。
6. TOC-like paragraph 默认跳过，不生成 section。
7. `readingOrderIndex` 必须继续由 `ParsedDocument` 自动生成，体现最终 section 顺序。
8. chunk metadata 必须传播 `contentKind`、`readingOrderIndex`、`pageNumSource`。

## 非目标

- 不实现复杂多栏 layout reconstruction。
- 不实现 DOCX 合并单元格语义、坐标、样式、嵌套表格完整还原。
- 不实现真实渲染页码。
- 不实现 native/cloud OCR。
- 不新增依赖。
- 不改 REST API、DTO、DB schema、frontend、VectorDB、retrieval/citation 合同。

## 验收口径

本切片完成后，P3-2 父项仍保持 open。完成范围只记为：

```text
P3-2 子任务：DOCX table/TOC reading-order provider
```

剩余 P3-2 工作包括 PDF layout/table/TOC provider、native/cloud OCR provider、provider confidence pipeline、真实渲染页码映射、真实 VectorDB adapter。
