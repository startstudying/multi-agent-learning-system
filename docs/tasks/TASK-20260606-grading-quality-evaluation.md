# 自动批改质量评估任务

## Checklist

- [x] 阅读 AGENTS.md、PROJECT_MEMORY、BACKEND_MEMORY、backend TODO 和 assessment 相关代码。
- [x] 完成 Skill Selection Gate、Subagent Decision 和 Confidence Check。
- [x] 创建 PRD / REQ / SPEC / PLAN / TASK / CONTEXT / RUN 文档。
- [ ] RED：新增 service/controller 测试并确认失败。
- [ ] GREEN：实现 `samples` 评估、总体指标和分组分析。
- [ ] 验证：运行指定 Maven 测试。
- [ ] 写入 Evidence / Acceptance。

## Done Criteria

- `GradingEvaluationServiceTest` 覆盖 MAE、`gradeAgreementRate`、`wrongCauseAgreementRate`、`questionType` / `knowledgePointId` / `rubricVersion` 分组。
- `AssessmentControllerTest#exposesAutomatedGradingQualityEvaluationReport` 覆盖响应含总体指标和 `groupedAnalysis`。
- `POST /api/assessment/grading-evaluations` 支持新版 `samples` 请求。
- 旧版 `humanScores` / `aiScores` 请求保持可用。
- 不新增数据库表或依赖。
- 不修改共享收口文件。
