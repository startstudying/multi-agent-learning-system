# AGENTS.md

## Project Identity

This project is an AI-powered personalized learning multi-agent system.

Core capabilities:

- Conversational learner profile construction
- Multi-agent personalized resource generation
- Personalized learning path planning
- Resource recommendation
- Context-aware AI tutoring
- Learning effect evaluation
- Agent Trace visibility
- RAG citation visibility
- AI-generated resource review

Tech stack:

- Frontend: Vue 3 + TypeScript + Vite
- Backend: Java 21 + Spring Boot 3.x + Spring AI
- Database: MySQL 8.x
- Optional: Redis, MinIO, Vector DB, Docker Compose

## Auto Feature Request Mode

The user should only need to provide a raw requirement.

When the user writes a requirement such as:

- "实现 xxx"
- "开发 xxx"
- "修复 xxx"
- "新增 xxx"
- "优化 xxx"
- "帮我做 xxx"

Automatically treat it as a feature-development request.

Do not ask the user to paste the full workflow prompt.

Automatically invoke the `feature-development-workflow` skill (`.agents/skills/feature-development-workflow/SKILL.md`) and route the request through the size-specific workflow:

```text
Project Memory
→ Skill Selection Gate
→ Size Classification Gate (XS / S / M / L)
→ Multi-Expert Subagent Gate (size-aware)
→ GitHub Reference Gate if needed
→ Size-specific documents
   XS: no pre-implementation docs; focused verification; final response record
   S: single TASK with embedded Context Pack, Evidence, and Acceptance
   M: SPEC / TASK / Context Pack by default; REQ / PLAN / PRD only when triggered
   L: PRD / REQ / SPEC / PLAN / TASK / Context Pack
→ Parallel / Single Task Execution
→ Integration Review when risk or subagents justify it
→ Test
→ Evidence / Acceptance according to size
→ Changelog / Memory Update according to size
→ Retrospective / Skill Extraction if useful
```

CLI entry point: `scripts/codex-feature.sh` or `scripts/codex-feature.ps1`.

Only ask clarification if the requirement is truly ambiguous.

## Legacy Documentation

`docs/superpowers/` is archived (Claude Code / superpowers era). Do not create new files there.

For new features, use only:

- `docs/product/PRD-*.md`
- `docs/requirements/REQ-*.md`
- `docs/specs/SPEC-*.md`
- `docs/plans/PLAN-*.md`
- `docs/tasks/TASK-*.md`
- `docs/context/CONTEXT-*.md`

Baseline docs live under `docs/architecture/`, `docs/api/`, `docs/data/`, `docs/operations/`, `docs/planning/`, `docs/research/`. See `docs/INDEX.md` for the full map.

## Absolute Rule

Never start coding directly from a user request.

Every feature must follow this workflow:

User Request
→ Project Memory
→ Skill Selection Gate
→ Size Classification Gate (XS / S / M / L)
→ Multi-Expert Subagent Gate (size-aware)
→ GitHub Reference Gate if needed
→ Size-specific documents
→ Parallel / Single Task Execution
→ Integration Review when risk or subagents justify it
→ Test
→ Evidence / Acceptance according to size
→ Changelog / Memory Update according to size
→ Retrospective / Skill Extraction if required by size or useful

The workflow is mandatory, but the document depth is size-specific. Do not force small slices through the full large-feature document set.

## Project Memory Rule

Before any task, read only the amount of memory justified by size:

- XS: focused search/read of directly affected files; read memory only when the patch touches a remembered decision or long-lived behavior.
- S: quick read of `docs/memory/PROJECT_MEMORY.md` plus the one related domain memory/spec when relevant.
- M/L: read `docs/memory/PROJECT_MEMORY.md`, related domain memory files, related specs, and related decisions.

After any task, update only durable records justified by size:

- XS: no memory/changelog update by default; mention verification in the final response.
- S: update the TASK evidence/acceptance; update changelog/memory only when behavior, workflow, or project decisions changed.
- M/L: update `docs/memory/PROJECT_MEMORY.md`, related domain memory, `docs/changelog/CHANGELOG.md`, and related PRD / REQ / SPEC / PLAN / TASK / ACCEPT documents required by the task size.

