# PRD - P3-2-G Real PDF/DOCX Parser SDK Provider

## 1. 问题陈述

P3-2-E/P3-2-F 已完成 lightweight PDF/DOCX 解析增强与 provider boundary，但当前 PDF/DOCX 仍依赖手写 best-effort 文本/XML 解析，无法覆盖真实 PDF 文本提取、真实 DOCX 段落样式、页级元数据和复杂文档失败场景。

本切片目标是在既有 `DocumentFormatParser` 边界后接入真实 Apache PDFBox 与 Apache POI DOCX provider，提升 RAG 索引的真实文档解析能力，同时保持索引编排、API、DB、frontend、retrieval/citation 合同不变。

## 2. 目标用户

| 用户 | 需求 |
|---|---|
| 教师 | 上传真实 PDF/DOCX 课程资料后能提取可检索文本 |
| 学生 | RAG 引用页码和章节元数据更稳定 |
| 运维/安全 | 新增 parser SDK 有依赖审查、资源限制和安全错误码 |
| 开发者 | 后续 OCR 可以独立接入，不污染 `IndexService` |

## 3. MVP 范围

纳入：

- 新增 PDFBox PDF provider。
- 新增 POI DOCX provider。
- 新增 parser 资源限制与文本清洗 helper。
- `backend/pom.xml` 新增必要依赖。
- TDD tests、adjacent tests、full backend verification。

不纳入：

- 真实 OCR。
- Tika / docx4j / iText / Tess4J。
- PDF embedded files / attachments。
- DB migration、API、frontend、retrieval/citation/VectorDB 变更。

## 4. 成功指标

| 指标 | 目标 |
|---|---|
| PDF | 真实 PDFBox 生成的多页 PDF 可提取文本并保留 `pageNum` |
| DOCX | 真实 POI 生成的 DOCX 可识别 Heading1-6、正文、page break |
| 安全 | 损坏/超限文档只暴露 `DOCUMENT_PARSE_FAILED` |
| 边界 | `IndexService` 无格式解析变更 |
| 依赖 | 依赖审查文档存在且明确版本/许可证/风险 |

## 5. 审批

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-09 | Approved for implementation |

