# PRD - P3-2-F Parser Provider Boundary + OCR Fallback Contract

## 1. 问题陈述

P3-2-E 已完成无新增依赖的 simple PDF 页码和 DOCX layout/page best-effort 增强，但 `DocumentParserService` 内部仍通过一个大类和 `switch` 分发 Markdown、TXT、PDF、DOCX。后续如果接入真实 PDF parser、DOCX SDK 或 OCR fallback，这个类会继续膨胀，并增加 parser 失败、安全错误码、raw fallback 和索引边界漂移风险。

本切片目标是先补齐可替换 parser provider 边界和 OCR fallback contract，保持 `IndexService` 只消费 `ParsedDocument/ParsedSection`，为后续复杂 PDF/DOCX/OCR 独立切片打基础。

## 2. 目标用户

| 用户 | 角色 | 核心需求 |
|---|---|---|
| 教师 | 课程资料上传者 | 后续复杂文档解析能力可逐步增强，当前索引行为不回退 |
| 学生 | RAG 问答使用者 | citation/chunk 元数据稳定，不因 parser 重构产生错误引用 |
| 运维/管理员 | 平台治理者 | parser/OCR 依赖在安全审查前不会被隐式引入 |
| 开发者 | 后续 parser/OCR 实施者 | 可以替换或新增格式 provider，不再修改索引编排层 |

## 3. 用户故事

- 作为开发者，我希望 `DocumentParserService` 只负责格式选择和 provider 调用，以便后续接入真实 PDF/DOCX parser 时不改 `IndexService`。
- 作为管理员，我希望 OCR fallback 先有明确 disabled/noop 状态，避免 scanned PDF 被 raw bytes 或伪 OCR 文本索引。
- 作为教师，我希望本次重构不改变现有 Markdown/TXT/PDF/DOCX 最小解析行为。

## 4. MVP 范围

### 纳入范围

- 新增 `ParseInput`。
- 新增 `DocumentFormatParser` provider contract。
- 将当前 Markdown/TXT/PDF/DOCX 轻量解析拆为 provider。
- `DocumentParserService` 聚合 provider 并统一异常映射。
- 新增 `OcrFallbackService`、`OcrFallbackResult`、`NoopOcrFallbackService`。
- 保持 scanned/image-only PDF 返回空 sections，不产生 raw fallback。
- 新增/更新 focused tests。

### 非目标

- 不引入 PDFBox / POI / Tika / docx4j / iText / Tess4J。
- 不实现真实 OCR。
- 不新增 Maven dependency。
- 不新增 DB migration。
- 不修改公开 API。
- 不修改 frontend。
- 不修改 VectorDB / embedding / retrieval / citation。

## 5. 成功指标

| 指标 | 目标 | 衡量方式 |
|---|---|---|
| provider boundary | `DocumentParserService` 通过 provider registry 分发 | parser 单测 |
| OCR contract | noop OCR 返回 disabled/no text | OCR 单测 |
| image-only PDF | 不产生 raw fallback chunk | parser / index 测试 |
| 行为兼容 | Markdown/TXT/PDF/DOCX 既有行为不回退 | focused + adjacent regression |
| 架构漂移 | 不改依赖/API/DB/frontend | 文件审查 + Maven 验证 |

## 6. 审批

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-09 | Approved for implementation |
