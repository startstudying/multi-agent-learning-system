# TASK-20260611 学生端数据源去硬编码

## 状态

Completed on 2026-06-11。

Evidence: `docs/evidence/EVIDENCE-20260611-student-dashboard-data-source-dehardcoding.md`

Acceptance: `docs/acceptance/ACCEPT-20260611-student-dashboard-data-source-dehardcoding.md`

## 目标

完成 `PLAN-20260611-student-dashboard-data-source-dehardcoding.md`：把学生端从演示固定数据改为后端数据和选择态驱动。

## 任务类型

前端重构 + 数据源去硬编码。

## 选用技能

- `feature-development-workflow`
- `vue3-component-design`
- `ai-streaming-ui`
- `rag-project-review`
- `agent-trace-design`
- `test-driven-development`
- `test-generator`

## Size Decision

Size: M。

原因：影响学生端一个 substantial module、API/types/test，改变可见行为；不改后端 API、DTO、DB、依赖。

## Context Pack

### 相关记忆和文档

- `docs/plans/PLAN-20260611-student-dashboard-data-source-dehardcoding.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/FRONTEND_MEMORY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`

### 允许修改

- `frontend/src/api/client.ts`
- `frontend/src/api/courses.ts`
- `frontend/src/composables/useStudentWorkbench.ts`
- `frontend/src/pages/student/StudentDashboard.vue`
- `frontend/src/pages/student/components/*.vue`
- `frontend/src/types/api.ts`
- `frontend/src/App.spec.ts`
- `docs/requirements/REQ-20260611-student-dashboard-data-source-dehardcoding.md`
- `docs/specs/SPEC-20260611-student-dashboard-data-source-dehardcoding.md`
- `docs/plans/PLAN-20260611-student-dashboard-data-source-dehardcoding.md`
- `docs/tasks/TASK-20260611-student-dashboard-data-source-dehardcoding.md`
- `docs/context/CONTEXT-20260611-student-dashboard-data-source-dehardcoding.md`
- `docs/evidence/EVIDENCE-20260611-student-dashboard-data-source-dehardcoding.md`
- `docs/acceptance/ACCEPT-20260611-student-dashboard-data-source-dehardcoding.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/FRONTEND_MEMORY.md`

### 禁止修改

- 后端 production code。
- 数据库 migration。
- 新依赖配置。
- 教师和管理员页面，除非测试挂载必须且不改行为。

### 测试命令

- `cd frontend && pnpm test -- --run`
- `cd frontend && pnpm build`
- `rg -n "stu_001|kb_java_backend|goal_java_backend|kp_sql_join|q_sql_join_cardinality|res_local|trc_resource_local" frontend/src/pages/student frontend/src/api frontend/src/types`

## 验收标准

- 生产学生端不再出现固定演示 ID。
- 学生端从真实 API 初始化课程、知识库和文档。
- 初始 resources/trace 为空状态。
- RAG/AI QA/上传/资源生成/测评均使用选择态或明确空状态。
- 前端测试和构建通过。
