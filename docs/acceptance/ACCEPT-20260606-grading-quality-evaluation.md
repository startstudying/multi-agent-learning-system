# 自动批改质量评估验收

## 验收项

- [x] 支持结构化人工评分样本。
- [x] 支持平均绝对误差 `meanAbsoluteError`。
- [x] 支持等级一致率 `gradeAgreementRate`。
- [x] 支持错因分类一致率 `wrongCauseAgreementRate`。
- [x] 支持按 `questionType` 分组。
- [x] 支持按 `knowledgePointId` 分组。
- [x] 支持按 `rubricVersion` 分组。
- [x] 保持旧版 `humanScores / aiScores / agreementThreshold` 兼容。

## 非本轮范围

- 新增批改评估数据库表。
- 真实人工标注工作流。
- 按班级或课程自动周期评估。
