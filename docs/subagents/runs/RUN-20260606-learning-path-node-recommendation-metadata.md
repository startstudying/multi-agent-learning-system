# 学习路径节点推荐元数据 Subagent Run

## 任务

为 P1-2 路径节点增强做并行只读分析，避免直接并行改同一批后端文件。

## 子 Agent

| Agent | 任务 | 状态 | 结果 |
|---|---|---|---|
| Rawls | DTO、实体、持久化和资源/测评关系影响分析 | timeout 后关闭 | 未采用输出；主线程基于源码完成影响分析 |
| Heisenberg | TDD RED 测试设计 | completed | 建议使用 `recommendationReason`、`estimatedDurationMinutes`、`resourceType`、`assessmentBindingRelation` 四个 JSON 字段，并覆盖 POST / GET 一致性 |

## 集成结论

- 实现采用主 Codex 串行提交，避免多个 Agent 修改同一文件。
- `estimatedDurationMinutes` 使用整数，便于测试和前端展示。
- 新字段进入 DTO 和 `learning_path_node` 持久化，避免查询路径丢失。
- 测评绑定先使用稳定标识 `...:question_{knowledgePointId}`，不引入完整题库实体。

## 风险

- 当前 `question` 表缺少 Java 实体，测评绑定关系只能先表达为约定字符串。
- 真实资源推荐和题目生成仍是后续任务，不在本切片完成。
