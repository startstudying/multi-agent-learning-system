# Retrospective - P3-2-F Parser Provider Boundary + OCR Fallback Contract

## 1. Feature Summary

完成 RAG parser provider boundary 与 disabled/noop OCR fallback contract。`DocumentParserService` 从直接 `switch` 分发改为 provider registry，新增 `ParseInput`、`DocumentFormatParser`、`OcrFallbackService`、`OcrFallbackResult`、`NoopOcrFallbackService`，并保持 `IndexService` 边界不变。

## 2. What Went Well

- RED 明确暴露缺少 contract/provider/noop OCR。
- focused、adjacent、extended adjacent、full backend 验证都通过。
- 没有引入 PDF/OCR 依赖，避免把边界重构误扩大成真实 parser/OCR 接入。
- 专家报告帮助把“真实 OCR”与“noop contract”区分清楚。

## 3. What Didn't Go Well

- 测试初版误把 `KbDocument.id` 当作 `Long`，实际为 `String`，导致一次额外编译修正。
- 当前 provider 仍是 `DocumentParserService` 内部轻量 provider，后续接真实 parser 时可再抽为顶层 bean。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| parser provider boundary before heavy parser/OCR dependency | Yes | 复用现有 `docs/skills/project-specific/rag-parser-boundary.md`，暂不新增 skill |
| disabled/noop external capability contract before real provider | Yes | 暂不新增 skill |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Workflow | P3-2 未完成项较大 | 继续按 P3-2-F/G 等小切片推进，避免一次引入大依赖 |
| Testing | RED 以编译失败验证 contract 缺失 | 后续真实 parser 接入时补资源限制、超时、malformed 文件测试 |
| Documentation | 已明确非目标 | TODO 中继续保留真实 PDF/DOCX/OCR 未完成状态 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 后续评估 PDFBox/POI 真实 parser 接入 | Main Codex | Future P3-2 slice |
| 后续评估 OCR dependency/runtime/privacy 策略 | Main Codex | Future P3-2 slice |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] Domain memory file
- [ ] SKILL_REGISTRY.md
- [ ] ARCHITECTURE_BASELINE.md

未更新 `SKILL_REGISTRY.md`：没有新增 skill。

未更新 `ARCHITECTURE_BASELINE.md`：本切片未改变系统基线，只强化 `rag/parser` 内部边界。
