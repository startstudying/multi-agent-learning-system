# PRD - P3-2-I Real OCR Provider

## 1. 问题陈述

P3-2-H 已完成可配置 OCR fallback provider boundary：系统可以通过配置启用 OCR fallback，并在 provider 缺失或失败时返回稳定安全状态。但当前仍没有一个真实 provider 实现，扫描型 PDF 即使启用 OCR 也只能依赖 fake provider 测试路径。

本切片目标是在不新增 Maven OCR/native/cloud 依赖的前提下，提供一个最小可用的外部命令型 OCR provider，使部署环境可以通过显式配置接入本地 OCR 工具，同时保持默认关闭、安全失败和 parser boundary 不漂移。

## 2. 目标用户

| 用户 | 需求 |
|---|---|
| 教师 | 上传扫描型 PDF 时，部署环境配置 OCR 后可抽取文本进入 RAG 索引 |
| 学生 | OCR 文本可作为普通 RAG chunk 被检索和引用；失败时不产生伪文本 |
| 运维 / 安全 | OCR 默认关闭，外部命令显式配置，失败不泄露路径、stderr、secret 或原文 |
| 开发者 | OCR provider 只实现 `OcrFallbackProvider`，不污染 `IndexService` 或查询链路 |

## 3. MVP 范围

纳入：

- 新增 process-based `OcrFallbackProvider`。
- 扩展 `learning-os.rag.parser.ocr.*` 配置以支持外部命令路径、超时和输出限制。
- 默认仍关闭 OCR。
- 命令未配置时返回 `UNAVAILABLE / OCR_PROVIDER_UNAVAILABLE`。
- 命令成功且输出文本时返回 `SUCCEEDED / OCR_PROVIDER_SUCCEEDED`。
- 命令失败、超时或异常时返回 `FAILED / OCR_PROVIDER_FAILED`。
- image-only PDF 可通过 process provider 生成 OCR section。
- 不泄露 stderr、命令路径、临时路径、secret 或 raw exception。

不纳入：

- Tess4J / JNA / OpenCV / cloud OCR SDK。
- Maven dependency 变更。
- DB migration / API / frontend 变更。
- `IndexService` / retrieval / citation / embedding / VectorDB 变更。
- 工业级 PDF/DOCX layout/table/TOC/reading order。
- 页级 OCR confidence 或多页 OCR metadata。

## 4. 成功指标

| 指标 | 目标 |
|---|---|
| 默认安全 | 默认配置不调用外部命令 |
| 可运行 provider | 显式配置 process provider 和命令后可得到 OCR text |
| 安全失败 | command missing/failure/timeout 均返回固定安全状态 |
| 边界稳定 | 只修改 parser/config/test/docs，不改索引与查询合同 |
| 依赖可控 | 不新增 Maven OCR/native/cloud dependency |

## 5. 审批

| 角色 | 日期 | 状态 |
|---|---|---|
| Architecture Expert | 2026-06-09 | Approved with constraints |
| Security Expert | 2026-06-09 | Approved with constraints |
| Main Codex | 2026-06-09 | Approved for spec-first implementation |
