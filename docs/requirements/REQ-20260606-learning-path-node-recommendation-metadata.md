# 学习路径节点推荐元数据需求

## 功能需求

1. 路径节点响应必须包含 `recommendationReason`，用于解释推荐该节点的学习动作。
2. 路径节点响应必须包含 `estimatedDurationMinutes`，类型为整数，值应大于 0。
3. 路径节点响应必须包含 `resourceType`，用于描述建议资源类型。
4. 路径节点响应必须包含 `assessmentBindingRelation`，用于描述节点与测评的绑定方式。
5. 模板路径和课程 Knowledge DAG 路径都必须生成上述字段。
6. 创建路径时写入 `learning_path_node`，查询路径时从持久化字段恢复。
7. 新字段允许为空以兼容历史数据，但新生成路径必须填充。

## 数据需求

- `learning_path_node.recommendation_reason varchar(2000)`
- `learning_path_node.estimated_duration_minutes integer`
- `learning_path_node.resource_type varchar(80)`
- `learning_path_node.assessment_binding_relation varchar(255)`

## 验收需求

- `POST /api/learning-paths` 的节点 JSON 包含四个新增字段。
- `GET /api/learning-paths/{pathId}` 的节点 JSON 与创建响应保持一致。
- Flyway 迁移文件包含上述四个字段。
- 相关学习路径测试通过。
