# PRD - P3-2-H Configurable OCR Fallback Provider

## 1. 问题陈述

P3-2-F 已建立 `OcrFallbackService` / `OcrFallbackResult` / `NoopOcrFallbackService` 合同，P3-2-G 已把 PDFBox/POI 真实解析器接入 `rag/parser` provider boundary。当前仍缺少一个显式的 Spring Boot OCR fallback 配置边界：无法通过配置表达“默认关闭 OCR”“启用但 provider 不可用”“provider 失败”“fake/provider 成功”等状态，也可能在未来接入真实 OCR provider 时出现多个 `OcrFallbackService` bean 的注入歧义。

本切片目标是补齐可配置 OCR fallback provider boundary，而不是交付真实 OCR 能力。

## 2. 目标用户

| 用户 | 需求 |
|---|---|
| 教师 | 上传扫描型 PDF 时，系统明确知道当前 OCR 处于关闭或不可用状态，而不是误把 raw bytes 当文本 |
| 学生 | 不因 OCR 缺失产生伪文本、伪引用或不可解释的 RAG 内容 |
| 运维 / 安全 | OCR 默认关闭，真实 OCR 依赖必须后续单独审查 |
| 开发者 | 未来可在 `rag/parser` boundary 内接入真实 OCR provider，不污染 `IndexService` |

## 3. MVP 范围

纳入：

- 新增 `learning-os.rag.parser.ocr.enabled` / `provider` 配置。
- 新增可配置 OCR fallback service，默认返回 `DISABLED / OCR_DISABLED`。
- 启用但 provider 缺失时返回 `UNAVAILABLE / OCR_PROVIDER_UNAVAILABLE`。
- fake provider 成功时可被 PDF fallback 使用，生成 OCR section。
- provider 异常被收敛为 `FAILED / OCR_PROVIDER_FAILED / ""`，不泄漏 raw exception。
- TDD tests、focused / adjacent / full verification。

不纳入：

- Tess4J / Tesseract / JNA / OpenCV / cloud OCR SDK。
- 新增 Maven 依赖。
- DB migration、API、frontend、retrieval/citation/VectorDB 改动。
- 工业级 PDF layout/table/TOC/reading order。
- 声明真实 OCR 生产能力完成。

## 4. 成功指标

| 指标 | 目标 |
|---|---|
| 默认安全 | 默认配置下 OCR disabled，不外呼、不抽取、不新增依赖 |
| 可配置性 | 启用但 provider 缺失时返回稳定 unavailable 状态 |
| 成功路径 | fake provider 可证明 PDF image-only fallback 能使用 OCR 文本生成 section |
| 安全失败 | provider raw 异常不进入 result、task error 或 parser exception message |
| 边界稳定 | `IndexService` / API / DB / frontend 不变 |

## 5. 审批

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-09 | Approved for spec-first implementation |

