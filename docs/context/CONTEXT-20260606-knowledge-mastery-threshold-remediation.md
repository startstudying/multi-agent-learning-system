# Knowledge DAG 掌握度阈值补救优先 Context Pack

## 当前任务边界

补齐 P1-2 中“增加掌握度阈值规则，例如低于 0.6 时优先补救前置知识”的最小后端切片。

## 相关记忆和文档

- `AGENTS.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/specs/SPEC-20260606-knowledge-dependency-types-path-planning.md`

## 选用技能

- `feature-development-workflow`
- `ai-learning-agent-development`
- `multi-agent-coder`
- `learning-path-planner`
- `test-driven-development`
- `confidence-check`

## Subagent 计划

已启动两个只读分析子代理：

- `Noether`：核查路径规划算法边界。
- `Planck`：设计 RED 测试场景。

实现由主 Codex 串行完成。

## 允许修改文件

- `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java`
- `backend/src/test/java/com/learningos/learning/api/LearningWorkflowControllerTest.java`
- `docs/product/PRD-20260606-knowledge-mastery-threshold-remediation.md`
- `docs/requirements/REQ-20260606-knowledge-mastery-threshold-remediation.md`
- `docs/specs/SPEC-20260606-knowledge-mastery-threshold-remediation.md`
- `docs/plans/PLAN-20260606-knowledge-mastery-threshold-remediation.md`
- `docs/tasks/TASK-20260606-knowledge-mastery-threshold-remediation.md`
- `docs/context/CONTEXT-20260606-knowledge-mastery-threshold-remediation.md`
- `docs/evidence/EVIDENCE-20260606-knowledge-mastery-threshold-remediation.md`
- `docs/acceptance/ACCEPT-20260606-knowledge-mastery-threshold-remediation.md`
- `docs/retrospectives/RETRO-20260606-knowledge-mastery-threshold-remediation.md`
- `docs/subagents/runs/RUN-20260606-knowledge-mastery-threshold-remediation.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 禁止修改

- 不修改数据库 migration。
- 不修改前端。
- 不新增依赖。
- 不修改 Assessment 掌握度更新算法。
- 不扩展路径节点字段。

## 测试命令

```powershell
cd backend
mvn "-Dtest=LearningWorkflowControllerTest" test
```

必要时补充：

```powershell
cd backend
mvn "-Dtest=CourseKnowledgeControllerTest,LearningWorkflowControllerTest" test
```
