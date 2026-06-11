# 自动批改质量评估需求

## 功能需求

1. `POST /api/assessment/grading-evaluations` 支持 `samples` 字段。
2. 每个样本支持以下字段：
   - `sampleId`
   - `questionType`
   - `knowledgePointId`
   - `rubricVersion`
   - `humanScore`
   - `systemScore`
   - `humanGrade`
   - `systemGrade`
   - `humanWrongCause`
   - `systemWrongCause`
3. 总体报告必须返回：
   - `sampleCount`
   - `meanAbsoluteError`
   - `gradeAgreementRate`
   - `wrongCauseAgreementRate`
   - `agreementRate`
4. `agreementRate` 保留为旧字段，语义与 `gradeAgreementRate` 对齐，避免破坏现有调用方。
5. 分组分析必须按以下维度返回：
   - `questionType`
   - `knowledgePointId`
   - `rubricVersion`
6. 每个分组项必须返回同一套指标：
   - `groupKey`
   - `sampleCount`
   - `meanAbsoluteError`
   - `gradeAgreementRate`
   - `wrongCauseAgreementRate`
7. 当 `samples` 为空且旧字段存在时，服务必须兼容旧版分数数组请求。
8. 空样本返回 0 指标和空分组。

## 校验需求

1. `humanScore`、`systemScore` 不得为 `null`、`NaN` 或无穷大。
2. 新版 `samples` 模式下，`humanGrade` 与 `systemGrade` 均为空的样本不参与等级一致率分母。
3. 新版 `samples` 模式下，`humanWrongCause` 与 `systemWrongCause` 均为空的样本不参与错因一致率分母。
4. 旧版分数数组模式下，`agreementThreshold` 默认为 `0.05`，且不得小于 0。

## 权限与安全需求

1. 本切片沿用当前 endpoint 的临时访问策略，不新增权限模型。
2. 报告不返回学生原始答案或批改长文本。
3. 不存储样本，不写数据库。

## 测试需求

1. Service test 覆盖 MAE、等级一致率、错因一致率、三类分组维度。
2. Controller test 覆盖响应包含总体指标和 `groupedAnalysis`。
3. 至少运行：

```powershell
cd backend; mvn "-Dtest=GradingEvaluationServiceTest,AssessmentControllerTest#exposesAutomatedGradingQualityEvaluationReport" test
```