Do not store secrets, API keys, private credentials, raw logs, or sensitive personal data in memory files.

## Skill Selection Gate

Before writing PRD, REQ, SPEC, PLAN, TASK, or code, select relevant skills.

Output depth is size-aware:

- XS: one concise sentence is enough.
- S: short report with task type, selected skills, GitHub research decision, and missing skills.
- M/L: full report.

For M/L, output:

1. Task type
2. Selected skills
3. Why each skill is selected
4. Missing skills, if any
5. Whether GitHub research is needed
6. New project-specific skill to create, if needed

Read:

- `docs/skills/SKILL_REGISTRY.md`

Do not load all skills at once. Select only relevant skills.

## Task Classification Strategy

After Skill Selection and before SPEC/PLAN, classify the task and apply the matching execution strategy. Different task types need different paths, not one fixed linear flow.

| Task type | Execution focus |
|---|---|
| New feature | Locate module, read existing interfaces/naming, design minimal implementation, add/update tests, check integration impact. |
| Bug fix | Reproduce or locate first, identify root cause, smallest necessary fix, add regression test. Avoid broad refactor. |
| Refactor | Preserve behavior, keep diffs reviewable, do not change API/schema silently. |
| RAG / retrieval | Pinpoint the failing layer (parsing / chunking / embedding / vector index / retrieval ranking / prompt construction / answer generation / citation) and modify only that layer. |
| Multi-agent workflow | Keep clear role boundaries (Planner / Retriever / Generator / Evaluator / Refiner), avoid agent proliferation, enforce max-iteration and max-tool-call limits. |
| Prompt engineering | Edit the prompt minimally, preserve required variables/formatting, keep prompts modular; avoid overlong prompts. |
| Docs / config / deployment | Update only the targeted files; do not touch unrelated modules. |

This classification is lightweight: it informs skill selection, task size, document depth, and subagent usage.

## Size Classification Gate (XS / S / M / L)

After Skill Selection and task-type classification, classify every request by size. The workflow remains mandatory, but the required documents and subagent depth depend on size.

### XS - Patch Lane

Use XS only when all are true:

- Affects one file or a very small set of tightly related documentation/config/test-only files.
- Changes no public REST API contract, request/response DTO, database schema, dependency, deployment topology, frontend/backend contract, Agent/RAG orchestration, model-provider behavior, permission/security architecture, or production data semantics.
- Has no meaningful product workflow ambiguity.
- Can be verified by focused inspection, one targeted test, or a clearly explained manual check.

XS workflow:

```text
Focused file/context read
→ One-line Skill Selection + Size Classification = XS
→ No pre-implementation docs
→ No subagents
→ Implementation
→ Focused verification only
→ Final response records files changed, verification, and residual risk
```

XS must upgrade to S or higher if it touches production behavior beyond the local patch, needs more than one edit/verify loop to understand, or reveals API/DB/Agent/RAG/security/dependency risk.

### S - Small Slice / Fast Lane

Use S when all are true:

- Affects one bounded module or one narrow cross-cutting cleanup.
- Changes no public REST API contract, request/response DTO, database schema, dependency, deployment topology, or frontend/backend contract.
- Usually modifies no more than 3 production files.
- Can be verified with focused and adjacent tests.
- Does not introduce or redesign Agent/RAG orchestration, model-provider behavior, or security architecture.

S workflow:

```text
Project Memory quick read
→ Skill Selection short report
→ Size Classification = S
→ No subagents by default
→ Single TASK with embedded Context Pack
→ RED/GREEN or focused verification
→ Implementation
→ Focused + adjacent tests; full test only when risk justifies it
→ Evidence/Acceptance recorded inside the TASK
→ Changelog + Memory update only when durable impact exists
```

S may skip separate PRD, REQ, SPEC, PLAN, standalone Context Pack, standalone Evidence, standalone Acceptance, subagent reports, and standalone Retro unless the change grows beyond S boundaries.

### M - Standard Feature Slice

Use M when the work affects one substantial module or two related modules, has integration risk, or changes visible behavior without becoming a broad architecture initiative.

M workflow:

```text
Project Memory
→ Skill Selection
→ Size Classification = M
→ Subagents optional for analysis/design when useful
→ SPEC / TASK / Context Pack by default
→ Implementation
→ Focused + adjacent tests; full test when backend/frontend risk justifies it
→ Evidence
→ Acceptance
→ Changelog + Memory update
```

For M, add `REQ` only when requirement boundaries are unclear, add `PLAN` only for multi-step or cross-module sequencing, and add `PRD` only when product/user behavior or workflow semantics change. Retrospective and skill extraction are required only when reusable process or domain knowledge was discovered.

### L - Large Feature / Architecture Change

Use L for new product capabilities, frontend + backend linkage, database/schema changes, new dependencies, Agent/RAG workflow changes, security architecture changes, or 3+ affected modules.

L workflow keeps the full strict path:

```text
Project Memory
→ Skill Selection
→ Size Classification = L
→ Multi-Expert Subagent Gate
→ GitHub Reference Gate if needed
→ PRD / REQ / SPEC / PLAN / TASK / Context Pack
→ Implementation
→ Integration Review
→ Focused / adjacent / full tests as applicable
→ Evidence
→ Acceptance
→ Changelog
→ Memory Update
→ Retrospective
→ Skill Extraction if useful
```

### Epic / Parent-Child Naming Rule

For long-running parent items such as `P3-4`, do not create artificial `X/Y/Z` slice names. Keep the parent ID and name child work semantically:

```text
P3-4 子任务：LearningPath create legacy overload cleanup
```

File names should use the parent ID plus semantic slug, for example:

```text
TASK-20260609-p3-4-learning-path-create-legacy-overload-cleanup.md
EVIDENCE-20260609-p3-4-learning-path-create-legacy-overload-cleanup.md
```

Do not mark the parent epic complete until every required child item is complete.

## Multi-Expert Subagent Parallel Development Gate

Positioning:

```text
Main Codex     = orchestrator / integrator / final decision maker
Subagent       = expert parallel analysis, design, review; isolated implementation when approved
Cursor         = IDE rule enforcement and local development assist
```

This gate runs after Skill Selection Gate and before GitHub Reference Gate.

For complex tasks, Codex must decide whether to use multi-expert subagents before writing PRD, SPEC, PLAN, TASK, or code.

### When To Enable

| Scope | Rule |
|---|---|
| 1 module affected | Default: single Codex, no subagents |
| 2 modules affected | May enable relevant experts for parallel analysis |
| 3+ modules affected | Must enable Multi-Expert Subagent |
| Agent/RAG involved | Must enable Agent/RAG Expert |
| Permission/security cleanup in S size | Use relevant security skills; subagent optional |
| Permission, DB, dependency, security in M/L size | Must enable Security & Quality |
| Frontend + backend API linkage | Must enable Frontend Expert + Backend Expert + Integration Reviewer |

Use subagents when the task affects two or more of:

- Frontend
- Backend
- Database
- Agent workflow
- RAG pipeline
- Security
- Architecture
- Testing

Good candidates: learner profile, multi-agent resource generation, learning path planning, Agent Trace Console, RAG Citation Viewer, SSE tutoring, evaluation dashboard, Agent Orchestrator, RAG knowledge base, resource review workbench.

Not for: button copy, single style tweak, one field fix, simple validation, one-line README.

### Required Workflow

1. Main Codex reads project memory.
2. Main Codex performs Skill Selection Gate.
3. Main Codex decides whether subagents are needed.
4. Main Codex assigns clear roles, boundaries, and expected outputs.
5. Each subagent produces an independent report under `docs/subagents/runs/`.
6. Integration Reviewer merges all reports.
7. Conflicts are resolved before implementation.
8. PLAN and TASK are updated based on the integrated result.
9. Implementation starts only after integration review.

### Parallelism Levels

```text
Level 1 — Parallel Analysis (default for medium/large tasks)
  Experts analyze requirements, risks, module impact in parallel.

Level 2 — Parallel Design
  Frontend, backend, Agent/RAG write separate design proposals.
  Integration Reviewer merges into unified SPEC / PLAN / TASK.

Level 3 — Parallel Implementation (rare, strict conditions)
  Only when task boundaries are crystal clear.
  Requires independent branch/worktree per subagent, no file overlap,
  Context Pack file ownership, Integration Reviewer merges final diff.
```

