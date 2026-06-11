# Frontend AI Cockpit UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor the existing Vue frontend into a restrained, high-density education AI cockpit while preserving tested routes, API payloads, and interaction contracts.

**Architecture:** Keep the current route shell and API/type layers stable. Improve the UI through page-level semantic layout changes in `App.vue`, `StudentDashboard.vue`, `TeacherReviewQueue.vue`, `AdminOperations.vue`, and a consolidated visual system update in `style.css`, with tests guarding stable selectors and behavior.

**Tech Stack:** Vue 3, TypeScript, Vite, Vue Router, Vitest, Vue Test Utils, lucide-vue-next, global CSS.

---

## File Structure

- Modify `frontend/src/App.vue`: keep the small shell, but replace engineering console copy with role-aware context content.
- Modify `frontend/src/pages/student/StudentDashboard.vue`: preserve state, API orchestration, computed values, and `data-test` selectors; update template hierarchy and semantic classes.
- Modify `frontend/src/pages/teacher/TeacherReviewQueue.vue`: keep selected-review decision logic; update review workspace layout and evidence hierarchy.
- Modify `frontend/src/pages/admin/AdminOperations.vue`: keep health/analytics API usage; update triage hierarchy and semantic grouping.
- Modify `frontend/src/style.css`: add the AI cockpit visual system, responsive rules, status board, review workspace, and admin triage styles.
- Modify `frontend/src/App.spec.ts`: add UI hierarchy contract tests without weakening existing behavior tests.
- Keep stable `frontend/src/router.ts`, `frontend/src/api/*`, `frontend/src/types/api.ts`, and backend files.

## Task 1: Add UI Hierarchy Contract Tests

**Files:**
- Modify: `frontend/src/App.spec.ts`

- [ ] **Step 1: Add the student cockpit hierarchy test**

Add this test inside `describe('AI Learning OS workbench', () => { ... })`, near the existing default render test:

```ts
  it('presents the student page as a learning cockpit instead of a debug console', async () => {
    const wrapper = await mountApp()
    await flushPromises()

    expect(wrapper.find('[data-test="student-primary-workspace"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="student-support-workspace"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="student-diagnostics"]').exists()).toBe(true)
    expect(wrapper.get('[data-test="student-primary-workspace"]').text()).toContain('RAG Chat')
    expect(wrapper.get('[data-test="student-primary-workspace"]').text()).toContain('Learning Path')
    expect(wrapper.get('[data-test="student-support-workspace"]').text()).toContain('Learning Profile')
    expect(wrapper.get('[data-test="student-diagnostics"]').text()).toContain('Agent Trace')
  })
```

- [ ] **Step 2: Add shell, teacher, and admin UI tests**

Add this test near the route test:

```ts
  it('uses role-aware shell context and triage-oriented role pages', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(existingKnowledgeBaseEnvelope())
      .mockResolvedValueOnce(emptyKnowledgeBaseDocumentsEnvelope())
      .mockResolvedValueOnce(profileEnvelope())
      .mockResolvedValueOnce(pathEnvelope())
      .mockResolvedValueOnce(apiEnvelope([]))
      .mockResolvedValueOnce(healthEnvelope())
      .mockResolvedValueOnce(analyticsEnvelope())

    const wrapper = await mountApp()
    await flushPromises()

    expect(wrapper.get('[data-test="shell-context"]').text()).toContain('Active learning loop')

    await wrapper.get('[data-test="teacher-view"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-test="shell-context"]').text()).toContain('Critic review queue')
    expect(wrapper.find('[data-test="teacher-review-workspace"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="teacher-evidence-checklist"]').exists()).toBe(true)

    await wrapper.get('[data-test="admin-view"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-test="shell-context"]').text()).toContain('Dependency health')
    expect(wrapper.find('[data-test="admin-triage"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="admin-dependency-matrix"]').exists()).toBe(true)
  })
```

- [ ] **Step 3: Run focused tests to verify RED**

Run:

```bash
cd frontend
npm test -- --run src/App.spec.ts -t "student page as a learning cockpit|role-aware shell context"
```

Expected: both new tests fail because `student-primary-workspace`, `student-support-workspace`, `student-diagnostics`, `shell-context`, `teacher-review-workspace`, `teacher-evidence-checklist`, `admin-triage`, and `admin-dependency-matrix` do not exist yet.

## Task 2: Refine Global Shell Context

