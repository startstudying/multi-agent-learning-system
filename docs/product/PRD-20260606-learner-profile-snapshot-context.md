# Learner Profile 维度与快照 PRD

## 背景

当前画像已经支持多来源合并更新，但 P1-1 仍缺三件事：画像维度命名不够贴合产品语言，每个维度缺少 `lastEvidenceId`，路径规划和资源生成没有保存当时引用的画像快照。

没有快照时，后续无法解释“为什么当时给学生规划这条路径 / 生成这类资源”，也不利于论文中的个性化证据链。

## 目标

- 明确画像维度：`baseline_level`、`learning_goal`、`weak_point`、`preference`、`pace_and_feedback`、`recent_error_pattern`、`teacher_note`。
- 每个 `ProfileDimension` 包含 `confidence`、`sourceType`、`lastEvidenceId`。
- `learning_path` 保存并返回 `profileSnapshot`。
- `resource_generation_task` 保存并返回 `profileSnapshot`。
- 不引入真实模型、不新增独立画像维度表，先基于现有 JSON 存储闭环。

## 非目标

- 不做前端页面。
- 不做完整画像编辑工作台。
- 不新增 Agent Trace 流程。
- 不改变现有画像合并策略的大方向。

## 用户价值

- 学生路径和资源推荐可以解释引用了哪些画像字段。
- 教师或答辩时可以看到个性化决策证据。
- 后续可以基于快照做资源生成质量评估和路径重规划审计。
