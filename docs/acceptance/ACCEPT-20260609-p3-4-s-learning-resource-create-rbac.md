# ACCEPT - P3-4-S LearningPath / ResourceGeneration course-bound create roles-first RBAC

## 1. Acceptance Decision

Accepted with explicit scope boundary.

P3-4-S is accepted for:

- `POST /api/learning-paths`
- `POST /api/resources/generation-tasks`

P3-4-S is not accepted as full coverage for all ResourceGeneration creation paths because Orchestrator `RESOURCE_GENERATION` workflow create remains a separate roles-first migration follow-up.

## 2. Requirement Acceptance

| Requirement | Status | Evidence |
|---|---|---|
| `LearningPathController.create` derives role facts from `UserContext.roles()` | Accepted | Controller passes explicit `ADMIN` / `TEACHER` facts to service. |
| `LearningWorkflowService.createPathForUser` has roles-first overload | Accepted | Explicit `currentUserAdmin` drives admin代创建 and enrollment bypass. |
| Bearer `ADMIN sub=ops_admin` can create course-bound learning path for another learner | Accepted | Focused controller test passes. |
| Bearer `USER sub=admin` cannot gain LearningPath admin semantics | Accepted | Focused controller test passes and no path/node/event persistence remains. |
| `ResourceGenerationController.create` derives role facts from `UserContext.roles()` | Accepted | Controller passes explicit role facts to service. |
| ResourceGeneration direct create remains owner-only | Accepted | Bearer admin/teacher代创建 is denied. |
| Bearer `USER sub=admin` cannot bypass ResourceGeneration course enrollment | Accepted | Focused controller test passes. |
| Forbidden ResourceGeneration direct create has no durable side effects | Accepted | Repository zero-count assertions cover task/resource/review/trace/model/token rows. |
| No API / DB / frontend / dependency drift | Accepted | No path/DTO/schema/dependency/frontend changes. |

## 3. Verification Acceptance

| Verification | Status |
|---|---|
| TDD RED observed | Accepted: `32 run, 3 failures` |
| Focused GREEN | Accepted: `32 run, 0 failures, 0 errors` |
| Adjacent regression | Accepted: `91 run, 0 failures, 0 errors` |
| Full backend regression | Accepted: `446 run, 0 failures, 0 errors, 1 skipped` |
| Integration review | Accepted with condition: direct API scope only |

## 4. Out-of-Scope Follow-Up

| Follow-up | Reason |
|---|---|
| Orchestrator `RESOURCE_GENERATION` workflow create roles-first RBAC | `OrchestratorWorkflowService` still calls the legacy `createTaskInWorkflow(ownerUserId, request, context)` path. |
| broader class/course matrix | P3-4 remains broader than this direct create slice. |
| formal OAuth2/JWK/Spring Security | Still planned production auth migration. |

## 5. Final Status

P3-4-S direct API slice is accepted.

Do not mark P3-4 or `docs/planning/backend-architecture-todolist.md` overall complete.
