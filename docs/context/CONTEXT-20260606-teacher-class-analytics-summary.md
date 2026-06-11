# 教师端班级学习分析 Context Pack

## 当前任务边界

补齐 P1-5 教师端 class summary 后端 API：`GET /api/analytics/classes/{courseId}/summary`。

## 相关记忆和文档

- `AGENTS.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/DATABASE_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/specs/SPEC-20260605-analytics-summary.md`
- `docs/subagents/runs/RUN-20260606-teacher-class-analytics-summary.md`

## 选用技能

- `feature-development-workflow`
- `ai-learning-agent-development`
- `spring-ai-agent-backend`
- `test-driven-development`
- `verification-before-completion`

## Subagent 计划

不再新增 subagent。此前三个只读分析 agent 已完成并关闭，本切片由主 Codex 串行实现。

## 允许修改文件

- `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java`
- `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java`
- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`
- `docs/product/PRD-20260606-teacher-class-analytics-summary.md`
- `docs/requirements/REQ-20260606-teacher-class-analytics-summary.md`
- `docs/specs/SPEC-20260606-teacher-class-analytics-summary.md`
- `docs/plans/PLAN-20260606-teacher-class-analytics-summary.md`
- `docs/tasks/TASK-20260606-teacher-class-analytics-summary.md`
- `docs/context/CONTEXT-20260606-teacher-class-analytics-summary.md`
- `docs/evidence/EVIDENCE-20260606-teacher-class-analytics-summary.md`
- `docs/acceptance/ACCEPT-20260606-teacher-class-analytics-summary.md`
- `docs/retrospectives/RETRO-20260606-teacher-class-analytics-summary.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 禁止修改

- 不修改数据库 migration。
- 不修改前端。
- 不修改 Agent/RAG 执行服务。
- 不新增依赖。

## 测试命令

```powershell
cd backend
mvn "-Dtest=AnalyticsControllerTest" test
```

必要时补充：

```powershell
cd backend
mvn "-Dtest=AnalyticsControllerTest,ResourceReviewControllerTest,ReviewGovernanceServiceTest" test
```
