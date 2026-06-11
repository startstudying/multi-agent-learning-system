# PRD - P3-2-E RAG Parser Layout / Page Hierarchy

## 1. 问题陈述

P3-2-C 已完成无依赖 parser hardening，但当前 PDF 页码仍主要固定为 `1`，DOCX 行内 `w:tab` / 非分页 `w:br` 等布局分隔会在文本抽取时丢失。由于 `pageNum` / `sectionTitle` 会进入 chunk、citation 和 RAG query log，这些 best-effort 元数据会直接影响学习资料引用可信度。

本切片目标是在不新增依赖、不改变 API/DB/frontend 的前提下，小步增强 parser layout/page hierarchy，降低简单 PDF 多页资料和 DOCX 行内布局被误处理的概率。

## 2. 目标用户

| 用户 | 角色 | 核心需求 |
|---|---|---|
| 教师 | 课程资料上传者 | 简单 PDF/DOCX 上传后，页码和章节引用尽量可用 |
| 学生 | RAG 问答使用者 | citation 中的页码/章节不明显误导 |
| 运维/管理员 | 平台治理者 | parser 增强不引入未审查依赖或资源风险 |

## 3. 用户故事

- 作为教师，我希望简单多页 PDF 的不同页文本能带上不同 `pageNum`，以便学生看到更准确的引用位置。
- 作为教师，我希望 DOCX 中 tab 和换行不会把词粘在一起，以便索引文本更可读。
- 作为管理员，我希望本次增强不新增大型 parser/OCR 依赖，以保持部署和安全边界稳定。

## 4. MVP 范围

### 纳入范围

- PDF simple page boundary best-effort：对简单 page object / marker 下的 `Tj` / `TJ` 文本输出对应页码。
- PDF 无可抽取文本时继续返回空 sections，不 fallback raw bytes。
- DOCX `w:tab` 作为空格处理。
- DOCX 非分页 `w:br` 作为换行/空格分隔处理；`w:br w:type="page"` 继续递增页码。
- 保持 chunk metadata 中 `headingPath` / `headingLevel` / `pageNum` 回写。

### 非目标

- 不实现真实 OCR。
- 不接入 PDFBox / POI / Tika / docx4j / iText / Tesseract / 云 OCR。
- 不新增 Maven dependency。
- 不新增 DB migration。
- 不修改公开 API 或 frontend。
- 不宣称复杂 PDF/DOCX 工业级解析完成。

## 5. 成功指标

| 指标 | 目标 | 衡量方式 |
|---|---|---|
| PDF simple multi-page | page 1 / page 2 可区分 | `DocumentParserServiceTest` |
| DOCX tab/line break | 文本分隔不粘连 | `DocumentParserServiceTest` |
| parser 安全边界 | 不恢复 raw fallback | 既有 PDF no-text 测试继续通过 |
| index metadata | pageNum 进入 chunk | `IndexServiceTest` |
| 依赖/API/DB drift | 无 | dependency/tree 或文件审查 |

## 6. 审批

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-09 | Approved for implementation |
