# Learner Profile 维度与快照需求

## 功能需求

1. `ProfileStructuredFields` 必须包含七个显式画像维度：
   - `baseline_level`
   - `learning_goal`
   - `weak_point`
   - `preference`
   - `pace_and_feedback`
   - `recent_error_pattern`
   - `teacher_note`
2. `ProfileDimension` 必须包含 `lastEvidenceId`。
3. 新抽取和合并后的画像维度必须保留 `confidence`、`sourceType`、`lastEvidenceId`。
4. 学习路径创建时必须保存当时 learner profile 快照，并在 `LearningPathResponse.profileSnapshot` 返回。
5. 资源生成任务创建时必须保存当时 learner profile 快照，并在 `ResourceGenerationResponse.profileSnapshot` 返回。
6. 新增字段允许为空以兼容历史数据，但新创建路径和资源任务应尽量填充。

## 数据需求

- `learning_path.profile_snapshot text`
- `resource_generation_task.profile_snapshot text`

## 验收需求

- profile extraction 响应和持久化 JSON 包含七个维度及 `lastEvidenceId`。
- path create/get 响应包含 `profileSnapshot`。
- resource generation create/get/replay 响应包含 `profileSnapshot`。
- V10 migration 包含两个 `profile_snapshot` 字段。
