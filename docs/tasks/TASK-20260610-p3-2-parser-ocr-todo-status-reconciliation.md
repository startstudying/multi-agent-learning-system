# TASK-20260610 P3-2 子任务：parser/OCR TODO 状态对账

Status: Done.

## Goal

对账 `docs/planning/backend-architecture-todolist.md` 中 P3-2 未勾选项“补齐复杂 PDF/DOCX、OCR fallback、真实页码和章节层级识别”，确认它是否已由 P3-2-E/F/G/H/I 的已验收工作达到最小生产化完成状态，并把工业级增强拆为独立后续项。

## Task Type

文档 / 计划状态对账，不新增后端功能代码。

## Selected Skills

| Skill | Why Needed |
|---|---|
| feature-development-workflow | 继续执行后端架构 TODO 总计划，并按 S/M/L 工作流收口 |
| rag-project-review | P3-2 涉及 RAG parser、OCR fallback、page metadata 和索引链路 |
| rag-parser-boundary | 项目已有 parser/OCR provider boundary 规则需要复用 |
| verification-before-completion | 勾选 TODO 前必须有证据和验收结论 |
| subagent-driven-development | 用户要求专家 subagent 并行审查 |

## Size Decision

Size: S.

Reason: 本任务只回填计划状态、拆出后续增强项并记录证据；不改后端代码、API、DB schema、依赖、前端或运行时行为。

## Context Pack

### Related Docs

- `docs/planning/backend-architecture-todolist.md`
- `docs/specs/SPEC-20260609-rag-parser-layout-hierarchy.md`
- `docs/evidence/EVIDENCE-20260609-rag-parser-layout-hierarchy.md`
- `docs/acceptance/ACCEPT-20260609-rag-parser-layout-hierarchy.md`
- `docs/specs/SPEC-20260609-p3-2-f-parser-provider-boundary.md`
- `docs/evidence/EVIDENCE-20260609-p3-2-f-parser-provider-boundary.md`
- `docs/specs/SPEC-20260609-p3-2-g-real-parser-sdk-provider.md`
- `docs/evidence/EVIDENCE-20260609-p3-2-g-real-parser-sdk-provider.md`
- `docs/acceptance/ACCEPT-20260609-p3-2-g-real-parser-sdk-provider.md`
- `docs/specs/SPEC-20260609-p3-2-h-configurable-ocr-fallback-provider.md`
- `docs/evidence/EVIDENCE-20260609-p3-2-h-configurable-ocr-fallback-provider.md`
- `docs/acceptance/ACCEPT-20260609-p3-2-h-configurable-ocr-fallback-provider.md`
- `docs/specs/SPEC-20260609-p3-2-i-real-ocr-provider.md`
- `docs/evidence/EVIDENCE-20260609-p3-2-i-real-ocr-provider.md`
- `docs/acceptance/ACCEPT-20260609-p3-2-i-real-ocr-provider.md`
- `docs/skills/project-specific/rag-parser-boundary.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`

### Subagent Plan

| Expert | Scope |
|---|---|
| Agent/RAG Architect | 判断 P3-2-E/F/G/H/I 是否足以把 TODO 标记为最小生产化完成，并列出仍未完成的工业级增强 |

### Files Allowed To Modify

- `docs/planning/backend-architecture-todolist.md`
- `docs/tasks/TASK-20260610-p3-2-parser-ocr-todo-status-reconciliation.md`
- `docs/evidence/EVIDENCE-20260610-p3-2-parser-ocr-todo-status-reconciliation.md`
- `docs/subagents/runs/RUN-20260610-p3-2-parser-ocr-todo-status-reconciliation.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`

### Files Not Allowed To Modify

- `backend/src/main/**`
- `backend/src/test/**`
- `backend/pom.xml`
- `frontend/**`
- `backend/src/main/resources/db/migration/**`
- secrets / `.env`

## Checklist

- [x] Read current P3-2 TODO state.
- [x] Verify P3-2-E page/layout hierarchy evidence exists.
- [x] Verify P3-2-F parser provider boundary evidence exists.
- [x] Verify P3-2-G PDFBox/POI parser provider evidence exists.
- [x] Verify P3-2-H configurable OCR fallback provider evidence exists.
- [x] Verify P3-2-I process OCR provider evidence exists.
- [x] Use subagent review for independent Agent/RAG judgment.
- [x] Update TODO without overstating industrial layout/table/TOC/native/cloud OCR completion.
- [x] Create combined evidence / acceptance.
- [x] Update changelog and memory.

## Test / Verification Commands

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentParserServiceTest,RealParserProviderTest,ConfigurableOcrFallbackServiceTest,ProcessOcrFallbackProviderTest test
mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest test
```

## Acceptance Criteria

- P3-2 parser/OCR TODO is marked completed only for minimum productionized parser/OCR capability.
- A separate unchecked follow-up remains for industrial PDF/DOCX layout/table/TOC/reading-order, native/cloud OCR, OCR confidence, and true rendered page numbers.
- No backend code, schema, dependency, API, frontend, or secret files are modified by this reconciliation task.
