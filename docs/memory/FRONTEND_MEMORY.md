# FRONTEND_MEMORY.md

## 2026-06-21 QA streaming and trace workbench MVP

- Student production/staging safe streaming now calls `/api/ai/qa/stream` through new `frontend/src/api/aiQa.ts`.
- The request body carries `answerMode`, `kbIds`, `question`, `courseId`, and `topK`; question/kbIds are not placed in the stream URL.
- Dev legacy EventSource RAG stream and REST fallback remain for existing local workflow compatibility.
- Student answer card now shows QA quality summary: verifier verdict, gate policy, source policy, uncertainty, answer mode, reasoning effort, tool call count, and quality flags.
- Right thought panel now includes a "校验回答质量" step and uses QA verdict/source policy/tool-call count in runtime metrics when available.
- Verification passed: `pnpm test -- --run App.spec.ts` (34 passed) and `pnpm build`.
- Visual screenshot verification was not run in this slice.

## 2026-06-21 chanceNB Frontend Overwrite Migration

- Current frontend was overwritten from `chanceNB/main` (`20bc8aacb0d8e368df97c8d251adc704713bb0fe`) after explicit user approval.
- The active frontend now uses the imported `AppShell`, `LeftSidebar`, Mobbin layout components, thought panel components, workspace stream/composer components, login pages, new chat page, and imported student/teacher/admin pages.
- The previous local student workspace split (`VITE_APP_WORKSPACE`, `StudentNavHub`, `StudentSearch`, `StudentSettings`, `useStudentWorkbench`, and student component panels) was removed from `frontend/src` as stale relative to the imported source.
- Verification passed: `pnpm test -- --run` (34 passed), `pnpm build`, and preview `http://127.0.0.1:4173/` returned 200.
- Visual screenshot verification was not completed because Playwright browser binaries are missing locally; do not claim screenshot evidence until `npx playwright install` or an equivalent browser setup is completed.
- Follow-up: assess backend compatibility for imported chat session APIs before treating the imported new-chat workflow as fully integrated.

## 2026-06-11 Student Dashboard Data Source Dehardcoding

- Student dashboard production source no longer hardcodes demo learner, KB, goal, node, question, resource, or trace IDs.
- `getDefaultUserId()` centralizes the temporary frontend identity fallback as `student_dev`; page code should not reintroduce page-level learner constants.
- `useStudentWorkbench()` owns student course, knowledge-base, goal, path-node, and question selection state.
- `frontend/src/api/courses.ts` wraps `GET /api/courses` and `GET /api/courses/{courseId}/knowledge-graph`.
- Initial resources and Agent Trace are empty states; resource and trace UI only fills from backend action responses.
- Without a course-question-list API, assessment submit must remain an empty state and must not fabricate `questionId`.
- Malformed native EventSource payloads now surface `Invalid SSE event payload` rather than silently falling back to REST.
- Verification passed: frontend `pnpm test -- --run` (34 passed), `pnpm build`, and production-scope fixed-ID search returned no matches.

## 2026-06-11 jc-ai Reference Optimization Roadmap

- Added a docs-only optimization roadmap that borrows information architecture ideas from `jc-rag-kb-front` without copying React/Ant Design code.
- Frontend roadmap focus: teacher/admin evaluation workbench, expected-source/chunk annotation, Agent Trace scoring display, KB document index status, and reusable Citation/Trace/Eval/Index state components.
- Production streaming rule remains unchanged: no GET EventSource URL carrying `question`, `kbIds`, or tokens; Vue frontend must continue using backend-owned API wrappers and stream helpers.

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
| Student dashboard data source dehardcoding | Done | `docs/evidence/EVIDENCE-20260611-student-dashboard-data-source-dehardcoding.md` | Student page now uses backend courses/knowledge bases/documents and selection state; initial resources/trace are empty; no fabricated assessment question ID |
| AI QA no-source fallback UI | Done | `docs/evidence/EVIDENCE-20260611-ai-qa-no-source-fallback.md` | Student QA now displays `COURSE_GROUNDED` vs `GENERAL_FALLBACK` source status; no-source AI QA fallback is shown as a general answer without citations instead of ERROR/refusal |
| Student upload panel polish | Done | `docs/evidence/EVIDENCE-20260611-student-upload-panel-polish.md` | Knowledge-base upload failures now show a clear Chinese backend-connection message instead of raw `Failed to fetch`; upload panel styling aligns better with the GPT Web-like student workspace |
| Frontend UI redesign execution | Done | `docs/evidence/EVIDENCE-20260611-frontend-ui-redesign.md` | Student workspace now follows a GPT Web-like shell with left navigation, central course chat, bottom composer, and right activity/context panel; teacher/admin first screens were refocused; frontend tests/build passed |
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
| Frontend UI redesign plan | P1 | 2026-06-11 已形成 M 级前端 UI 修复计划，参考 Open edX、LearnHouse、Kotaemon、assistant-ui、Vuestic Admin；实施顺序为 build 修复、shell/移动端、学生端主工作流、教师审核流、管理端 triage、样式/组件收敛 |
| UI audit redesign follow-up | P1 | 2026-06-11 UI 审查认为当前前端更像接口验收台/中保真原型；需要后续 M 级改版，重点是移动端 shell、三角色首屏主任务区、降低双语/API 噪音、统一状态组件和管理端图表可信度 |
| Component extraction | P2 | `StatusPill`, `NoSourceCard`, `TraceTimeline`, `CitationPanel`, and admin chart placeholders are still page-level markup; extract after UI direction stabilizes |
| Reject review action | P1 | Teacher `Reject` button is disabled until review decision API supports `REJECTED` in frontend type contract |
