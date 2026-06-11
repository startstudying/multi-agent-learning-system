# Knowledge DAG 依赖类型与路径规划任务拆解

## Task 1 文档与子代理收口

- [x] 读取项目记忆和后端 TODO。
- [x] 启动 dependencyType 校验核查子代理。
- [x] 启动路径规划依赖排序核查子代理。
- [x] 创建 PRD / REQ / SPEC / PLAN / TASK / CONTEXT。
- [x] 创建 subagent run 报告。

## Task 2 TDD 测试

- [x] 在 `CourseKnowledgeControllerTest` 添加非法 `dependencyType` 返回 `VALIDATION_ERROR` 的 RED 测试。
- [x] 在 `LearningWorkflowControllerTest` 添加 `RELATED` / `ADVANCED` 不锁定路径节点的 RED 测试。
- [x] 运行 `CourseKnowledgeControllerTest,LearningWorkflowControllerTest` 并确认 RED。

## Task 3 后端实现

- [x] 在 `KnowledgeCatalogService` 添加 `dependencyType` 规范化与白名单校验。
- [x] 在 `LearningWorkflowService` 只让 `PREREQUISITE` 参与前置依赖集合。
- [x] 保持知识图谱查询返回全部依赖类型。

## Task 4 验证与交付

- [x] 运行相关测试并通过。
- [x] 创建 evidence 文档。
- [x] 创建 acceptance 文档。
- [x] 更新 changelog。
- [x] 更新 memory。
- [x] 更新 backend TODO。
- [x] 创建 retrospective。

## Done Criteria

- 非法 `dependencyType` 返回 400 与 `VALIDATION_ERROR`。
- 合法 `PREREQUISITE`、`RELATED`、`ADVANCED` 可以创建。
- `RELATED` / `ADVANCED` 不影响学习路径节点排序和锁定。
- `PREREQUISITE` 仍影响排序和锁定。
- 无数据库迁移、无新依赖、无前端变更。