**Files:**
- Modify: `frontend/src/App.vue`
- Modify: `frontend/src/style.css`

- [ ] **Step 1: Add route-aware context data in `App.vue`**

Add this computed block after `activeLabel`:

```ts
const shellContext = computed(() => {
  if (route.name === 'teacher') {
    return {
      eyebrow: 'Teacher cockpit',
      title: 'Critic review queue',
      items: [
        ['Release gate', 'Approve or request revision'],
        ['Evidence focus', 'Citation, learner fit, safety'],
        ['Decision scope', 'Selected resource only'],
      ],
    }
  }

  if (route.name === 'admin') {
    return {
      eyebrow: 'Operations cockpit',
      title: 'Dependency health',
      items: [
        ['Signals', 'Health and analytics overview'],
        ['Triage', 'Backlog, tokens, learning activity'],
        ['Scope', 'Current backend APIs only'],
      ],
    }
  }

  return {
    eyebrow: 'Learner cockpit',
    title: 'Active learning loop',
    items: [
      ['Primary task', 'Ask, cite, practice'],
      ['Evidence', 'Course sources and mastery'],
      ['Release state', 'Teacher-reviewed resources'],
    ],
  }
})
```

- [ ] **Step 2: Replace the sidebar console markup**

Replace the current `<section class="sidebar-panel" aria-labelledby="console-title">...</section>` with:

```vue
      <section class="sidebar-panel context-rail" aria-labelledby="console-title" data-test="shell-context">
        <p id="console-title" class="eyebrow">{{ shellContext.eyebrow }}</p>
        <h2>{{ shellContext.title }}</h2>
        <dl class="state-list">
          <div v-for="[label, value] in shellContext.items" :key="label">
            <dt>{{ label }}</dt>
            <dd>{{ value }}</dd>
          </div>
          <div>
            <dt>Active page</dt>
            <dd>{{ activeLabel }}</dd>
          </div>
        </dl>
      </section>
```

- [ ] **Step 3: Add shell context styles**

In `frontend/src/style.css`, add styles for `.context-rail h2` near `.sidebar-panel`:

```css
.context-rail h2 {
  margin-top: 4px;
  color: #f8fbff;
  font-size: 18px;
  line-height: 1.25;
  letter-spacing: 0;
}
```

- [ ] **Step 4: Run focused shell test**

Run:

```bash
cd frontend
npm test -- --run src/App.spec.ts -t "role-aware shell context"
```

Expected: shell assertions pass; teacher/admin workspace assertions still fail until later tasks.

## Task 3: Rebalance Student Workbench Template

**Files:**
- Modify: `frontend/src/pages/student/StudentDashboard.vue`
- Modify: `frontend/src/style.css`

- [ ] **Step 1: Update the student header and workflow section**

In `StudentDashboard.vue`, keep the existing button with `data-test="ask-rag"`, but change the header note to:

```vue
        <p class="header-note">Ask course-grounded questions, inspect citations, follow the active path, and release only teacher-reviewed resources.</p>
```

Change `<section class="workflow-strip" aria-label="Workflow status">` to:

```vue
    <section class="workflow-strip compact-progress" aria-label="Workflow status">
```

- [ ] **Step 2: Change the runtime summary into cockpit summary tiles**

Change `<section class="metric-row" aria-label="Runtime summary">` to:

```vue
    <section class="summary-strip" aria-label="Learning cockpit summary">
```

Keep all six `article` children and current interpolations so tests still see the same state values.

- [ ] **Step 3: Create the three student workspace layers**

Replace the single wrapper:

```vue
    <section class="workbench-grid">
```

with:

```vue
    <section class="student-cockpit">
      <div class="student-primary-workspace" data-test="student-primary-workspace">
```

Close this first div after the current `path-panel` article.

Then wrap the existing `profile-panel`, `kb-panel`, `resource-panel`, `assessment-panel`, and `profile-dimensions-panel` articles in:

```vue
      <div class="student-support-workspace" data-test="student-support-workspace">
```

Wrap the existing `trace-panel` article in:

```vue
      <div class="student-diagnostics" data-test="student-diagnostics">
```

The final structure should be:

```vue
    <section class="student-cockpit">
      <div class="student-primary-workspace" data-test="student-primary-workspace">
        <!-- chat-panel, citation-panel, path-panel -->
      </div>
      <div class="student-support-workspace" data-test="student-support-workspace">
        <!-- profile-panel, kb-panel, resource-panel, assessment-panel, profile-dimensions-panel -->
      </div>
      <div class="student-diagnostics" data-test="student-diagnostics">
        <!-- trace-panel -->
      </div>
    </section>
```

