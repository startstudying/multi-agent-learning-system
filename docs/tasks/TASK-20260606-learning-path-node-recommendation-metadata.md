# 学习路径节点推荐元数据任务拆解

## Task 1 文档和并行分析

- [x] 读取项目规则、记忆、TODO 和现有路径规划代码。
- [x] 启动只读子 Agent 做测试设计。
- [x] 关闭超时子 Agent，避免后台长时间运行。
- [x] 创建 PRD / REQ / SPEC / PLAN / TASK / CONTEXT / subagent run。

## Task 2 TDD RED

- [x] 在 `LearningWorkflowControllerTest` 增加节点元数据 JSON 断言。
- [x] 在 `LearningWorkflowServiceTest` 增加 DTO accessor 断言。
- [x] 运行最小测试并确认失败来自缺少新增字段。

## Task 3 后端实现

- [x] 扩展 `LearningPathNodeResponse`。
- [x] 扩展 `LearningPathNode` 实体。
- [x] 扩展 `LearningWorkflowService` 的 DAG、模板、持久化和查询恢复逻辑。
- [x] 新增 `V9__learning_path_node_recommendation_metadata.sql`。
- [x] 更新 `SchemaConvergenceMigrationTest`。

## Task 4 验证和交付

- [x] 运行 `LearningWorkflowServiceTest` 和 `LearningWorkflowControllerTest`。
- [x] 运行迁移文本测试。
- [x] 更新 evidence / acceptance。
- [x] 更新 changelog / memory / API memory / backend TODO。
- [x] 创建 retrospective。

## Done Criteria

- 创建路径和查询路径都返回新增四个字段。
- 新生成节点的 `estimatedDurationMinutes > 0`。
- 新生成节点的 `recommendationReason`、`resourceType`、`assessmentBindingRelation` 非空。
- 新字段通过 V9 migration 持久化。
- 相关测试通过。
