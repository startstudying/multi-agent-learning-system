# Soybean Frontend Uplift Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Improve the existing Vue learning workbench using the team-provided Soybean Admin frontend as a design reference while preserving the current backend API integration.

**Architecture:** Keep the current `frontend/` stack and route structure. Borrow the Soybean education-agent interaction model as product guidance: editable profile dialogue, clearer resource review states, and operations signals that match real API data. Avoid importing Soybean's Naive UI, UnoCSS, Pinia, workspace packages, or generated router.

**Tech Stack:** Vue 3, TypeScript, Vite, Vue Router, Vitest, Vue Test Utils, lucide-vue-next, existing backend REST/SSE APIs.

---

## File Structure

- Modify `frontend/src/types/api.ts`: add student-facing follow-up questions and review-status values to `WorkbenchState` and resource models.
- Modify `frontend/src/api/resources.ts`: add `fetchResourceGenerationTask(taskId)` for the documented `GET /api/resources/generation-tasks/{taskId}` endpoint.
- Modify `frontend/src/pages/student/StudentDashboard.vue`: add editable profile intake, render follow-up questions, support re-extraction, split generated resources by review status, and add review-status refresh.
- Modify `frontend/src/pages/admin/AdminOperations.vue`: narrow copy to the actual health and analytics API contract.
- Modify `frontend/src/App.spec.ts`: add red-green tests for follow-up questions, resource review status grouping/refresh, and truthful admin copy.
- Modify `frontend/src/style.css`: improve cockpit layout, profile dialogue controls, resource review shelves, and compact responsive behavior.

## Tasks

### Task 1: Profile Follow-Up Loop

**Files:**
- Modify: `frontend/src/types/api.ts`
- Modify: `frontend/src/pages/student/StudentDashboard.vue`
- Test: `frontend/src/App.spec.ts`

- [x] **Step 1: Write the failing test**

Add a test asserting that backend `followUpQuestions` render and a learner-selected follow-up is sent to `POST /api/profile/dialogue/extract`.

- [x] **Step 2: Run test to verify it fails**

Run: `cd frontend; npm test -- --run src/App.spec.ts -t "renders profile follow-up questions"`

Expected: FAIL because follow-up questions are not rendered.

- [x] **Step 3: Implement minimal profile dialogue**

Store `profilePrompt`, `followUpQuestions`, and `selectedFollowUpQuestion` in workbench state; render an editable textarea and follow-up chips; add a `refineProfile()` action that calls `extractProfile` with the learner-entered message.

- [x] **Step 4: Run test to verify it passes**

Run: `cd frontend; npm test -- --run src/App.spec.ts -t "renders profile follow-up questions"`

Expected: PASS.

### Task 2: Student Resource Review Shelf

**Files:**
- Modify: `frontend/src/types/api.ts`
- Modify: `frontend/src/api/resources.ts`
- Modify: `frontend/src/pages/student/StudentDashboard.vue`
- Test: `frontend/src/App.spec.ts`

- [x] **Step 1: Write the failing test**

Add a test asserting that `PENDING_CRITIC`, `APPROVED`, and `REVISION_REQUESTED` resources render in separate student shelves, and that refresh calls `GET /api/resources/generation-tasks/{taskId}`.

- [x] **Step 2: Run test to verify it fails**

Run: `cd frontend; npm test -- --run src/App.spec.ts -t "groups student resources by review status"`

Expected: FAIL because the current UI uses a single generated-resource list and has no refresh action.

- [x] **Step 3: Implement minimal resource shelf**

Add `fetchResourceGenerationTask`; normalize resource review statuses; add computed `approvedResources`, `pendingResourcesForReview`, and `revisionResources`; render three sections and a "Check review status" action for generated task refresh. Follow-up review tightened this by preserving unknown backend review statuses in an "Other review states" shelf and surfacing task-level status, review status, progress, and safety.

- [x] **Step 4: Run test to verify it passes**

Run: `cd frontend; npm test -- --run src/App.spec.ts -t "groups student resources by review status"`

Expected: PASS.

### Task 3: Admin Truthful Operations Copy

**Files:**
- Modify: `frontend/src/pages/admin/AdminOperations.vue`
- Test: `frontend/src/App.spec.ts`

- [x] **Step 1: Write the failing test**

Add a test asserting admin operations shows "Available signals" and does not claim model cost, trace coverage, citation rate, or index health until those APIs exist.

- [x] **Step 2: Run test to verify it fails**

Run: `cd frontend; npm test -- --run src/App.spec.ts -t "keeps admin copy aligned"`

Expected: FAIL because current header copy overstates available telemetry.

- [x] **Step 3: Implement minimal copy correction**

Update page heading, descriptions, and labels to match `/api/health` and `/api/analytics/overview`: dependency health, token activity, review backlog, learning activity. Follow-up review also removed "telemetry" language from current admin UI/docs where richer trace/cost/citation/index APIs do not exist yet.

- [x] **Step 4: Run test to verify it passes**

Run: `cd frontend; npm test -- --run src/App.spec.ts -t "keeps admin copy aligned"`

Expected: PASS.

### Task 4: Visual Polish From Soybean Reference

**Files:**
- Modify: `frontend/src/style.css`
- Test: `frontend/src/App.spec.ts`

- [x] **Step 1: Preserve behavior tests**

Run: `cd frontend; npm test -- --run src/App.spec.ts`

Expected: PASS before styling-only work.

- [x] **Step 2: Apply scoped style refinements**

Improve hierarchy with a cockpit next-action band, segmented resource shelves, profile dialogue panel, and denser operations cards. Keep colors restrained and avoid adding new dependencies. Follow-up review added teacher review detail/feedback controls, task summary styling, stronger disabled/focus states, and explicit follow-up selection feedback.

- [x] **Step 3: Re-run frontend verification**

Run: `cd frontend; npm test -- --run src/App.spec.ts`

Expected: PASS.

### Task 5: Final Verification

**Files:**
- Verify only.

- [x] **Step 1: Run frontend tests**

Run: `cd frontend; npm test -- --run`

Expected: all tests pass.

- [x] **Step 2: Run frontend build**

Run: `cd frontend; npm run build`

Expected: TypeScript and Vite build exit 0.

- [x] **Step 3: Run backend gates if frontend API contract changed**

Run: `cd backend; mvn test`

Expected: backend tests pass.

Result: PASS. `cd backend; mvn test` ran 39 tests with 0 failures and 0 errors.

- [x] **Step 4: Completion audit**

Check the objective against current evidence: Soybean reference inspected, multi-agent review used, frontend shortcomings improved, tests/build fresh.