### Parallelism Policy

Subagents may work in parallel for:

- Codebase exploration
- Requirement analysis
- Architecture review
- Frontend design
- Backend design
- Agent/RAG design
- Security review
- Test planning

Subagents may implement in parallel only when:

- Tasks are independent.
- Files do not overlap.
- Context Pack defines allowed files.
- Work is isolated by branch or worktree.
- Integration Reviewer reviews final diffs.

### Default Rule

- Small tasks: do not use subagents.
- Medium tasks: subagents for analysis and design only.
- Large tasks: subagents for analysis, design, review; optionally isolated implementation.

Never let multiple subagents modify the same file without explicit integration planning.

## GitHub Reference Gate

If no existing skill or rule fits the task:

1. Identify the missing capability.
2. Search GitHub for strong references.
3. Review popular repositories, examples, file structures, workflows, and rules.
4. Summarize useful patterns.
5. Create a GitHub reference report under `docs/research/github-references/`.
6. Create or update project-specific skill under `docs/skills/project-specific/`.
7. Continue the normal workflow.

Never copy code blindly from GitHub.
Never add a dependency only because a GitHub project uses it.

## Documentation Language

Workflow documents (PRD, REQ, SPEC, PLAN, TASK, Context Pack, Evidence, Acceptance) must be written in **Chinese**.

Exceptions (keep English): API paths, JSON field names, SQL, state enums, code identifiers, shell commands.

## Spec-First Rule

Implementation must not begin until the required documents for the selected size exist:

- XS: no pre-implementation workflow document is required; the final response is the record.
- S: `docs/tasks/TASK-*.md` with embedded Context Pack, Evidence, and Acceptance.
- M: `docs/specs/SPEC-*.md`, `docs/tasks/TASK-*.md`, and `docs/context/CONTEXT-*.md` by default. Add `REQ` only when requirement boundaries are unclear, `PLAN` only for multi-step/cross-module sequencing, and `PRD` only when product/user behavior changes.
- L: `docs/product/PRD-*.md`, `docs/requirements/REQ-*.md`, `docs/specs/SPEC-*.md`, `docs/plans/PLAN-*.md`, `docs/tasks/TASK-*.md`, and `docs/context/CONTEXT-*.md`.

If the task grows beyond the selected size, stop, reclassify, and create the additional documents before continuing.

## Context Pack Rule

Before implementation, create a Context Pack according to size. XS tasks do not need a Context Pack. For S tasks, the Context Pack is embedded in the TASK file. For M/L tasks, create a standalone file under `docs/context/`.

The Context Pack must define:

- Related memory and docs
- Selected skills
- Subagent plan (if any)
- Files allowed to modify
- Files not allowed to modify
- Test commands
- Current task boundary

## Security / Dependency Gate

Before adding any new dependency:

1. Create a dependency review under `docs/security/`.
2. Document why the dependency is needed.
3. Check license, security advisories, and maintenance status.
4. Get explicit approval in PLAN or TASK if the dependency is significant.

Never commit API keys, secrets, or credentials.

## Architecture Drift Check

Architecture drift checks are risk-triggered:

1. Required for backend/frontend architecture boundary changes, Agent/RAG orchestration, permission/security, DB schema, dependencies, deployment topology, API contracts, or model-provider behavior.
2. Optional for XS/S docs-only, copy, style, test-only, or local validation changes with no architecture impact.
3. When required, read `docs/architecture/ARCHITECTURE_BASELINE.md`, run `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`, and document drift in PLAN/TASK or create an ADR before proceeding.

## Evidence / Acceptance Rule

After implementation:

1. Run the focused or full tests justified by task size and risk, using `docs/harness/TEST_COMMANDS.md` when applicable.
2. XS tasks record verification and residual risk in the final response only.
3. S tasks record evidence and acceptance verdict inside the TASK file.
4. M/L tasks should create standalone `docs/evidence/EVIDENCE-*.md` and `docs/acceptance/ACCEPT-*.md`.
5. A feature or slice is not done without test evidence and an acceptance verdict at the required size depth.

