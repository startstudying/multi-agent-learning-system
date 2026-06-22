# TASK-20260611 前端角色工作台分离

## 目标

解决学生、教师、管理员界面边界不清的问题，让每个角色拥有独立的工作台壳和角色专属业务导航。

## 任务类型

前端可见行为调整。

## 选用技能

- `feature-development-workflow`：按 M 级流程创建文档、实现、验证和记录。
- `frontend-design`：保证角色工作台壳的视觉和交互边界清晰。
- `vue3-component-design`：按 Vue 3 + Router 现有结构改造。

## Size Decision

Size: M。

原因：修改前端全局工作台壳和测试，属于可见行为调整；不改 API、DTO、数据库、依赖或后端合同。

## Context Pack

### 相关记忆和文档

- `docs/memory/PROJECT_MEMORY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/specs/SPEC-20260611-frontend-role-workspace-separation.md`

### 允许修改

- `frontend/src/App.vue`
- `frontend/src/style.css`
- `frontend/src/App.spec.ts`
- `docs/requirements/REQ-20260611-frontend-role-workspace-separation.md`
- `docs/specs/SPEC-20260611-frontend-role-workspace-separation.md`
- `docs/plans/PLAN-20260611-frontend-role-workspace-separation.md`
- `docs/tasks/TASK-20260611-frontend-role-workspace-separation.md`
- `docs/context/CONTEXT-20260611-frontend-role-workspace-separation.md`
- `docs/evidence/EVIDENCE-20260611-frontend-role-workspace-separation.md`
- `docs/acceptance/ACCEPT-20260611-frontend-role-workspace-separation.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`

### 禁止修改

- 后端代码。
- API DTO 和 API path。
- 数据库 schema。
- 新依赖配置。
- `StudentDashboard.vue` 内部业务流程。

### 测试命令

- `cd frontend && pnpm test -- --run`
- `cd frontend && pnpm build`

## 验收标准

- 学生、教师、管理员业务侧栏不再混用同一组项目和聊天入口。
- 教师页不显示学生学习业务侧栏。
- 管理员页不显示学生或教师业务侧栏。
- 管理员模型供应商页保留管理员二级入口。
- 测试和构建通过。
