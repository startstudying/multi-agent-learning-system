# 自适应掌握度模型 Context Pack

## 允许修改

- `backend/src/main/java/com/learningos/assessment/application/AssessmentFeedbackService.java`
- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java`
- `backend/src/test/java/com/learningos/assessment/application/AssessmentFeedbackServiceTest.java`
- `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java`
- `docs/planning/backend-architecture-todolist.md`
- `docs/product/PRD-20260605-adaptive-mastery-model.md`
- `docs/requirements/REQ-20260605-adaptive-mastery-model.md`
- `docs/specs/SPEC-20260605-adaptive-mastery-model.md`
- `docs/plans/PLAN-20260605-adaptive-mastery-model.md`
- `docs/tasks/TASK-20260605-adaptive-mastery-model.md`
- `docs/evidence/EVIDENCE-20260605-adaptive-mastery-model.md`
- `docs/acceptance/ACCEPT-20260605-adaptive-mastery-model.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/changelog/CHANGELOG.md`
- `docs/subagents/runs/RUN-20260605-adaptive-mastery-model.md`

## 不允许修改

- 数据库迁移脚本。
- 前端代码。
- RAG、agent、analytics 业务代码。
- 依赖配置。

## 架构约束

- Controller 不新增业务逻辑。
- Service 层负责读取 repository 和业务编排。
- 不新增外部依赖。
- 不用 prompt 控制权限或掌握度。
- 保留现有 API 合同。
