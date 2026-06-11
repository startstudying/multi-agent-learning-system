# PLAN-20260610 P3-2 子任务：industrial parser reading-order/confidence/page-number metadata

## 1. Skill Selection Report

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | P3-2 TODO 推进，必须走项目 S/M/L 工作流。 |
| `educational-rag-pipeline` | 任务涉及文档解析、chunk、index metadata。 |
| `rag-parser-boundary` | 必须保持 parser provider 边界，不把格式解析扩散到 IndexService。 |
| `test-driven-development` | 先补 parser/index metadata RED，再做最小实现。 |
| `security-review` | 确保不写 raw OCR/provider/secret payload。 |
| `Confidence Check` | 实现前确认方向、重复实现、架构边界和根因。 |

Missing Skills：无。

GitHub research needed：No。本切片不新增 SDK/依赖，不需要外部参考；后续 native/cloud OCR provider 需要。

New project-specific skill：暂不创建。完成后如形成可复用规则，再更新 `rag-parser-boundary`。

## 2. Size Classification

Size：M - Standard Feature Slice。

原因：

- 修改后端 parser contract 与 IndexService metadata，两处相关模块。
- 不改公开 API/DTO/DB schema/dependency/frontend，因此不升级 L。
- 不是单文件小修；需要 REQ/SPEC/PLAN/TASK/CONTEXT。

PRD：不需要。该切片不改变用户可见流程。

## 3. Subagent Decision

Use Subagents：Yes。

Parallelism Level：L1 Parallel Analysis。

Selected Subagents：

- RAG/parser 架构专家：已输出 P3-2 open item 拆分报告，并建议第一切片。

Implementation Mode：Single Codex implementation。

原因：核心改动集中在 parser record 与 IndexService metadata，不能并行编辑同一 contract。

## 4. Confidence Check

| Check | Result |
|---|---|
| No duplicate implementation | PASS：当前无 `pageNumSource` / `readingOrderIndex` / confidence / `contentKind` contract。 |
| Architecture compliance | PASS：沿用 `rag/parser` 和 `IndexService.metadataJson`。 |
| Official docs | N/A：不新增外部 SDK/API。 |
| OSS references | N/A：不实现 provider 算法，只扩 contract。 |
| Root cause identified | PASS：`ParsedSection` / `OcrFallbackResult` 承载不足。 |

Confidence：0.93。Proceed。

## 5. Implementation Plan

1. RED：在 parser tests 中断言 `ParsedDocument` 自动填充 `readingOrderIndex`、默认 `pageNumSource/contentKind`。
2. RED：在 OCR fallback parser test 中断言成功 OCR section 有 `contentKind=OCR_TEXT` 和 `ocrConfidence`。
3. RED：在 `IndexServiceTest` 中断言 chunk metadata 保存短 parser metadata。
4. GREEN：扩展 `ParsedSection` record，保留旧构造器。
5. GREEN：扩展 `ParsedDocument` canonical constructor，为 section 补 1-based reading order。
6. GREEN：扩展 `OcrFallbackResult` record，保留旧构造器和安全 confidence 规范化。
7. GREEN：OCR fallback 生成 `OCR_TEXT / OCR_FALLBACK / ocrConfidence` metadata。
8. GREEN：`IndexService.ChunkDraft` 与 `metadataJson` 写入短字段。
9. 运行 focused / adjacent / full backend tests。
10. 更新 Evidence / Changelog / Memory / TODO。

## 6. 风险与边界

| Risk | Mitigation |
|---|---|
| Record 构造器变更导致大量测试编译失败 | 保留旧构造器。 |
| metadata 字段被误认为工业级 layout 已完成 | 文档明确本切片只做承载 contract。 |
| confidence 异常值导致 parser 失败 | 规范化为 `null`，不抛出。 |
| provider raw 信息泄露到 chunk metadata | 只写短白名单字段。 |

## 7. Architecture Drift

预检 PASS：不改 API/DB/依赖；parser 逻辑仍在 `rag/parser`；IndexService 只消费 section metadata 并写白名单 metadata。
