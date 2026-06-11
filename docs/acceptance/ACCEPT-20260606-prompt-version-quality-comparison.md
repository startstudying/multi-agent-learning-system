# Prompt Version 质量对比验收

## 验收项

- [x] 支持记录 evaluation run。
- [x] 支持记录 run 级质量指标。
- [x] 支持按 `evaluationSetId + promptCode + promptVersion` 联合对比质量指标。
- [x] 对比结果包含 baseline、每个版本指标、delta、winner。
- [x] comparison 只使用 `SUCCEEDED` run。
- [x] 指标聚合按 `metric.sampleCount` 加权平均，避免小样本 run 被错误放大。
- [x] `metric.sampleCount <= 0` 被拒绝。
- [x] `SUCCEEDED` run 的 `sampleCount <= 0` 被拒绝。
- [x] `FAILED` run 允许 `sampleCount = 0` 且不要求 metrics。
- [x] 同一 run 内重复 `metricName` 被拒绝，数据层也有唯一约束兜底。
- [x] 少于两个可对比 prompt version 时返回 `VALIDATION_ERROR`。
- [x] 学生不能访问 comparison。
- [x] 非创建者、非课程教师不能访问他人课程 evaluation set。
- [x] promptCode 与 evaluation set 不匹配时拒绝。
- [x] 响应不返回 `promptText`、`answerText`、`inputJson`、`rawOutput`。
- [x] V14 migration 覆盖 `evaluation_run`、`evaluation_run_metric`、状态约束、样本数约束和 `(run_id, metric_name)` 唯一约束。

## 非本轮范围

- 真实模型 A/B 执行器。
- 逐样本回放明细。
- 前端可视化 comparison 页面。
