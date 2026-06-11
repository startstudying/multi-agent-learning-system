# 学习路径节点推荐元数据证据

## 1. 追踪

- PRD：`docs/product/PRD-20260606-learning-path-node-recommendation-metadata.md`
- REQ：`docs/requirements/REQ-20260606-learning-path-node-recommendation-metadata.md`
- SPEC：`docs/specs/SPEC-20260606-learning-path-node-recommendation-metadata.md`
- TASK：`docs/tasks/TASK-20260606-learning-path-node-recommendation-metadata.md`
- 日期：2026-06-06

## 2. 实现内容

- `LearningPathNodeResponse` 增加 `recommendationReason`、`estimatedDurationMinutes`、`resourceType`、`assessmentBindingRelation`。
- `LearningPathNode` 增加对应持久化字段。
- `LearningWorkflowService` 在模板路径和课程 DAG 路径中生成节点推荐元数据。
- `GET /api/learning-paths/{pathId}` 从持久化节点恢复新增字段，历史空值使用保守默认值。
- 新增 `V9__learning_path_node_recommendation_metadata.sql`。

## 3. TDD 证据

### RED

命令：

```powershell
cd backend
mvn "-Dtest=LearningWorkflowServiceTest,LearningWorkflowControllerTest,SchemaConvergenceMigrationTest" test
```

失败摘要：

```text
No value at JSON path "$.data.nodes[0].recommendationReason"
LearningPathNodeResponse record components did not contain recommendationReason, estimatedDurationMinutes, resourceType, assessmentBindingRelation
V9__learning_path_node_recommendation_metadata.sql resource was missing
```

### GREEN

命令：

```powershell
cd backend
mvn "-Dtest=LearningWorkflowServiceTest,LearningWorkflowControllerTest,SchemaConvergenceMigrationTest" test
```

结果：

```text
Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 相关回归

命令：

```powershell
cd backend
mvn "-Dtest=CourseKnowledgeControllerTest,LearningWorkflowServiceTest,LearningWorkflowControllerTest,SchemaConvergenceMigrationTest" test
```

结果：

```text
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 4. 架构检查

- [x] Controller 保持薄层。
- [x] 未新增依赖。
- [x] 数据库变更有 V9 migration。
- [x] API 合同已在 SPEC 中记录。
- [x] 未引入完整 Question 实体或题库管理范围。

## 5. 已知限制

- `assessmentBindingRelation` 当前是稳定约定字符串，不是真实外键。
- `resourceType` 当前由状态和掌握度推导，不是真实资源推荐排序结果。
