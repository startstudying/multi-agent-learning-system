# ACCEPT - P3-4-T Orchestrator RESOURCE_GENERATION create roles-first RBAC

## 1. Acceptance Decision

Accepted for the explicit P3-4-T scope.

P3-4-T is accepted for:

- `POST /api/orchestrator/workflows` with `workflowType=RESOURCE_GENERATION`.
- `POST /api/orchestrator/workflows/{workflowId}/retry` for failed `RESOURCE_GENERATION` workflows.
- roles-first auth fact propagation from Orchestrator HTTP entrypoints into ResourceGeneration workflow create.

P3-4-T is not accepted as full P3-4 completion.

## 2. Requirement Acceptance

| Requirement | Status | Evidence |
|---|---|---|
| Orchestrator create derives role facts from `UserContext.roles()` | Accepted | Controller passes explicit `ADMIN` / `TEACHER` facts to service. |
| Orchestrator service has roles-first create overload | Accepted | `createWorkflow(ownerUserId, admin, teacher, request)` is used by HTTP create. |
| `RESOURCE_GENERATION` branch calls roles-first ResourceGeneration workflow API | Accepted | Service passes explicit role facts to `createTaskInWorkflow(...)`. |
| Orchestrator retry uses roles-first facts | Accepted | HTTP retry calls `retryWorkflow(ownerUserId, admin, teacher, workflowId)`, which routes through roles-first create. |
| Bearer `USER sub=admin` cannot bypass course enrollment | Accepted | Focused controller regression returns `FORBIDDEN`. |
| Bearer admin cannot create ResourceGeneration workflow for another learner | Accepted | Focused controller regression returns `FORBIDDEN` before workflow side effects. |
| Bearer student owner with active enrollment succeeds despite spoofed `X-User-Id` | Accepted | Focused controller regression succeeds with owner `alice`, not spoofed header user. |
| Forbidden enrollment failure has no ResourceGeneration/model/token/citation side effects | Accepted | Repository assertions cover no ResourceGeneration business/model/cost/citation rows. |
| No API / DB / frontend / dependency drift | Accepted | No path/DTO/schema/dependency/frontend changes. |

## 3. Verification Acceptance

| Verification | Status |
|---|---|
| TDD RED observed | Accepted: focused old implementation produced `28 run, 1 failure` |
| Focused GREEN | Accepted: `28 run, 0 failures, 0 errors` |
| Adjacent regression | Accepted: `94 run, 0 failures, 0 errors` |
| Full backend regression | Accepted: `449 run, 0 failures, 0 errors, 1 skipped` |
| Integration review | Accepted: PASS for P3-4-T scope |

## 4. Out-of-Scope Follow-Up

| Follow-up | Reason |
|---|---|
| broader class/course authorization matrix | P3-4 covers more than Orchestrator ResourceGeneration create/retry. |
| formal OAuth2/JWK/Spring Security | Current slice uses existing Bearer HS256 compatibility layer. |
| broader permission penetration tests | Existing matrix should expand across remaining teacher/student/admin class/course flows. |
| ResourceGeneration detail/trace/cancel/review RBAC | Not part of create/retry entrypoint migration. |

## 5. Final Status

P3-4-T Orchestrator `RESOURCE_GENERATION` create/retry roles-first RBAC slice is accepted.

Do not mark P3-4 or `docs/planning/backend-architecture-todolist.md` overall complete.
