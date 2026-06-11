# RUN - P3-4-V Integration Review

## Inputs

- Architect report: `RUN-20260609-p3-4-v-resource-trace-detail-rbac-architecture.md`
- Security report: `RUN-20260609-p3-4-v-resource-trace-detail-rbac-security.md`
- Test report: `RUN-20260609-p3-4-v-resource-trace-detail-rbac-test.md`

## Integrated Decision

Proceed with a single-Codex implementation. The reports agree on root cause and file boundary:

- HTTP controllers pass only `currentUserId()`.
- Services infer admin from `sub == "admin"`.
- The fix should pass explicit role facts from `UserContext.roles()` into service-layer authorization.

## Conflict Resolution

| Topic | Decision |
|---|---|
| Admin can read ResourceGeneration detail | Yes, explicit `ADMIN` role can read existing task detail. |
| Admin can read learner-resources | No. `learner-resources` remains owner-only; explicit admin only controls missing `NOT_FOUND` semantics. |
| Admin can cancel foreign task | No. Cancel remains owner-only and out of scope. |
| Legacy service overloads | Preserve for compatibility; HTTP paths must use roles-first overloads. |

## Implementation Guardrails

- No API/DTO/schema/dependency/frontend changes.
- No formal OAuth2/JWK/Spring Security migration.
- No Agent/RAG/model runtime changes.
- Keep non-admin missing/foreign anti-enumeration.

## Verification Gate

Required:

1. Focused RED observed.
2. Focused GREEN for new tests.
3. Adjacent regression:
   `ResourceGenerationControllerTest,AgentTraceControllerTest,ResourceReviewControllerTest,OrchestratorWorkflowControllerTest,AnalyticsControllerTest`
4. Full backend `mvn test`.

## Status

PASS to implement.
