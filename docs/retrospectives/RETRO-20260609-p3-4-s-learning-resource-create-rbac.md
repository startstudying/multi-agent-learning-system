# RETRO - P3-4-S LearningPath / ResourceGeneration course-bound create roles-first RBAC

## 1. What Changed

- LearningPath direct create now uses explicit role facts from `UserContext.roles()`.
- ResourceGeneration direct create now uses explicit role facts while preserving owner-only behavior.
- Course-bound enrollment checks can use a roles-first helper.
- RED/GREEN tests cover Bearer admin success, subject-name role-confusion denial, and ResourceGeneration zero side effects.

## 2. What Worked

- The existing P3-4 roles-first pattern from Course / RAG KB / Assessment slices transferred cleanly.
- Keeping ResourceGeneration owner-only avoided accidental privilege expansion.
- Side-effect assertions made the security boundary concrete instead of only checking HTTP status.
- Integration review caught the Orchestrator workflow path boundary before it was accidentally implied as complete.

## 3. What Did Not Change

- No API path, DTO, schema, frontend, or dependency changes.
- No formal OAuth2/JWK/Spring Security migration.
- No ResourceGeneration detail/trace/cancel/review RBAC changes.
- No Orchestrator `RESOURCE_GENERATION` workflow create migration.

## 4. Lessons

- "Create" may have more than one runtime entrypoint. Future RBAC slices should enumerate direct HTTP, Orchestrator, scheduler, retry, and internal service callers separately.
- roles-first overloads are useful, but legacy overloads must be tracked as risk until all external entrypoints migrate.
- Admin bypass should be call-site explicit, not inferred by subject names or hidden in shared helpers.

## 5. Reusable Pattern

No new skill is extracted in this slice.

Reusable existing patterns:

- `auth-context-boundary`: derive role facts from verified `UserContext.roles()`, not `X-User-Id` or subject-name conventions.
- `object-scope-authorization`: perform owner/enrollment checks before replay, task creation, model calls, trace writes, or other durable side effects.

Potential future project-specific skill:

- `roles-first-authorization-migration`: checklist for enumerating all entrypoints, replacing subject-name inference with explicit role facts, preserving legacy compatibility safely, and proving side-effect order with tests.
