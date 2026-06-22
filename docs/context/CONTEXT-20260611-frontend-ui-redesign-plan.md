# 前端 UI 修复 Context Pack

## 当前任务

基于 UI 审查和外部参考，规划 AI Learning OS 前端 UI 改版。当前轮次只写计划，不实施代码。

## 相关文档

- `docs/evidence/EVIDENCE-20260611-frontend-ui-audit.md`
- `docs/research/github-references/REF-20260611-frontend-ui-redesign.md`
- `docs/product/PRD-20260611-frontend-ui-redesign-plan.md`
- `docs/requirements/REQ-20260611-frontend-ui-redesign-plan.md`
- `docs/specs/SPEC-20260611-frontend-ui-redesign-plan.md`
- `docs/plans/PLAN-20260611-frontend-ui-redesign-plan.md`
- `docs/tasks/TASK-20260611-frontend-ui-redesign-plan.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/FRONTEND_MEMORY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/harness/TEST_COMMANDS.md`

## 参考项目

- Open edX Learning MFE: https://github.com/openedx/frontend-app-learning
- LearnHouse: https://github.com/learnhouse/learnhouse
- Kotaemon: https://github.com/Cinnamon/kotaemon
- assistant-ui: https://github.com/assistant-ui/assistant-ui
- Vuestic Admin: https://github.com/epicmaxco/vuestic-admin

## Allowed Files For Future Implementation

- `frontend/src/api/analytics.ts`
- `frontend/src/App.vue`
- `frontend/src/style.css`
- `frontend/src/App.spec.ts`
- `frontend/src/pages/student/StudentDashboard.vue`
- `frontend/src/pages/teacher/TeacherReviewQueue.vue`
- `frontend/src/pages/admin/AdminOperations.vue`
- `frontend/src/pages/admin/AdminModelProviders.vue`
- `frontend/src/components/*.vue`

## Disallowed Files

- `backend/**`
- `frontend/src/api/client.ts`
- `frontend/src/types/api.ts`
- `frontend/package.json`
- `frontend/pnpm-lock.yaml`
- 数据库 migration

## Test Commands

```bash
cd frontend && pnpm test
cd frontend && pnpm build
cd frontend && pnpm dev
```

## Architecture Rules

- 前端不直连 LLM。
- 前端不存 API key。
- API 调用走共享 request wrapper。
- AI streaming 保持现有封装。
- RAG sources 和 traceId 保持可见，但降为证据/诊断层。

## Current Known Failure

`cd frontend && pnpm build` 当前失败：

```text
src/api/analytics.ts(1,10): error TS6133: 'apiRequest' is declared but its value is never read.
```

后续实施必须先修复该问题。
