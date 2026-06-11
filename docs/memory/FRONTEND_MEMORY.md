# FRONTEND_MEMORY.md

## Tech Stack

- Vue 3 + TypeScript + Vite
- Pinia (state management)
- Vue Router
- Element Plus / Naive UI
- Axios or fetch wrapper for API calls

## Directory Conventions

```
frontend/
  src/
    api/          # API client modules
    components/   # Reusable components
    composables/  # Shared logic (SSE, auth, etc.)
    views/        # Page-level views
    stores/       # Pinia stores
    router/       # Route definitions
    types/        # TypeScript interfaces
    utils/        # Helpers
```

## Core Rules

- Frontend cannot call LLM APIs directly.
- Frontend cannot store API keys.
- All API calls go through shared request wrapper.
- AI streaming uses SSE wrapper composable.
- Every page handles: loading, error, empty states.

## Completed Features

| Feature | Status | Related Docs | Notes |
|---|---|---|---|
| Frontend medium-fidelity UI prototype refactor | Done | `docs/specs/SPEC-20260606-frontend-ui-prototype-refactor.md` | Chinese SaaS cockpit for Student Learning Loop, Teacher Review Queue, Admin Operations; keeps API/backend stable and includes no-source/status/trace/citation UI contracts |

## Known Patterns

- SSE streaming for AI chat responses
- Pinia stores for session and user state
- Route guards for auth checks (backend validates)
- For AI learning cockpit pages, keep Chinese user-facing labels while preserving English engineering aliases for API paths, traceId, chunkId, documentId, state enums, and compatibility tests.
- Target-state controls that backend does not yet support must be disabled and documented, not wired to fake API calls.
- Admin charts may use CSS placeholders until backend production observability APIs exist; do not fabricate metrics.

## Open Issues

| Issue | Priority | Notes |
|---|---|---|
| Component extraction | P2 | `StatusPill`, `NoSourceCard`, `TraceTimeline`, `CitationPanel`, and admin chart placeholders are still page-level markup; extract after UI direction stabilizes |
| Reject review action | P1 | Teacher `Reject` button is disabled until review decision API supports `REJECTED` in frontend type contract |
