# Retrospective - P3-2 industrial parser reading-order/confidence/page-number metadata

## 1. Feature Summary

完成 P3-2 工业 parser open item 的第一 M 切片：只扩展 parser metadata contract 与 chunk metadata 白名单，不实现真实 layout/table/TOC、native/cloud OCR 或 rendered page label。

## 2. What Went Well

- 先用专家 subagent 把大父项拆为语义子任务，避免一次性把工业 parser 做成不可控 L 级大改。
- 保留 `ParsedSection` 旧构造器和 `OcrFallbackResult` 旧构造器，降低 record 字段扩展的连锁 diff。
- `IndexService` 只写短 metadata 字段，并用测试断言不写 `rawOcrText` / `providerResponse`。
- full backend 第一次遇到无关 SSE MockMvc async race 时，没有直接忽略；先 isolated 复现再重跑 full，证据更干净。

## 3. What Didn't Go Well

- 实现中实际修改了 `ConfigurableOcrFallbackService.java`，最初 TASK/CONTEXT allowed files 未列出，后续补账。
- `OCR confidence` 这个父项措辞容易误解为真实 OCR provider confidence 已完成；文档必须明确本次只是承载和传递字段。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| Parser metadata contract 应只通过 parser section 和 chunk metadata 白名单传递，不把 raw provider/OCR payload 写入索引元数据 | Yes | `docs/skills/project-specific/rag-parser-boundary.md` |
| Full suite 遇到无关 async/flaky 失败时，应 isolated 复现、归因、重跑 full，并把失败也写入 Evidence | Yes | 暂不新建；保留在本 Retro 和 Evidence |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Workflow | Context Pack allowed files 容易漏掉辅助 service | 实现中一旦触及新文件，先停下补 TASK/CONTEXT allowed files 再继续 |
| Testing | Full suite 偶发 async race 容易污染结论 | Evidence 同时记录失败、isolated rerun、fresh full rerun |
| Documentation | 父项与子任务完成状态容易混淆 | TODO 行保留 open，并在括号中列明已完成 foundation 与未完成 provider/algorithm |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 后续补 PDF layout/table/TOC provider | Future Codex | 新 P3-2 子任务 |
| 后续补 native/cloud OCR confidence provider | Future Codex | 新 P3-2 子任务 |
| 后续补 rendered page label mapping | Future Codex | 新 P3-2 子任务 |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] BACKEND_MEMORY.md
- [x] AGENT_RAG_MEMORY.md
- [x] SKILL_REGISTRY.md：无需改，已有 `rag-parser-boundary`
- [x] ARCHITECTURE_BASELINE.md：无需改，无架构层级变化

