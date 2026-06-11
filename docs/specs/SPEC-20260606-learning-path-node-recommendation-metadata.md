# 学习路径节点推荐元数据规格

## API 合同

`LearningPathNodeResponse` 扩展为：

```json
{
  "nodeId": "kp_entity_mapping",
  "title": "Entity Mapping",
  "status": "ACTIVE",
  "mastery": 0.55,
  "reasonSummary": "Selected because prerequisites are satisfied in the Knowledge DAG.",
  "recommendationReason": "Start with a guided lesson because prerequisites are satisfied and mastery is still below completion threshold.",
  "estimatedDurationMinutes": 30,
  "resourceType": "LECTURE",
  "assessmentBindingRelation": "FORMATIVE_CHECK:question_kp_entity_mapping"
}
```

## 生成规则

- `DONE` 节点：
  - `resourceType = REVIEW`
  - `estimatedDurationMinutes = 10`
  - `assessmentBindingRelation = MASTERY_CHECK:question_{knowledgePointId}`
- `ACTIVE` 且 `mastery < 0.6`：
  - `resourceType = REMEDIATION`
  - `estimatedDurationMinutes = 45`
  - `assessmentBindingRelation = REMEDIATION_CHECK:question_{knowledgePointId}`
- 普通 `ACTIVE`：
  - `resourceType = LECTURE`
  - `estimatedDurationMinutes = 30`
  - `assessmentBindingRelation = FORMATIVE_CHECK:question_{knowledgePointId}`
- `LOCKED`：
  - `resourceType = PREREQUISITE_REVIEW`
  - `estimatedDurationMinutes = 20`
  - `assessmentBindingRelation = LOCKED_UNTIL_PREREQUISITE_MASTERY:question_{knowledgePointId}`

模板路径沿用相同策略，使用模板节点状态和掌握度计算。

## 持久化

新增 V9 迁移，向 `learning_path_node` 增加四个可空字段。实体 `LearningPathNode` 增加对应属性。新生成路径必须写入这些字段；历史路径查询如果字段为空，应返回保守默认值，避免 API 输出缺字段。

## 架构约束

- Controller 不增加业务逻辑。
- 规划逻辑继续放在 `LearningWorkflowService`。
- 不新增依赖。
- 不改变 Assessment 掌握度更新算法。
