# Frontend AI Cockpit UI Design

## Goal

Refactor the existing Vue frontend into a restrained, high-density education AI cockpit. The interface should make the student learning loop, teacher review workflow, and admin operations triage clearer without changing backend API contracts or introducing a large UI framework.

## Confirmed Direction

Use Scheme A: restrained high-density education AI cockpit.

The product should feel like a professional LMS plus AI operations console: task-first, evidence-first, readable under repeated daily use, and not like a marketing landing page or generic developer dashboard. Keep the current Vue 3, TypeScript, Vite, Vue Router, and lucide icon stack. Do not add Naive UI, UnoCSS, Pinia, generated router systems, or large animation/3D dependencies.

## Current Problems

### Global Shell

`frontend/src/App.vue` currently treats all roles as sharing a permanent engineering console. `Backend only`, `MySQL 8 primary store`, and `agent_task + agent_trace` are useful implementation facts, but they are not the right default context for students and teachers.

### Student Workbench

`frontend/src/pages/student/StudentDashboard.vue` contains the right workflows, but the hierarchy is flat. The page gives workflow status, runtime metrics, RAG, citations, documents, resources, assessment, trace, and profile evidence nearly equal visual weight. The student's main tasks should be more obvious: ask a grounded question, inspect citations, follow the active path, use approved resources, and submit assessment reasoning.

### Teacher Review Queue

`frontend/src/pages/teacher/TeacherReviewQueue.vue` has a working selected-review decision flow, but the layout still gives too much space to static rubric content and not enough to the selected review as an evidence-based decision surface.

### Admin Operations

`frontend/src/pages/admin/AdminOperations.vue` loads real health and analytics APIs, but it reads like raw status lists instead of an operations triage screen. The top level should answer what needs attention, which dependency is degraded, and what review or learning activity is active.

## Design Requirements

1. Preserve every current API call, payload, route, and `data-test` selector used by `frontend/src/App.spec.ts`.
2. Preserve role routes:
   - `/`
   - `/teacher/reviews`
   - `/admin/operations`
3. Improve visual hierarchy without hiding required functional controls.
4. Keep technical trace and task information available, but demote it from primary visual weight on student and teacher pages.
5. Use responsive layouts that remain readable at desktop and mobile widths.
6. Keep UI copy aligned with implemented backend signals. Do not claim unavailable metrics such as model cost, trace coverage, citation rate, or index-health APIs.
7. Avoid nested cards and marketing-style hero composition. Sections should read as work surfaces, not landing-page panels.

## Proposed Information Architecture

### Global Shell

Refine the shell into a role navigation and context rail:

- Keep the brand and three role links.
- Replace the engineering console with role-aware task context:
  - Student: active learning loop, cited guidance, mastery update.
  - Teacher: critic review queue, release gate, feedback decision.
  - Admin: dependency health, analytics overview, review backlog.
- Move engineering facts into quieter metadata rows or admin-oriented language.

### Student Workbench

Rebalance the student page into three visual layers:

1. Primary learning action: RAG question and answer, citations, current path.
2. Supporting inputs: profile refinement, document upload, resource generation, assessment.
3. Diagnostic metadata: SSE stage, trace IDs, task IDs, agent trace.

Implementation should keep the current components in one page but change their layout and class semantics. The workflow strip can become a compact progress rail. Metric cards should become small summary tiles with student-facing labels. Resources should remain grouped by approved, pending, revision, and other states to preserve tests, but the four groups should feel like a compact review-status board rather than four large repeated shelves.

### Teacher Review Queue

Refactor the teacher page into an explicit review workspace:

- Queue list on one side.
- Selected review detail as the main decision area.
- Rubric items presented as a contextual checklist near the detail.
- Feedback textarea and approve/revision buttons remain scoped to `selectedReview`.

The page should make the current review target obvious and reduce accidental actions.

### Admin Operations

Refactor admin into a triage dashboard:

- Top summary: runtime health, review backlog, token activity, learning activity.
- Dependency matrix: application, database, redis, MinIO, model provider.
- Analytics panel: agent tasks, token usage, learning counts, review status counts.
- Metadata remains visible as secondary detail text, not as the main headline.

## Visual System

Use the existing CSS token approach in `frontend/src/style.css`, with stronger semantic classes:

- `context-rail`
- `summary-strip`
- `summary-tile`
- `task-panel`
- `evidence-card`
- `status-board`
- `resource-lane`
- `triage-grid`
- `review-workspace`

Keep the palette mixed and professional: light neutral work area, dark restrained shell, teal for success/learning, blue for retrieval/action, amber for pending, red for errors, muted gray for locked/unknown states. Avoid a one-note blue/purple or beige palette.

## File Boundaries

Modify:

- `frontend/src/App.vue`: role-aware shell context only. Keep this as a small route shell; do not split it into new shell components during the first pass.
- `frontend/src/pages/student/StudentDashboard.vue`: layout class names and semantic grouping while preserving state and API behavior.
- `frontend/src/pages/teacher/TeacherReviewQueue.vue`: review workspace hierarchy.
- `frontend/src/pages/admin/AdminOperations.vue`: triage hierarchy.
- `frontend/src/style.css`: visual system, responsive layout, status board, triage, review workspace, and shell refinements.
- `frontend/src/App.spec.ts`: only add or adjust tests if a new stable UI contract is needed; do not weaken existing API/behavior tests.

Do not modify backend code for this UI refactor.

Keep stable:

- `frontend/src/router.ts`
- `frontend/src/api/*`
- `frontend/src/types/api.ts`
- Existing route names, paths, request payloads, and tested `data-test` selectors.

The first implementation pass should prioritize `StudentDashboard.vue` and `style.css`, because they carry the largest UI and layout complexity. Teacher and admin pages should receive page-level hierarchy improvements, not deep component extraction, unless doing so is required to preserve readability.

## Testing And Verification

Automated verification:

- `npm test -- --run`
- `npm run build`

Browser verification:

- Start or reuse the Vite dev server.
- Check student, teacher, and admin pages at desktop width.
- Check student page at mobile width.
- Confirm there are no blank screens, major overlaps, unreadable cards, or text overflow inside controls.
- Confirm primary controls remain reachable:
  - Run RAG Chat
  - Upload document
  - Generate resources
  - Submit answer
  - Approve selected
  - Request revision
  - Refresh operations

## Non-Goals

- No new UI framework.
- No full route split of the student workbench.
- No new backend endpoints.
- No marketing landing page.
- No decorative 3D, oversized hero, or purely aesthetic animation pass.
- No removal of trace/debug data; only visual demotion.

## Acceptance Criteria

1. The UI presents the three roles as distinct work surfaces with clearer hierarchy.
2. Student page primary actions are easier to scan than diagnostic metadata.
3. Teacher review page makes selected review and decision controls visually dominant.
4. Admin page reads as an operations triage dashboard rather than raw backend lists.
5. Existing tests pass without weakening coverage.
6. Production build passes.
7. Browser screenshots at desktop and mobile show coherent responsive layout without overlapping UI.
