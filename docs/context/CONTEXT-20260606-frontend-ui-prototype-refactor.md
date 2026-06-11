# Context Pack：前端中低保真 UI 原型重构

## 当前任务边界

基于用户提供的 5 张低保真原型和 1 张视觉风格稿，将现有 Vue 前端重构为中文中低保真 SaaS 后台原型。只修改前端页面、样式、测试和交付文档。

## 相关记忆和文档

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/FRONTEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/product/PRD-20260606-frontend-ui-prototype-refactor.md`
- `docs/requirements/REQ-20260606-frontend-ui-prototype-refactor.md`
- `docs/specs/SPEC-20260606-frontend-ui-prototype-refactor.md`
- `docs/plans/PLAN-20260606-frontend-ui-prototype-refactor.md`
- `docs/tasks/TASK-20260606-frontend-ui-prototype-refactor.md`

## 选用技能

- feature-development-workflow
- frontend-design
- vue-edu-admin-frontend
- vue-ai-learning-ui
- rag-citation-viewer
- agent-trace-design
- test-driven-development
- verification-before-completion

## Subagent Plan

不启用 subagent。原因：本轮只涉及前端一个模块，不涉及后端、数据库、API contract 或安全实现。

## 允许修改文件

- `frontend/src/App.vue`
- `frontend/src/pages/student/StudentDashboard.vue`
- `frontend/src/pages/teacher/TeacherReviewQueue.vue`
- `frontend/src/pages/admin/AdminOperations.vue`
- `frontend/src/style.css`
- `frontend/src/App.spec.ts`
- `docs/tasks/TASK-20260606-frontend-ui-prototype-refactor.md`
- `docs/context/CONTEXT-20260606-frontend-ui-prototype-refactor.md`
- `docs/evidence/EVIDENCE-20260606-frontend-ui-prototype-refactor.md`
- `docs/acceptance/ACCEPT-20260606-frontend-ui-prototype-refactor.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/FRONTEND_MEMORY.md`
- `docs/retrospectives/RETRO-20260606-frontend-ui-prototype-refactor.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/skills/project-specific/vue-ai-learning-ui.md`

## 禁止修改文件

- `backend/**`
- `frontend/src/api/**`
- `frontend/src/router.ts`
- `frontend/src/types/api.ts`
- `frontend/package.json`
- `frontend/package-lock.json`
- `docs/superpowers/**`
- `docs/planning/backend-architecture-todolist.md`

## 测试命令

```bash
cd frontend && npm test -- --run
cd frontend && npm run build
```

## 当前任务边界说明

- 可以展示目标态接口来源说明，但不新增真实 fetch。
- 可以展示禁用态目标按钮，但不调用未实现 API。
- 可以使用 CSS 图表占位，不新增图表库。
- 所有页面文字以中文为主，API path、traceId、chunkId、documentId、状态枚举保持英文。

## 架构漂移检查

| 检查 | 预期 |
|---|---|
| Frontend 不直连 LLM | PASS |
| Frontend 不保存 API key | PASS |
| API 调用仍走 shared client | PASS |
| 不新增依赖 | PASS |
| 不改后端/API/DB | PASS |
