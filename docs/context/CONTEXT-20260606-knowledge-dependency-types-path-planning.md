# Knowledge DAG 依赖类型与路径规划 Context Pack

## 当前任务边界

补齐 P1-2 Knowledge DAG 最小切片：区分 `PREREQUISITE`、`RELATED`、`ADVANCED` 三类边，并让学习路径规划只使用 `PREREQUISITE` 作为前置依赖。

## 相关记忆和文档

- `AGENTS.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/subagents/SUBAGENT_REGISTRY.md`

## 选用技能

- `feature-development-workflow`
- `ai-learning-agent-development`
- `multi-agent-coder`
- `learning-path-planner`
- `test-driven-development`
- `confidence-check`

## Subagent 计划

已启动两个只读分析子代理：

- `Carver`：核查 `dependencyType` 创建、DTO、Service 和异常 envelope。
- `Ohm`：核查路径规划中依赖类型如何影响拓扑排序和节点锁定。

实现由主 Codex 串行完成，避免多个代理修改同一批 Java 文件。

## 允许修改文件

- `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java`
- `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java`
- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`
- `backend/src/test/java/com/learningos/learning/api/LearningWorkflowControllerTest.java`
- `docs/product/PRD-20260606-knowledge-dependency-types-path-planning.md`
- `docs/requirements/REQ-20260606-knowledge-dependency-types-path-planning.md`
- `docs/specs/SPEC-20260606-knowledge-dependency-types-path-planning.md`
- `docs/plans/PLAN-20260606-knowledge-dependency-types-path-planning.md`
- `docs/tasks/TASK-20260606-knowledge-dependency-types-path-planning.md`
- `docs/context/CONTEXT-20260606-knowledge-dependency-types-path-planning.md`
- `docs/evidence/EVIDENCE-20260606-knowledge-dependency-types-path-planning.md`
- `docs/acceptance/ACCEPT-20260606-knowledge-dependency-types-path-planning.md`
- `docs/retrospectives/RETRO-20260606-knowledge-dependency-types-path-planning.md`
- `docs/subagents/runs/RUN-20260606-knowledge-dependency-types-path-planning.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 禁止修改

- 不修改数据库 migration。
- 不修改前端。
- 不新增依赖。
- 不新增 Agent/RAG 执行链路。
- 不扩大到掌握度阈值、路径节点字段扩展或资源推荐。

## 测试命令

```powershell
cd backend
mvn "-Dtest=CourseKnowledgeControllerTest,LearningWorkflowControllerTest" test
```

必要时补充：

```powershell
cd backend
mvn test
```
