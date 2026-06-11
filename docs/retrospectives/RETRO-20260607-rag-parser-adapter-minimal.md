# Retrospective - RAG Parser Adapter 最小生产切片

## 1. Feature Summary

完成了 RAG 文档解析边界抽取：`IndexService` 只保留索引编排，Markdown / TXT / PDF / DOCX 解析进入 `rag/parser` 独立服务，worker/manual 索引路径继续共用同一边界。

## 2. What Went Well

- 先写 parser 边界测试，再抽取实现，RED-GREEN 路径清晰。
- 现有 chunk/hash/heading 逻辑可以直接复用，改动面很窄。
- 安全错误码在 `IndexService` 里统一收口，避免 parser 细节泄露。

## 3. What Didn't Go Well

- 额外的 code-review subagent 因线程上限无法派发，只能用本地 diff 和测试结果收口。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| 先抽 parser 边界，再保留索引编排与 chunk/hash 逻辑 | Yes | docs/skills/project-specific/rag-parser-boundary.md |
| 解析失败统一映射为安全错误码 | Yes | docs/skills/project-specific/rag-parser-boundary.md |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Workflow | 先完成设计文档，再做 TDD 与实现 | 继续保持，但把 code-review subagent 的可用性作为前置检查 |
| Testing | parser + 索引回归通过 | 后续增加更明确的 PDF/DOCX 边界样本 |
| Documentation | PRD / REQ / SPEC / PLAN / TASK / Context / Evidence / Acceptance 已补齐 | 继续把完成状态写回 TODO 和 memory |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 将 P3-2 parser adapter item 勾选完成 | Main Codex | `TASK-20260607-rag-parser-adapter-minimal` |
| 观察后续 OCR / 真 parser 接入时的安全错误码兼容性 | Main Codex | `TASK-20260607-rag-parser-adapter-minimal` |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] Domain memory file
- [x] SKILL_REGISTRY.md
- [x] ARCHITECTURE_BASELINE.md（无变更，架构漂移检查通过）
