# PLAN-20260610 P3-4 子任务：RAG query runtime roles-first RBAC

Status: Done.

## Skill Selection Report

### Task Type

安全 / RAG runtime RBAC 补口。

### Selected Skills

| Skill | Why Needed |
|---|---|
| feature-development-workflow | 按项目 S/M/L 工作流执行后端架构 TODO |
| rag-project-review | RAG query runtime、query log、citation 和 retrieval permission filtering |
| object-scope-authorization | 对象级 KB read scope 和 anti-side-effect 权限规则 |
| auth-context-boundary | Bearer role facts、spoofed header、subject-name role-confusion |
| test-generator | 需要 RED/GREEN 覆盖 runtime 授权矩阵 |
| subagent-driven-development | 用户要求专家 subagent 并行开发/审查 |

### Missing Skills

无。

### GitHub Research Needed

No. 本任务使用项目既有 Spring Security / RAG / RBAC 规则，不需要外部参考。

### New Project-Specific Skill To Create

暂不创建。若后续做 KB-course lifecycle schema，可扩展 `object-scope-authorization` 或新增 KB lifecycle skill。

## Size Classification

Size: M.

Reason:

- 影响 `rag` runtime、`tutor` runtime、orchestrator RAG_QA 和安全授权事实传递。
- 不改 API/DTO/DB/dependency/frontend，但属于跨 runtime 的安全补口，有集成风险。

Required Documents:

- `REQ`
- `SPEC`
- `PLAN`
- `TASK`
- `CONTEXT`
- `EVIDENCE`
- `ACCEPT`

Can Skip:

- PRD：不改变产品能力或用户流程，只收口安全边界。
- Dependency review：不新增依赖。

Upgrade Trigger:

- 若新增 `KnowledgeBase.courseId` 或 KB lifecycle schema，升级为 L 并单独建 schema/API 文档。

## Subagent Decision

Use Subagents: Yes.

Reason: 用户明确要求专家 subagent 并行；本任务涉及 RAG runtime + security + tests。

Parallelism Level: L1 Parallel Analysis.

Selected Subagents:

| Expert | Scope |
|---|---|
| Agent/RAG Architect | 切片边界、allowed files、是否需要 DB schema |
| Test Engineer | RED/GREEN matrix、focused/adjacent verification |

Implementation Mode: Main Codex single implementation.

## Implementation Plan

1. 写 RED controller tests：
   - `/api/rag/query` Bearer admin + spoofed `X-User-Id` 应向 service 传 `admin=true`。
   - `/api/rag/query` Bearer `USER sub=admin` 应向 service 传 `admin=false`。
   - Tutor ask / stream 继续使用同一 roles-first service overload。
2. 写 RED service tests：
   - role-aware admin 可 query foreign private KB。
   - legacy literal `admin` 不可 query foreign private KB。
   - role-aware requestId path 可 query/replay foreign private KB。
3. 修改 runtime：
   - `ChatController`
   - `TutorController`
   - `RagQueryService`
   - `OrchestratorWorkflowService`
4. 运行 focused + adjacent tests。
5. 写 Evidence / Acceptance / Changelog / Memory。

## Architecture Drift Pre-check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller pass facts; Service authorizes |
| Frontend rules | PASS | No frontend changes planned |
| Agent / RAG rules | PASS | Retrieval permission filtering remains before retrieval |
| Security | PASS | No new dependency; no Prompt permission |
| API / Database | PASS | No path/DTO/schema changes planned |

## Architecture Drift Post-check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controllers only derive auth facts and delegate; `RagQueryService` / `PermissionService` own authorization. |
| Frontend rules | PASS | No frontend files changed. |
| Agent / RAG rules | PASS | RAG permission filtering still happens before retrieval, query log, and citation writes; Orchestrator `RAG_QA` trace alignment preserved. |
| Security | PASS | Bearer roles drive explicit admin/teacher facts; legacy overloads default to non-admin/non-teacher; no Prompt permission and no secrets added. |
| API / Database | PASS | No REST path, DTO, DB schema, dependency, or migration changes. |

## Completion Summary

- Added roles-first RAG query runtime propagation across `/api/rag/query`, Chat/Tutor runtime, and Orchestrator `RAG_QA`.
- Added Orchestrator `RAG_QA` Bearer admin / subject-name role-confusion regressions.
- Verification passed: focused `60/60`, adjacent `161/161`, full backend `509 run, 0 failures, 0 errors, 1 skipped`.
- P3-4 parent remains open for KB-course binding schema/lifecycle governance, broader class/course matrix follow-up, SSE production auth strategy, and dev/test legacy fallback cleanup.
