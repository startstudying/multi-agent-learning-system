# Prompt Version 质量对比证据

## RED

命令：

```powershell
cd backend; mvn "-Dtest=EvaluationRunServiceTest,EvaluationRunControllerTest,SchemaConvergenceMigrationTest" test
```

结果：

- 失败，符合预期。
- 失败原因：`EvaluationRunService`、`EvaluationMetricRequest`、`EvaluationRunRecordRequest` 等类型尚不存在。

## GREEN

命令：

```powershell
cd backend; mvn "-Dtest=EvaluationRunServiceTest,EvaluationRunControllerTest,SchemaConvergenceMigrationTest" test
```

结果：

- Tests run: 19
- Failures: 0
- Errors: 0
- BUILD SUCCESS

## Code Review RED

命令：

```powershell
cd backend; mvn "-Dtest=EvaluationRunServiceTest#weightsMetricAveragesByMetricSampleCount" test
```

结果：

- 失败，符合预期。
- 失败原因：同一 `metricName` 聚合使用简单平均，`expected: 0.18`，`actual: 0.5`。

命令：

```powershell
cd backend; mvn "-Dtest=EvaluationRunServiceTest#rejectsMetricWithZeroSampleCount" test
```

结果：

- 失败，符合预期。
- 失败原因：服务层接受了 `metric.sampleCount = 0`，断言信息为 `Expecting code to raise a throwable`。

## Code Review GREEN

命令：

```powershell
cd backend; mvn "-Dtest=EvaluationRunServiceTest#weightsMetricAveragesByMetricSampleCount+rejectsMetricWithZeroSampleCount" test
```

结果：

- Tests run: 2
- Failures: 0
- Errors: 0
- BUILD SUCCESS

命令：

```powershell
cd backend; mvn "-Dtest=EvaluationRunServiceTest#rejectsSucceededRunWithZeroSampleCount+allowsFailedRunWithZeroSampleCount+rejectsDuplicateMetricNamesInSingleRun" test
```

结果：

- Tests run: 3
- Failures: 0
- Errors: 0
- BUILD SUCCESS

## 回归验证

命令：

```powershell
cd backend; mvn "-Dtest=EvaluationRunServiceTest,EvaluationRunControllerTest,SchemaConvergenceMigrationTest" test
```

结果：

- Tests run: 26
- Failures: 0
- Errors: 0
- BUILD SUCCESS

命令：

```powershell
cd backend; mvn "-Dtest=EvaluationRunServiceTest,EvaluationRunControllerTest,EvaluationSetServiceTest,EvaluationSetControllerTest,PromptVersionServiceTest,PromptVersionControllerTest,RagEvaluationServiceTest,RagEvaluationControllerTest,GradingEvaluationServiceTest,AssessmentControllerTest#exposesAutomatedGradingQualityEvaluationReport,SchemaConvergenceMigrationTest" test
```

结果：

- Tests run: 49
- Failures: 0
- Errors: 0
- BUILD SUCCESS

## 覆盖点

- `POST /api/evaluation-runs` 可记录 run 和白名单质量指标。
- `GET /api/evaluation-runs/comparison` 可返回 baseline、rows、delta、winner。
- comparison 只使用 `SUCCEEDED` run。
- 同一指标跨多次 run 聚合时按 metric sample count 加权平均。
- `metric.sampleCount <= 0` 会返回 `VALIDATION_ERROR`。
- `SUCCEEDED` run 的 `sampleCount <= 0` 会返回 `VALIDATION_ERROR`，`FAILED` run 允许 `sampleCount = 0` 且不要求 metrics。
- 同一 run 内重复 `metricName` 会返回 `VALIDATION_ERROR`，同时 V14 使用 `uk_evaluation_run_metric_run_name(run_id, metric_name)` 做数据层兜底。
- 学生访问 comparison 返回 `FORBIDDEN`。
- 缺失 evaluation set 返回 `NOT_FOUND`。
- run 不足两个 prompt version 返回 `VALIDATION_ERROR`。
- promptCode 与 evaluation set 不匹配返回 `VALIDATION_ERROR`。
- 非白名单指标被拒绝。
- V14 migration 文本包含 run/metric 表、核心列、索引、外键、状态约束、`SUCCEEDED` 样本数约束和 metric 样本数约束。

## 仍需说明

- 未运行完整 `mvn test`。
- 本轮不做真实模型 A/B 执行器，只记录并对比已产生的评测指标。