- [ ] **Step 4: Convert resource shelves into a status board without changing selectors**

In the `resource-panel`, wrap the four existing sections with:

```vue
        <div class="status-board resource-status-board" aria-label="Resource review status board">
          <!-- existing resource-shelf approved, pending, revision, other sections -->
        </div>
```

Keep every existing `data-test="resource-shelf-*"` value.

- [ ] **Step 5: Add student cockpit layout styles**

Add these styles in `style.css` near `.workbench-grid`:

```css
.summary-strip {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
}

.summary-strip article {
  display: grid;
  gap: 3px;
  min-height: 76px;
  padding: 12px;
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: 8px;
  box-shadow: var(--shadow);
}

.summary-strip span {
  color: var(--muted);
  font-size: 11px;
  font-weight: 800;
  text-transform: uppercase;
}

.summary-strip strong {
  color: var(--ink);
  font-size: 18px;
  line-height: 1.2;
  overflow-wrap: anywhere;
}

.summary-strip p {
  color: var(--muted);
  font-size: 12px;
}

.student-cockpit {
  display: grid;
  gap: 14px;
}

.student-primary-workspace,
.student-support-workspace {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 12px;
}

.student-diagnostics {
  display: grid;
}

.student-primary-workspace .chat-panel {
  grid-column: span 5;
}

.student-primary-workspace .citation-panel {
  grid-column: span 3;
}

.student-primary-workspace .path-panel {
  grid-column: span 4;
}

.student-support-workspace .profile-panel,
.student-support-workspace .kb-panel {
  grid-column: span 4;
}

.student-support-workspace .resource-panel {
  grid-column: span 8;
}

.student-support-workspace .assessment-panel,
.student-support-workspace .profile-dimensions-panel {
  grid-column: span 6;
}

.student-diagnostics .trace-panel {
  min-height: 0;
}

.status-board {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}
```

- [ ] **Step 6: Run focused student test**

Run:

```bash
cd frontend
npm test -- --run src/App.spec.ts -t "student page as a learning cockpit"
```

Expected: PASS.

## Task 4: Refactor Teacher Review Workspace Hierarchy

**Files:**
- Modify: `frontend/src/pages/teacher/TeacherReviewQueue.vue`
- Modify: `frontend/src/style.css`

- [ ] **Step 1: Add teacher workspace test anchors**

Change:

```vue
    <section class="review-layout">
```

to:

```vue
    <section class="review-layout review-workspace" data-test="teacher-review-workspace">
```

Change:

```vue
        <ul class="rubric-list">
```

to:

```vue
        <ul class="rubric-list evidence-checklist" data-test="teacher-evidence-checklist">
```

- [ ] **Step 2: Make the selected detail visually dominant**

Add `decision-surface` to the selected review section:

```vue
        <section v-if="selectedReview" class="review-detail decision-surface" data-test="review-detail">
```

Keep `data-test="review-feedback-input"`, `data-test="approve-selected-review"`, `data-test="request-revision"`, and `data-test="select-review-${review.reviewId}"`.

- [ ] **Step 3: Add review workspace styles**

Add these styles near existing `.review-layout` styles:

```css
.review-workspace {
  align-items: start;
}

.review-workspace .review-panel {
  position: sticky;
  top: 18px;
}

.evidence-checklist li {
  background: #f8fbff;
}

.decision-surface {
  margin-top: 4px;
  padding: 14px;
  background: #fbfdff;
  border: 1px solid #c9d8f2;
  border-radius: 8px;
}

.decision-surface h3 {
  color: var(--ink);
  font-size: 20px;
}
```

- [ ] **Step 4: Run focused teacher tests**

Run:

```bash
cd frontend
npm test -- --run src/App.spec.ts -t "role-aware shell context|teacher review details|keeps review feedback scoped"
```

Expected: PASS.

## Task 5: Refactor Admin Operations Into Triage

**Files:**
- Modify: `frontend/src/pages/admin/AdminOperations.vue`
- Modify: `frontend/src/style.css`

- [ ] **Step 1: Add admin triage wrappers**

Change:

```vue
    <section class="metric-row">
```

to:

```vue
    <section class="summary-strip admin-triage" data-test="admin-triage">
```

Change:

