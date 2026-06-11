# Knowledge DAG 掌握度阈值补救优先需求

## 功能需求

1. 课程 DAG 路径规划读取学习者最新 `mastery_record`。
2. 系统定义补救阈值 `REMEDIATION_THRESHOLD = 0.6`。
3. 当某个知识点满足以下条件时，视为补救优先候选：
   - 当前掌握度 `< 0.6`。
   - 该知识点是至少一个其他知识点的 `PREREQUISITE`。
   - 该知识点自身当前可学习，即没有未满足前置依赖。
4. 在同一批可学习节点中，补救优先候选排在普通可学习节点之前。
5. 多个补救优先候选同时存在时，掌握度更低者优先。
6. 非前置依赖类型 `RELATED` / `ADVANCED` 不参与补救优先判断。
7. 孤立低掌握度节点如果没有下游依赖，不应仅因掌握度低被提前。
8. `reasonSummary` 应说明该节点因低于补救阈值且会解锁下游知识而被优先。

## 数据边界

- 使用现有 `mastery_record` 表。
- 使用现有 `knowledge_dependency` 表。
- 使用现有 `LearningPathNodeResponse.reasonSummary`。
- 不新增字段、不新增迁移。

## 验收需求

- 低掌握度且阻塞下游的前置知识点排在普通可学习节点前。
- 下游节点在前置掌握度不足时仍保持 `LOCKED`。
- 普通低掌握度孤立节点不因低掌握度自动抢占优先级。
- 已有 `PREREQUISITE`、`RELATED`、`ADVANCED` 行为测试继续通过。
