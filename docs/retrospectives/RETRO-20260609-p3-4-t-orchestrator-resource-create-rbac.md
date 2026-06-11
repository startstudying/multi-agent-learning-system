# RETRO - P3-4-T Orchestrator RESOURCE_GENERATION create roles-first RBAC

## 1. What Changed

- Orchestrator HTTP create/retry now derives admin/teacher facts from `UserContext.roles()`.
- Orchestrator service now has roles-first create/retry overloads.
- `RESOURCE_GENERATION` workflow create no longer enters ResourceGeneration through the legacy subject-name role inference path.
- Regression tests cover subject-name role confusion, admin other-learner denial, spoofed header precedence, and side-effect ordering.

## 2. What Worked

- The P3-4-S direct-create pattern transferred cleanly to the Orchestrator workflow entrypoint.
- Treating retry as a first-class entrypoint prevented the same legacy create path from reappearing after a failure.
- Side-effect assertions were more useful than status-only tests because Orchestrator may legitimately persist safe failed workflow evidence.
- Integration review kept the boundary clear: ResourceGeneration remains owner-only and P3-4 remains broader than this slice.

## 3. What Did Not Change

- No API path, DTO, schema, frontend, or dependency changes.
- No formal OAuth2/JWK/Spring Security migration.
- No ResourceGeneration detail/trace/cancel/review RBAC changes.
- No broader class/course authorization matrix changes.

## 4. Lessons

- Every AI workflow entrypoint needs its own auth-fact audit: direct HTTP, Orchestrator create, retry, scheduler, and internal recovery paths can diverge.
- Role facts should be passed explicitly through orchestration boundaries; helper methods that infer from `userId` should be treated as transitional compatibility only.
- A denied Orchestrator request can have acceptable failure evidence while still requiring zero downstream business/model/cost side effects.

## 5. Reusable Pattern

No new skill is extracted in this slice.

Reusable existing patterns:

- `auth-context-boundary`: derive role facts from verified `UserContext.roles()`, not `X-User-Id` or subject-name conventions.
- `object-scope-authorization`: assert denial before sensitive business, model, token, citation, or review side effects.

Potential future project-specific skill:

- `workflow-entrypoint-rbac-audit`: enumerate direct/orchestrated/retry/scheduled entrypoints for one business action, migrate them to roles-first facts, and prove side-effect boundaries with focused tests.
