---
name: feature-development-workflow
description: >
  Use this skill whenever the user gives a feature request, bug fix, refactor,
  frontend page, backend API, Agent workflow, RAG pipeline, database change,
  or system enhancement. This skill enforces Project Memory, Skill Selection,
  size classification, size-specific documents, implementation, testing,
  evidence, acceptance, changelog, and memory update. Keywords: feature,
  requirement, implement,
  add, build, fix, refactor, page, api, agent, rag, workflow, 需求, 功能,
  实现, 开发, 修复, 重构, 页面, 接口, 智能体.
---

# Feature Development Workflow Skill

## Goal

Turn a raw user requirement into a controlled Codex/Cursor development workflow.

Never jump directly from requirement to code.

## Mandatory Workflow

For every user requirement, follow this order:

```text
User Requirement
→ Read Project Memory
→ Skill Selection Gate
→ Size Classification Gate (S / M / L)
→ Multi-Expert Subagent Gate (size-aware)
→ GitHub Reference Gate if needed
→ Size-specific documents
   S: mini TASK with embedded Context Pack; combined Evidence/Acceptance
   M: REQ / SPEC / PLAN / TASK / Context Pack; PRD only if product behavior changes
   L: PRD / REQ / SPEC / PLAN / TASK / Context Pack
→ Parallel / Single Task Execution
→ Integration Review
→ Test
→ Evidence
→ Acceptance
→ Changelog
→ Memory Update
→ Retrospective / Skill Extraction if required by size or useful
```

The workflow is mandatory, but the document depth is size-specific. Do not force small slices through the full large-feature document set.

## Step 1: Read Project Memory

Read:

- `AGENTS.md`
- `.cursor/rules/*`
- `docs/memory/PROJECT_MEMORY.md`
- Related files under `docs/memory/`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/subagents/SUBAGENT_REGISTRY.md`

## Step 2: Skill Selection Gate

Before writing any PRD, REQ, SPEC, PLAN, TASK, or code, output:

```md
# Skill Selection Report

## Task Type

## Selected Skills

| Skill | Why Needed |
|---|---|

## Missing Skills

## GitHub Research Needed

Yes / No

## New Project-Specific Skill To Create
```

Only select relevant skills. Do not load all skills.

## Step 2.5: Size Classification Gate

Before creating documents or using subagents, classify the task:

| Size | Use When | Required Documents Before Code |
|---|---|---|
| S - Small Slice / Fast Lane | One bounded module or narrow cleanup; no public API/DTO/schema/dependency/frontend-backend contract change; usually <=3 production files; focused/adjacent tests sufficient | One mini `docs/tasks/TASK-YYYYMMDD-parent-id-semantic-slug.md` with embedded Context Pack |
| M - Standard Feature Slice | One substantial module or two related modules; visible behavior or integration risk; not a broad architecture initiative | `REQ`, `SPEC`, `PLAN`, `TASK`, standalone `CONTEXT`; `PRD` only if product/user behavior changes |
| L - Large Feature / Architecture Change | New product capability, frontend+backend linkage, DB/schema, dependency, Agent/RAG workflow, security architecture, or 3+ modules | Full `PRD`, `REQ`, `SPEC`, `PLAN`, `TASK`, standalone `CONTEXT` |

Output:

```md
## Size Classification

Size: S / M / L
Reason:
Required Documents:
Can Skip:
Upgrade Trigger:
```

If a task grows beyond the selected size, stop, reclassify, and create the missing documents before continuing.

### Epic / Parent-Child Naming

For long-running parent items such as `P3-4`, do not create artificial `X/Y/Z` names. Keep the parent ID and name child work semantically:

```text
P3-4 子任务：LearningPath create legacy overload cleanup
```

Use file names such as:

```text
TASK-20260609-p3-4-learning-path-create-legacy-overload-cleanup.md
EVIDENCE-20260609-p3-4-learning-path-create-legacy-overload-cleanup.md
```

Do not mark the parent epic complete until every required child item is complete.

## Step 3: Multi-Expert Subagent Gate

Runs after Skill Selection and Size Classification, before GitHub Reference Gate.

Positioning: Main Codex = orchestrator; Subagents = parallel analysis/design/review.

### When To Enable

| Scope | Rule |
|---|---|
| 1 module | Default single Codex |
| 2 modules | May enable experts for parallel analysis |
| 3+ modules | Must enable Multi-Expert Subagent |
| Agent/RAG | Must enable Agent/RAG Expert |
| S-size permission/security cleanup | Use relevant security skills; subagent optional |
| M/L Security / DB / dependency | Must enable Security & Quality |
| Frontend + backend API | Must enable Frontend + Backend + Integration Reviewer |

Not for: copy tweaks, style fix, one field, simple validation, README one-liner.

### Parallelism Levels

- **L1 Parallel Analysis** — default for medium/large (safest)
- **L2 Parallel Design** — merge via Integration Reviewer
- **L3 Parallel Implementation** — rare; worktree + no file overlap + Context Pack ownership

Default: analysis/design may parallelize; coding is single-task.

If subagents are needed, create report:

```text
docs/subagents/runs/RUN-YYYYMMDD-feature-name.md
```

Output a Subagent Decision block:

```md
## Subagent Decision

