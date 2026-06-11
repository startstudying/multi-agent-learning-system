# SPEC-20260610 P3-4 子任务：RAG query runtime roles-first RBAC

## Scope

本规格只覆盖 RAG query runtime 的 roles-first 授权事实传递，不改变 API / DB / dependency / frontend。

## Current Gap

当前 runtime 调用链：

```text
ChatController / TutorController / OrchestratorWorkflowService
-> RagQueryService.query(userId, kbIds, question, topK)
-> PermissionService.requireReadableKbIds(userId, kbIds)
```

该链路没有传入 `currentUserAdmin` / `currentUserTeacher`，因此与 P3-4-P management path 的 roles-first 行为不一致。

## Target Call Chain

```text
Controller
-> CurrentUserService.currentUser()
-> hasRole(UserContext, "ADMIN" / "TEACHER")
-> RagQueryService.query(userId, admin, teacher, kbIds, question, topK)
-> PermissionService.requireReadableKbIds(userId, admin, teacher, kbIds)
```

Orchestrator：

```text
OrchestratorWorkflowController
-> OrchestratorWorkflowService.createWorkflow(ownerUserId, admin, teacher, request)
-> replayQueryIfPresent(ownerUserId, admin, teacher, ...)
-> queryWithTraceIdAndRequestId(ownerUserId, admin, teacher, ...)
```

## API Contract

No public API contract changes.

## Service Contract

`RagQueryService` 新增 role-aware overload：

- `query(String userId, boolean currentUserAdmin, boolean currentUserTeacher, List<String> kbIds, String question, Integer topK)`
- `queryWithTraceId(...)`
- `queryWithRequestId(...)`
- `queryWithTraceIdAndRequestId(...)`
- `replayQueryIfPresent(...)`

Legacy overloads remain and delegate to role-aware overloads with `false, false`.

## Authorization Semantics

| Actor | Expected Runtime Behavior |
|---|---|
| Explicit `ADMIN` role | Can query active KBs according to `PermissionService` role-aware admin semantics. |
| Explicit `TEACHER` role | Uses current `PermissionService` read semantics; this slice does not introduce teacher-course KB lifecycle rules. |
| `USER sub=admin` | No admin semantics; foreign private KB query returns `FORBIDDEN`. |
| Student / ordinary user | Owner/public/explicit KB permission only. |

## Architecture Drift Check

| Check | Expected |
|---|---|
| Backend layering | PASS: Controller passes facts; Service authorizes. |
| Agent / RAG | PASS: retrieval permission filtering remains before retrieval/log/citation writes. |
| Security | PASS: no Prompt-based permission; Bearer roles drive role facts. |
| API / DB | PASS: no path/DTO/schema changes. |
| Dependency | PASS: no new dependency. |

## Follow-up

- KB-course binding schema / lifecycle governance remains a separate L-sized follow-up.
- SSE production auth transport remains a separate auth-context follow-up.
