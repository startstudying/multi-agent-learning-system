# RAG 质量评估 PRD

## 背景

当前后端已经有 `RagEvaluationService` 和 `/api/rag/evaluations` 初步接口，可根据期望来源和实际引用计算基础指标。但 P2-2 仍缺少课程级 benchmark 汇总、无来源拒答指标和可归档报告字段，难以支撑论文实验、答辩演示和后续 prompt version 质量对比。

## 目标

- 在现有 RAG evaluation API 上补齐最小可验收的课程 benchmark 评估切片。
- 支持按样本输入期望 chunk、实际引用、答案文本和 no-source 预期计算：
  - `recallAtK`
  - `citationAccuracy`
  - `groundedness`
  - `noSourceRefusalRate`
- 输出 benchmark 汇总和可归档报告文本，便于后续保存到 evaluation run 或人工归档。

## 用户价值

- 教师或研发人员可以用固定课程样本评估 RAG 检索与引用质量。
- 无来源场景可以被单独衡量，避免系统在没有课程依据时编造答案。
- 评估结果可作为 prompt version 对比和答辩材料的质量证据。

## 非目标

- 本轮不改 `evaluation` 包，不新增 evaluation run 自动记录。
- 本轮不新增数据库表或迁移。
- 本轮不执行真实 RAG 检索或模型调用，只评估已提供的 benchmark 输出。
- 本轮不新增依赖，不新增前端页面。
