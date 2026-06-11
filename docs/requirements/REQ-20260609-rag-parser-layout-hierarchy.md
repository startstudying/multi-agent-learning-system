# REQ - P3-2-E RAG Parser Layout / Page Hierarchy

## 1. 追踪

- PRD：`docs/product/PRD-20260609-rag-parser-layout-hierarchy.md`
- 需求编号：REQ-20260609-rag-parser-layout-hierarchy

## 2. 功能需求

| 编号 | 需求描述 | 优先级 | 验收标准 |
|---|---|---|---|
| FR-01 | PDF parser 对简单多页 PDF 文本对象做 best-effort 页码识别 | 必须 | 测试中 page 1 / page 2 文本分别输出 `pageNum=1/2` |
| FR-02 | PDF parser 无文本对象时继续返回空 section | 必须 | 不索引 raw `%PDF` / object stream |
| FR-03 | DOCX parser 将 `w:tab` 转为空格分隔 | 必须 | `A<w:tab/>B` 输出 `A B` |
| FR-04 | DOCX parser 将非分页 `w:br` 转为文本分隔 | 必须 | `A<w:br/>B` 输出 `A B` |
| FR-05 | DOCX parser 保持 page break 递增 `pageNum` | 必须 | `w:br w:type="page"` 后正文页码递增 |
| FR-06 | parser 输出的页码/章节 metadata 继续进入 chunk | 必须 | `IndexServiceTest` 覆盖 PDF/DOCX metadata |

## 3. 非功能需求

| 编号 | 需求描述 | 优先级 |
|---|---|---|
| NFR-01 | 不新增 Maven dependency | 必须 |
| NFR-02 | 不修改 DB schema / migration | 必须 |
| NFR-03 | 不修改公开 API / frontend | 必须 |
| NFR-04 | 不持久化 raw parser error、路径、storage key、原文片段或 secret | 必须 |
| NFR-05 | 测试需覆盖 focused 和 adjacent regression | 必须 |

## 4. 边界情况

| 场景 | 预期行为 |
|---|---|
| PDF 无 page marker 但有文本 | 保守输出 `pageNum=1` |
| PDF 简单 page marker 后有文本 | 输出对应 best-effort pageNum |
| PDF 扫描件 / 无文本 operator | 返回空 sections |
| DOCX tab / line break | 不粘连文本 |
| DOCX page break | pageNum 递增 |

## 5. 审批

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-09 | Accepted |
