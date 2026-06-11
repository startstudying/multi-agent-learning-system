# Knowledge DAG 掌握度阈值补救优先任务拆解

## Task 1 文档与子代理

- [x] 读取项目记忆和后端 TODO。
- [x] 启动算法边界核查子代理。
- [x] 启动 RED 测试设计子代理。
- [x] 创建 PRD / REQ / SPEC / PLAN / TASK / CONTEXT。
- [x] 创建 subagent run 报告。

## Task 2 TDD 测试

- [x] 在 `LearningWorkflowControllerTest` 添加低掌握度前置补救优先 RED 测试。
- [x] 运行 `LearningWorkflowControllerTest` 并确认 RED。

## Task 3 后端实现

- [x] 在 `LearningWorkflowService` 增加补救阈值常量。
- [x] 构建 `dependentsByPoint`。
- [x] 在拓扑排序的每批可学习节点中优先低掌握度前置知识。
- [x] 在 `reasonSummary` 中说明补救优先原因。

## Task 4 验证与交付

- [x] 运行相关测试并通过。
- [x] 创建 evidence 文档。
- [x] 创建 acceptance 文档。
- [x] 更新 changelog。
- [x] 更新 memory。
- [x] 更新 backend TODO。
- [x] 创建 retrospective。

## Done Criteria

- 低掌握度且阻塞下游的前置知识点优先于普通可学习节点。
- `reasonSummary` 说明低于 `0.6` 补救阈值。
- `PREREQUISITE` 锁定规则保留。
- `RELATED` / `ADVANCED` 不参与补救优先判断。
- 无数据库迁移、无新依赖、无前端变更。
