# Learner Profile 维度与快照 Subagent Run

## 任务

为 P1-1 Learner Profile 更新策略做只读并行分析。

## 子 Agent

| Agent | 任务 | 状态 | 结果 |
|---|---|---|---|
| Linnaeus | profile/schema/API 影响分析 | completed | 建议不新增独立 profile dimension 表，本切片在 `learning_path` 和 `resource_generation_task` 保存 `profile_snapshot` |
| Hooke | TDD RED 测试设计 | completed | 建议覆盖七维画像、`lastEvidenceId`、path/resource 创建/查询/replay 快照和 V10 migration |

## 主线程初步结论

- `ProfileDimension` 已有 `confidence`、`evidence`、`sourceType`，缺 `lastEvidenceId`。
- `ProfileStructuredFields` 已有基础字段，但缺 `recent_error_pattern` 和 `teacher_note`。
- `LearningPath` 和 `ResourceGenerationTask` 均缺 `profileSnapshot`。
- 最小实现采用 JSON 字符串快照，不新增独立维度表。

## 风险

- 当前快照是 JSON 字符串，后续做分析查询可能需要拆表或 JSON 查询索引。
- 没有画像时需要稳定空快照，避免路径和资源生成失败。

## 集成结果

- 主线程采用 JSON 字符串快照，未新增独立画像维度表。
- `ProfileDimension` 增加 `lastEvidenceId`。
- `LearningPath` 与 `ResourceGenerationTask` 均持久化 `profileSnapshot`。
- 快照兼容历史字段别名，并合并 `weakPointsJson` / `preferencesJson`。
