# 学习路径节点推荐元数据 Context Pack

## 当前任务边界

补齐 P1-2 中“路径节点增加推荐原因、预估时长、资源类型、测评绑定关系”的后端最小切片。

## 相关文档

- `AGENTS.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/specs/SPEC-20260606-knowledge-mastery-threshold-remediation.md`

## 允许修改文件

- `backend/src/main/java/com/learningos/learning/dto/LearningPathNodeResponse.java`
- `backend/src/main/java/com/learningos/learning/domain/LearningPathNode.java`
- `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java`
- `backend/src/main/resources/db/migration/V9__learning_path_node_recommendation_metadata.sql`
- `backend/src/test/java/com/learningos/learning/api/LearningWorkflowControllerTest.java`
- `backend/src/test/java/com/learningos/learning/application/LearningWorkflowServiceTest.java`
- `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java`
- `docs/product/PRD-20260606-learning-path-node-recommendation-metadata.md`
- `docs/requirements/REQ-20260606-learning-path-node-recommendation-metadata.md`
- `docs/specs/SPEC-20260606-learning-path-node-recommendation-metadata.md`
- `docs/plans/PLAN-20260606-learning-path-node-recommendation-metadata.md`
- `docs/tasks/TASK-20260606-learning-path-node-recommendation-metadata.md`
- `docs/context/CONTEXT-20260606-learning-path-node-recommendation-metadata.md`
- `docs/evidence/EVIDENCE-20260606-learning-path-node-recommendation-metadata.md`
- `docs/acceptance/ACCEPT-20260606-learning-path-node-recommendation-metadata.md`
- `docs/retrospectives/RETRO-20260606-learning-path-node-recommendation-metadata.md`
- `docs/subagents/runs/RUN-20260606-learning-path-node-recommendation-metadata.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 禁止修改

- 不改前端。
- 不新增依赖。
- 不新增完整 Question / Rubric 实体。
- 不改变资源生成、RAG、Assessment 主流程。

## 测试命令

```powershell
cd backend
mvn "-Dtest=LearningWorkflowServiceTest,LearningWorkflowControllerTest,SchemaConvergenceMigrationTest" test
```

必要回归：

```powershell
cd backend
mvn "-Dtest=CourseKnowledgeControllerTest,LearningWorkflowServiceTest,LearningWorkflowControllerTest,SchemaConvergenceMigrationTest" test
```
