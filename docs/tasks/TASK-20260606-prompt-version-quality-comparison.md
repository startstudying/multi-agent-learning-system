# Prompt Version 质量对比任务

## Checklist

- [x] 新增 RED 测试：controller comparison、权限、缺失 set、run 不足、V14 migration。
- [x] 新增 code review RED 测试：指标聚合必须按 `metric.sampleCount` 加权平均。
- [x] 新增 code review RED 测试：`metric.sampleCount <= 0` 必须被拒绝。
- [x] 新增 code review 测试：`SUCCEEDED` run 必须有正样本数，`FAILED` run 可记录零样本失败。
- [x] 新增 code review 测试：同一 run 内重复 `metricName` 必须被拒绝。
- [x] 新增 `V14__evaluation_run_quality_metrics.sql`。
- [x] 新增 `EvaluationRun` 和 `EvaluationRunMetric`。
- [x] 新增 repositories。
- [x] 新增 run record / comparison DTO。
- [x] 新增 `EvaluationRunService`。
- [x] 新增 `EvaluationRunController`。
- [x] 更新 `SchemaConvergenceMigrationTest`。
- [x] 修复 comparison 聚合为样本数加权平均。
- [x] 加固 V14：`SUCCEEDED` 样本数约束、metric 样本数约束、`(run_id, metric_name)` 唯一约束。
- [x] 更新 `docs/planning/backend-architecture-todolist.md` P2-1 行。
- [x] 更新 Evidence / Acceptance。
- [x] 更新 Changelog / Memory。

## Done Criteria

- `POST /api/evaluation-runs` 可以记录白名单质量指标。
- `GET /api/evaluation-runs/comparison` 可以对比至少两个 prompt version。
- 同一质量指标跨多次 run 聚合时按 `metric.sampleCount` 加权平均。
- `metric.sampleCount <= 0` 返回 `VALIDATION_ERROR`。
- `SUCCEEDED` run 的 `sampleCount <= 0` 返回 `VALIDATION_ERROR`。
- 同一 run 内重复 `metricName` 返回 `VALIDATION_ERROR`。
- 学生访问 comparison 返回 `FORBIDDEN`。
- 不存在的 evaluation set 返回 `NOT_FOUND`。
- 少于两个已完成版本返回 `VALIDATION_ERROR`。
- V14 migration 文本测试覆盖表、核心列、索引和约束。
