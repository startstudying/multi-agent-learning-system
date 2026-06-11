# P3-2-E RAG Parser Layout / Page Hierarchy - 集成评审

## 1. 集成决策

本轮执行 **P3-2-E：RAG parser layout/page hierarchy 最小增强**。

选择理由：

- 直接推进 `docs/planning/backend-architecture-todolist.md` 中 P3-2 未完成项。
- 不新增依赖，不触碰 API/DB/frontend，风险可控。
- RAG/Parser 专家、Dependency 专家、Security 专家结论一致：OCR 与真实 VectorDB 应单独切片。

## 2. 本轮范围

- PDF：简单多页 best-effort 文本 section，避免所有 PDF 文本固定 `pageNum=1`。
- DOCX：保留 `w:tab` 与非分页 `w:br` 的文本分隔。
- 测试：parser focused + index metadata 回归。
- 文档：PRD/REQ/SPEC/PLAN/TASK/CONTEXT/Evidence/Acceptance/Memory/TODO。

## 3. 非目标

- 不实现 OCR fallback。
- 不接真实 parser SDK。
- 不接真实 VectorDB。
- 不改公开 API、DB schema、frontend。

## 4. 执行模式

- 并行级别：L1 并行分析。
- 实现方式：Main Codex 串行 TDD。
- 允许修改文件由 Context Pack 约束。