Use Subagents: Yes / No
Reason:
Parallelism Level: L1 / L2 / L3
Selected Subagents:
Implementation Mode: Single Codex / Parallel analysis / Parallel design / Worktree implementation
```

## Step 4: GitHub Reference Gate

If existing skills are insufficient:

1. Search GitHub for strong references.
2. Summarize useful patterns.
3. Create a report under `docs/research/github-references/`.
4. Create or update a project-specific skill under `docs/skills/project-specific/`.

Never copy code blindly.
Never add dependencies only because a GitHub project uses them.

## Step 5: Create Size-Specific Documents

All workflow documents must be written in **Chinese** (正文中文；API/JSON/SQL/代码标识符可保留英文).

Create or update only the documents required by the selected size.

S:

```text
docs/tasks/TASK-YYYYMMDD-parent-id-semantic-slug.md
```

The S mini TASK must include goal, task type, selected skills, size decision, related memory/docs, allowed files, disallowed files, test commands, acceptance criteria, and current boundary.

M:

```text
docs/requirements/REQ-YYYYMMDD-feature-name.md
docs/specs/SPEC-YYYYMMDD-feature-name.md
docs/plans/PLAN-YYYYMMDD-feature-name.md
docs/tasks/TASK-YYYYMMDD-feature-name.md
docs/context/CONTEXT-YYYYMMDD-feature-name.md
```

Add `docs/product/PRD-YYYYMMDD-feature-name.md` for M only when product/user behavior changes.

L:

```text
docs/product/PRD-YYYYMMDD-feature-name.md
docs/requirements/REQ-YYYYMMDD-feature-name.md
docs/specs/SPEC-YYYYMMDD-feature-name.md
docs/plans/PLAN-YYYYMMDD-feature-name.md
docs/tasks/TASK-YYYYMMDD-feature-name.md
docs/context/CONTEXT-YYYYMMDD-feature-name.md
```

Do not implement until the required documents for the selected size exist.

## Step 6: Implementation

Implement only one task at a time.

Follow Context Pack restrictions:

- Only modify allowed files.
- Do not modify unrelated files.
- Do not add dependencies without dependency review.
- Do not change API contracts without SPEC update.
- Do not change database schema without SPEC update.

## Step 7: Test and Evidence

Run tests when possible.

Create records according to task size.

S may use one combined evidence/acceptance document:

```text
docs/evidence/EVIDENCE-YYYYMMDD-parent-id-semantic-slug.md
```

M/L should create:

```text
docs/evidence/EVIDENCE-YYYYMMDD-feature-name.md
docs/acceptance/ACCEPT-YYYYMMDD-feature-name.md
```

If tests cannot run, explain why.

## Step 8: Update Memory

After implementation, update:

- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- Related memory files
- Related documents required by the selected size
- Skill Registry if new skills were added

## Architecture Rules

Never:

- Let frontend call LLM APIs directly.
- Put API keys in frontend code.
- Let Agent tools access Mapper or Repository directly.
- Use Prompt as permission control.
- Create unbounded Agent loops.
- Generate RAG answers without sources.
- Skip Agent Trace for AI workflows.

Always:

- Backend owns AI API calls.
- Tool calls go through Service layer.
- Permission checks happen in backend code.
- Agent loops must have max round limits.
- RAG answers must include citations.
- AI-generated resources must have review status.
- Important operations must have traceId.

## Final Output Format

At the end, output:

```md
# Development Summary

## Requirement

## Documents Created / Updated

## Selected Skills

## Subagents Used

## Implementation Status

## Tests Run

## Evidence

## Acceptance

## Memory Updated

## Next Recommended Task
```
