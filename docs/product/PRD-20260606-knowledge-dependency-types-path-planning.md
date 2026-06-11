# Knowledge DAG 依赖类型与路径规划 PRD

## 背景

当前后端已经具备 `knowledge_dependency` 基础表和学习路径 DAG 排序能力，但 `dependencyType` 仍是任意字符串，路径规划也会把所有依赖关系都当作前置依赖处理。这会导致 `RELATED`、`ADVANCED` 这类非前置关系错误地锁定学习路径节点，影响自适应学习路径的准确性。

## 目标

- 将知识依赖类型收敛为 `PREREQUISITE`、`RELATED`、`ADVANCED` 三类。
- 创建知识依赖时拒绝不支持的 `dependencyType`。
- 学习路径规划只把 `PREREQUISITE` 作为拓扑排序和锁定节点的前置边。
- 保留 `RELATED` 和 `ADVANCED` 作为知识图谱关系，供后续推荐、扩展学习和图谱展示使用。
- 不新增数据库迁移，不引入新依赖，不改变现有 API envelope。

## 非目标

- 不实现掌握度阈值补救优先规则。
- 不扩展 `learning_path_node` 的推荐原因、预计时长、资源类型或测评绑定字段。
- 不新增前端页面。
- 不新增真实图数据库或图算法框架。
- 不实现完整知识图谱可视化。

## MVP 定义

本次 MVP 只完成 P1-2 中“区分 `PREREQUISITE`、`RELATED`、`ADVANCED` 三类边，并在规划策略中使用”的最小后端切片：

- `POST /api/knowledge-dependencies` 只接受三种依赖类型。
- 知识图谱查询仍返回全部依赖类型。
- `POST /api/learning-paths` 生成课程 DAG 路径时，只有 `PREREQUISITE` 会影响节点顺序和 `LOCKED` 状态。

## 用户价值

- 教师可以区分真正的前置知识和普通相关知识，避免课程图谱语义混乱。
- 学生路径不会因为“相关知识”被错误锁住，学习路径更符合教学意图。
- 后续可以在不改表的基础上继续扩展知识推荐、进阶学习和补救学习策略。
