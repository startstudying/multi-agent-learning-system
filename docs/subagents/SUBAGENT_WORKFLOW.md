# Subagent Workflow

## Goal

Use multiple expert subagents to analyze, design, review, and optionally implement complex tasks safely.

## Main Principle

Subagents are used to reduce blind spots, not to create uncontrolled parallel edits.

```text
复杂任务 → 多专家并行分析设计
普通任务 → 单 Codex 小步执行
```

## Workflow

1. Main Codex identifies task complexity (module count, Agent/RAG, security).
2. Main Codex selects required subagents per SUBAGENT_REGISTRY hard rules.
3. Each subagent receives bounded instructions and expected output format.
4. Each subagent returns a structured report section.
5. Integration Reviewer compares reports and lists conflicts.
6. Conflicts are resolved with documented decisions.
7. Final PLAN and TASK are created or updated.
8. Implementation proceeds one task at a time unless worktree isolation is explicitly approved.

## Parallelism Levels

### Level 1 — Parallel Analysis (safest, recommended default)

Multiple experts analyze requirements, risks, and module impact in parallel.

### Level 2 — Parallel Design

Frontend, backend, and Agent/RAG experts write separate design proposals.
Integration Reviewer merges into unified SPEC / PLAN / TASK.

### Level 3 — Parallel Implementation (strict conditions only)

Allowed only when:

- Task boundaries are crystal clear
- Independent branch or worktree per subagent
- No file overlap
- Context Pack defines file ownership
- Integration Reviewer merges and reviews final diff

**Default: analysis and design may parallelize; coding does not.**

## Non-Negotiable Rules

- No uncontrolled parallel edits.
- No overlapping file edits without approval.
- No dependency addition without dependency review.
- No database change without SPEC update.
- No API contract change without frontend/backend agreement.
- No Agent/RAG change without trace and security review.

## Report Storage

```text
docs/subagents/runs/RUN-YYYYMMDD-feature-name.md
```
