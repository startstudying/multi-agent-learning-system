# 自动批改质量评估证据

## RED

命令：

```powershell
cd backend; mvn "-Dtest=GradingEvaluationServiceTest,AssessmentControllerTest#exposesAutomatedGradingQualityEvaluationReport" test
```

结果：

- 失败，符合预期。
- 失败原因：现有批改评估仅支持旧版 `humanScores / aiScores`，缺少样本结构、等级一致率、错因一致率和分组分析。

## GREEN

命令：

```powershell
cd backend; mvn "-Dtest=GradingEvaluationServiceTest,AssessmentControllerTest#exposesAutomatedGradingQualityEvaluationReport" test
```

结果：

- Tests run: 4
- Failures: 0
- Errors: 0
- BUILD SUCCESS

## 覆盖点

- 支持人工评分样本结构。
- 支持 `meanAbsoluteError`、`gradeAgreementRate`、`wrongCauseAgreementRate`。
- `agreementRate` 保持旧响应兼容，并与新版等级一致率对齐。
- 支持按 `questionType`、`knowledgePointId`、`rubricVersion` 分组分析。

## 仍需说明

- 本轮为离线请求级计算，不新增数据库表。
- 人工评分样本集的持久化仍由 `evaluation_set/evaluation_sample` 管理。
