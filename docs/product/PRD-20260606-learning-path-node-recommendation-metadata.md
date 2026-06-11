# 学习路径节点推荐元数据 PRD

## 背景

当前学习路径节点只返回 `nodeId`、`title`、`status`、`mastery` 和 `reasonSummary`。这能说明节点为什么出现，但还不能支撑学生端展示“下一步怎么学”、教师端解释“推荐什么资源”、以及论文中“路径规划与测评闭环”的证据链。

P1-2 TODO 中仍有一项未完成：路径节点增加推荐原因、预估时长、资源类型、测评绑定关系。本切片补齐这部分后端能力。

## 目标

- `POST /api/learning-paths` 返回的每个节点补充：
  - `recommendationReason`
  - `estimatedDurationMinutes`
  - `resourceType`
  - `assessmentBindingRelation`
- `GET /api/learning-paths/{pathId}` 返回同样的节点元数据。
- 新增字段持久化到 `learning_path_node`，避免创建后查询丢失。
- 不引入新依赖，不接入真实模型，不新增完整 Question 实体。

## 非目标

- 不实现前端页面。
- 不实现资源真实推荐排序。
- 不实现题库管理或测评题生成。
- 不改变已有 `DONE` / `ACTIVE` / `LOCKED` 状态语义。

## 用户价值

- 学生可以看到每个路径节点建议学习哪类资源、预计投入多久、对应哪类测评。
- 教师可以解释路径节点推荐依据。
- 系统能在演示和论文中证明学习路径不是简单列表，而是带有资源和测评闭环的规划结果。
