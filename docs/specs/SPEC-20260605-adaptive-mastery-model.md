# 自适应掌握度模型规格

## 影响范围

- `AssessmentService`
- `AssessmentFeedbackService`
- `AssessmentControllerTest`
- 相关交付文档、TODO、memory、changelog

## 知识点推导

第一版不新增题库 repository。`AssessmentFeedbackService.resolveKnowledgePointId(questionId)` 使用稳定规则：

- `q_sql_join` -> `kp_sql_join`
- `question_sql_join` -> `kp_sql_join`
- 其他非空值 -> `kp_` + 规范化后的 question id
- 空值兜底为 `kp_unknown`

后续接入 `question` 表实体后，应改为从题目记录读取 `knowledgePointId`。

## 掌握度读取

`AssessmentService.submitAnswer`：

1. 验证当前用户只能提交自己的答案。
2. 推导 `knowledgePointId`。
3. 查询 `MasteryRecordRepository.findFirstByLearnerIdAndKnowledgePointIdOrderByUpdatedAtDesc`。
4. 有记录时使用最新 `mastery` 作为 `beforeMastery`。
5. 无记录时使用 `AssessmentFeedbackService.defaultInitialMastery()`。
6. 调用 `AssessmentFeedbackService.evaluate(request, beforeMastery)`。

## BKT-lite 更新策略

系统按错因类别做确定性更新：

| Wrong Cause | 规则 |
|---|---|
| `TRANSFER_WEAKNESS` | `min(0.78, before + 0.16)` |
| `STEP_ERROR` | `min(0.72, before + 0.10)` |
| `INCOMPLETE_EXPRESSION` | `min(0.46, before + 0.04)` |
| `CONCEPT_ERROR` | `max(0.0, min(0.38, before - 0.04))` |

所有结果四舍五入到两位小数，并限制在 `[0.0, 1.0]`。

## API 兼容性

`POST /api/assessment/answers` 请求结构不变。响应字段不变，但 `masteryUpdates[].beforeMastery` 和 `afterMastery` 从固定演示值变为基于学习历史和错因的动态值。

## 验收标准

- 有历史 `mastery_record` 时，响应中的 `beforeMastery` 等于历史最新值。
- 动态 `afterMastery` 被写入新的 `mastery_record`。
- 路径节点和路径重规划使用动态掌握度。
- 无历史记录时仍能正常走默认初始掌握度。