## Retrospective / Skill Extraction Rule

After completing a feature:

1. XS/S tasks: retrospective and skill extraction are optional; do them only if a reusable pattern or workflow issue was discovered.
2. M tasks: run a short retrospective when implementation required multiple iterations, cross-module tradeoffs, or non-obvious decisions.
3. L tasks: run retrospective using `docs/retrospectives/RETRO-TEMPLATE.md`.
4. Extract reusable patterns into `docs/skills/project-specific/` when useful.
5. Update `docs/skills/SKILL_REGISTRY.md` if a new skill is created.

## Task Execution Rule

During implementation:

- Implement only one task at a time.
- Modify only files listed in the Context Pack, embedded or standalone. For XS, modify only the directly identified files named in the working notes/final response.
- Do not rewrite large modules without approval.
- Do not change API contracts without updating specs.
- Do not change database schema without updating specs.
- Do not introduce new dependencies without dependency review.
- Run tests when possible.
- Explain clearly if tests cannot be run.

## Codex Cost and Loop Control

This rule constrains Codex's own execution behavior (separate from the runtime Agent loop limits in Architecture Rules).

During any task, Codex must:

- Prefer focused inspection over scanning the whole repository.
- Prefer small patches over large rewrites.
- Set a maximum number of edit/verify iterations for a single task; if exceeded, stop and summarize.
- Stop and summarize when progress stalls instead of continuing.
- Never repeat a failed action without a new hypothesis or new evidence; analyze the failure before editing again.
- Never fabricate test results; only claim a test passed if it was actually run.
- When blocked (login, missing data, unexpected state, ambiguous requirement), stop and report rather than blind retrying.

At the end of every task, output:

1. Summary of changes.
2. Files modified.
3. Verification performed.
4. Remaining risks or assumptions.
5. Suggested next step, if any.

## Architecture Rules

Never:

- Let frontend call LLM APIs directly.
- Put API keys in frontend code.
- Let Agent tools access Mapper or Repository directly.
- Use Prompt as permission control.
- Create unbounded Agent loops.
- Generate RAG answers without sources.
- Skip Agent Trace for AI workflows.
- Add dependencies without review.

Always:

- Backend owns AI API calls.
- Tool calls go through Service layer.
- Permission checks happen in backend code.
- Agent loops must have max round limits.
- RAG answers must include citations.
- AI-generated resources must have review status.
- Important operations must have traceId.

## Done Definition

A feature or slice is done only when the selected size's done definition is met:

### XS Done Definition

- Patch stayed inside XS boundaries.
- Code/docs/config are implemented.
- Focused verification is run, or limitation is explained.
- Final response records files changed, verification, remaining risk, and any reason memory/changelog was not updated.

### S Done Definition

- Single TASK exists and includes goal, scope, allowed files, disallowed files, tests, embedded Context Pack, evidence, and acceptance criteria.
- Code is implemented.
- Focused and adjacent tests are run, or any limitation is explained.
- TASK Evidence/Acceptance has a clear verdict.
- Changelog and project/domain memory are updated concisely when the slice changes durable behavior, workflow, or project decisions.
- Parent epic child item is updated without marking the parent complete unless all child items are done.

### M Done Definition

- SPEC, TASK, and Context Pack exist.
- REQ exists if requirement boundaries were unclear.
- PLAN exists if multi-step or cross-module sequencing was needed.
- PRD exists if product/user behavior changed.
- Code is implemented.
- Focused and adjacent tests are run; full tests are run when risk justifies it, or limitation is explained.
- Evidence and Acceptance exist.
- Changelog and memory are updated.
- Retrospective/skill extraction completed if useful.

### L Done Definition

- PRD, REQ, SPEC, PLAN, TASK, and Context Pack exist.
- Required subagent reports and integration review exist.
- Code is implemented.
- Focused, adjacent, and full applicable tests are run, or limitations are explained.
- Evidence and Acceptance exist.
- Changelog and memory are updated.
- Retrospective completed.
- New skills are extracted if useful.
