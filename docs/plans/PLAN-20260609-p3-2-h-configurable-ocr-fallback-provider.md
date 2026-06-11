# PLAN - P3-2-H Configurable OCR Fallback Provider

## 1. 追踪

- PRD：`docs/product/PRD-20260609-p3-2-h-configurable-ocr-fallback-provider.md`
- REQ：`docs/requirements/REQ-20260609-p3-2-h-configurable-ocr-fallback-provider.md`
- SPEC：`docs/specs/SPEC-20260609-p3-2-h-configurable-ocr-fallback-provider.md`
- TASK：`docs/tasks/TASK-20260609-p3-2-h-configurable-ocr-fallback-provider.md`
- Context：`docs/context/CONTEXT-20260609-p3-2-h-configurable-ocr-fallback-provider.md`
- Dependency Review：`docs/security/DEPENDENCY-REVIEW-20260609-p3-2-h-configurable-ocr-fallback-provider.md`
- Subagents：
  - `docs/subagents/runs/RUN-20260609-p3-2-h-configurable-ocr-fallback-provider-architecture.md`
  - `docs/subagents/runs/RUN-20260609-p3-2-h-configurable-ocr-fallback-provider-security.md`

## 2. Skill Selection Report

### Task Type

RAG parser provider boundary / configurable backend slice。

### Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 项目强制 spec-first 流程 |
| `educational-rag-pipeline` | OCR fallback 属于 RAG document indexing parser 层 |
| `rag-parser-boundary` | 约束 parser provider、safe error、raw fallback 禁止事项 |
| `security-review` | OCR 涉及不可信文件、隐私、外部 provider 风险 |
| `dependency-review` | 明确本切片不新增 OCR 依赖 |
| `test-driven-development` | 新行为必须 RED/GREEN |
| `architecture-drift-check` | 防止配置边界扩散到 `IndexService` |
| `verification-before-completion` | 完成声明前 fresh verification |
| `Confidence Check` | 实现前确认无重复、边界清晰、可实施 |

### Missing Skills

无。

### GitHub Research Needed

No。本切片不新增真实 OCR SDK，不需要 GitHub Reference Gate。

### New Project-Specific Skill To Create

暂不创建；完成后如沉淀通用规则，扩展 `rag-parser-boundary`。

## 3. Multi-Expert Subagent Gate

| Item | Decision |
|---|---|
| Use Subagents | Yes |
| Reason | RAG + security/dependency boundary |
| Parallelism Level | L1 analysis |
| Selected Subagents | Architecture expert, Security expert |
| Implementation Mode | Main Codex 单任务 TDD |

## 4. Confidence Check

| Check | Status | Notes |
|---|---|---|
| No duplicate implementation | PASS | 已有 OCR contract/noop，但无 configurable provider selector |
| Architecture compliance | PASS | 只在 `rag/parser` 与 config boundary 内变更 |
| Official documentation verified | N/A | 不新增 SDK/API，沿用 Spring Boot properties 现有项目模式 |
| Working OSS implementation referenced | N/A | 不接真实 OCR SDK |
| Root cause identified | PASS | 缺少显式 OCR enable/provider 配置与未来 provider 选择边界 |

Confidence：0.92，可进入实现。

## 5. 实施阶段

| Phase | Description | Status |
|---|---|---|
| 1 | 创建 PRD/REQ/SPEC/PLAN/TASK/CONTEXT/Dependency/Subagent docs | Completed |
| 2 | RED：新增 configurable OCR fallback tests | Completed |
| 3 | GREEN：新增配置类、provider SPI、configurable service | Completed |
| 4 | 更新 application.yml/test.yml | Completed |
| 5 | Focused / adjacent / full verification | Completed |
| 6 | Evidence / Acceptance / Changelog / Memory / TODO / Retro | Completed |

## 6. 风险与缓解

| Risk | Mitigation |
|---|---|
| 多个 `OcrFallbackService` bean 注入歧义 | 真实 provider 使用 `OcrFallbackProvider` SPI，Spring 对外仅注入 configurable `OcrFallbackService` |
| OCR 依赖范围膨胀 | 本切片不改 `pom.xml`，dependency review 写明无新增依赖 |
| raw provider error 泄漏 | configurable service catch exception 并返回固定 `OCR_PROVIDER_FAILED` |
| `IndexService` 边界漂移 | Context Pack 禁止修改 `IndexService` |

## 7. 依赖审批

本 PLAN 不批准任何新增 Maven dependency。

## 8. Verification Summary

- RED observed：`testCompile` 缺少 `RagParserOcrProperties`、`ConfigurableOcrFallbackService`、`OcrFallbackProvider`。
- Focused：`15 run, 0 failures, 0 errors, 0 skipped`。
- Adjacent：`33 run, 0 failures, 0 errors, 0 skipped`。
- Dependency tree：无 Tess4J/JNA/Bytedeco/云 OCR SDK 匹配条目。
- Compile：build success。
- Full backend：`385 run, 0 failures, 0 errors, 1 skipped`。
