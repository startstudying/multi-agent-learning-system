# 自动批改质量评估 PRD

## 背景

当前系统已经有 `GradingEvaluationService` 和 `POST /api/assessment/grading-evaluations` 的基础能力，只能按两组分数计算平均绝对误差和简单一致率。TODO P2-3 需要补齐更贴近人工评分样本集的离线评估报告，让论文答辩和后续 prompt 质量对比能说明自动批改是否可靠。

## 目标

- 支持请求中直接提交人工评分样本 `samples`，包含题型、知识点、rubric 版本、人工分、系统分、人工等级、系统等级、人工错因、系统错因。
- 计算总体指标：
  - `meanAbsoluteError`
  - `gradeAgreementRate`
  - `wrongCauseAgreementRate`
  - `sampleCount`
- 增加按 `questionType`、`knowledgePointId`、`rubricVersion` 的分组分析。
- 不新增数据库表，优先生成离线评估报告。
- 保留旧版 `humanScores` / `aiScores` / `agreementThreshold` 请求兼容。

## 非目标

- 不改答题提交流程。
- 不接入真实 LLM 批改器。
- 不持久化 evaluation run。
- 不修改 `evaluation` 包或 V14 migration。
- 不新增依赖。

## 用户价值

- 教师或研究人员可用人工评分样本快速验证自动批改质量。
- 后端可提供可测试、可复现的指标口径，支撑后续 prompt version 对比。
- 分组结果可帮助定位某类题型、知识点或 rubric 版本下的批改偏差。
