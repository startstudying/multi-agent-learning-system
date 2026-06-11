# Knowledge DAG 依赖类型与路径规划需求

## 功能需求

1. `POST /api/knowledge-dependencies` 的 `dependencyType` 只允许：
   - `PREREQUISITE`
   - `RELATED`
   - `ADVANCED`
2. 当 `dependencyType` 为空、超长或不属于上述枚举时，返回 `VALIDATION_ERROR`。
3. 创建成功后，响应中的 `dependencyType` 必须与规范化后的枚举值一致。
4. `GET /api/courses/{courseId}/knowledge-graph` 必须继续返回全部依赖关系，包括 `PREREQUISITE`、`RELATED`、`ADVANCED`。
5. 学习路径规划中，只有 `PREREQUISITE` 依赖参与：
   - 前置依赖集合构建。
   - 拓扑排序。
   - 节点 `LOCKED` 状态判断。
   - `reasonSummary` 中的前置依赖说明。
6. `RELATED` 和 `ADVANCED` 依赖不得导致节点被锁定，也不得强制节点排在另一节点之后。
7. 已有 `PREREQUISITE` 行为必须保持：未掌握前置知识时，下游节点仍为 `LOCKED`。

## 数据边界

- 不修改 `knowledge_dependency` 表结构。
- `dependency_type` 仍使用现有 `varchar(40)` 字段。
- 不新增索引。
- 不新增迁移脚本。

## 权限与安全需求

- 本切片不新增权限模型。
- 错误响应不得暴露数据库异常或内部实现细节。
- 服务层负责业务校验，Controller 保持薄路由。

## 验收需求

- 非法 `dependencyType` 返回 400 与 `VALIDATION_ERROR`。
- `RELATED` 依赖创建成功，但学习路径中不作为前置依赖。
- `ADVANCED` 依赖创建成功，但学习路径中不作为前置依赖。
- `PREREQUISITE` 依赖仍会影响排序和锁定状态。
- 相关测试通过。