```vue
      <article class="panel admin-health-panel">
```

to:

```vue
      <article class="panel admin-health-panel triage-panel" data-test="admin-dependency-matrix">
```

Change:

```vue
      <article class="panel admin-cost-panel">
```

to:

```vue
      <article class="panel admin-cost-panel triage-panel">
```

- [ ] **Step 2: Refine admin copy without changing backend signal claims**

Update the admin header note to:

```vue
        <p class="header-note">Triage live backend health, review backlog, token activity, and learning activity from the current APIs.</p>
```

Keep strings required by existing tests: `Available signals`, `Dependency health`, `Token activity`, `Review backlog`, `Learning activity`, and `Analytics overview count`.

- [ ] **Step 3: Add admin triage styles**

Add:

```css
.admin-triage {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.triage-panel .document-list li {
  background: #f8fbff;
}
```

- [ ] **Step 4: Run focused admin tests**

Run:

```bash
cd frontend
npm test -- --run src/App.spec.ts -t "role-aware shell context|admin dependency health|admin copy"
```

Expected: PASS.

## Task 6: Final Responsive CSS Pass

**Files:**
- Modify: `frontend/src/style.css`

- [ ] **Step 1: Update the 1220px breakpoint**

Inside `@media (max-width: 1220px)`, add:

```css
  .summary-strip,
  .admin-triage {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .student-primary-workspace,
  .student-support-workspace {
    grid-template-columns: repeat(6, minmax(0, 1fr));
  }

  .student-primary-workspace .chat-panel,
  .student-primary-workspace .citation-panel,
  .student-primary-workspace .path-panel,
  .student-support-workspace .profile-panel,
  .student-support-workspace .kb-panel,
  .student-support-workspace .resource-panel,
  .student-support-workspace .assessment-panel,
  .student-support-workspace .profile-dimensions-panel {
    grid-column: span 6;
  }

  .status-board {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
```

- [ ] **Step 2: Update the 760px breakpoint**

Inside `@media (max-width: 760px)`, add:

```css
  .summary-strip,
  .admin-triage,
  .student-primary-workspace,
  .student-support-workspace,
  .status-board {
    grid-template-columns: 1fr;
  }

  .student-primary-workspace .chat-panel,
  .student-primary-workspace .citation-panel,
  .student-primary-workspace .path-panel,
  .student-support-workspace .profile-panel,
  .student-support-workspace .kb-panel,
  .student-support-workspace .resource-panel,
  .student-support-workspace .assessment-panel,
  .student-support-workspace .profile-dimensions-panel {
    grid-column: span 1;
  }
```

- [ ] **Step 3: Run the full frontend test suite**

Run:

```bash
cd frontend
npm test -- --run
```

Expected: all 26 tests pass after the two new tests are added.

- [ ] **Step 4: Run the production build**

Run:

```bash
cd frontend
npm run build
```

Expected: `vue-tsc -b && vite build` exits 0.

## Task 7: Browser Visual Verification

**Files:**
- No source file changes expected.

- [ ] **Step 1: Start or reuse a dev server**

Run:

```bash
cd frontend
npm run dev -- --host 127.0.0.1
```

Use the reported local URL. If an existing server is already reachable, reuse it.

- [ ] **Step 2: Check desktop pages**

Open these routes at `1440x900`:

- `/`
- `/teacher/reviews`
- `/admin/operations`

Expected: no blank screens, no major overlapping UI, no unreadable controls, and active sidebar state is visible.

- [ ] **Step 3: Check mobile student page**

Open `/` at `375x844`.

Expected: no horizontal scrolling, summary tiles stack, resource lanes stack, and primary controls remain visible.

- [ ] **Step 4: Record verification result**

If screenshots are saved for inspection, delete temporary screenshots after review unless they are needed as final evidence.

## Self-Review

- Spec coverage: Tasks cover global shell, student workbench, teacher queue, admin operations, CSS responsiveness, tests, build, and browser verification.
- Placeholder scan: No placeholder tasks remain; each task names exact files, selectors, commands, and expected results.
- Type consistency: No new TypeScript types are required. New selectors are `student-primary-workspace`, `student-support-workspace`, `student-diagnostics`, `shell-context`, `teacher-review-workspace`, `teacher-evidence-checklist`, `admin-triage`, and `admin-dependency-matrix`.
- Scope check: Plan keeps `router.ts`, `api/*`, `types/api.ts`, and backend stable, matching the design boundary.
