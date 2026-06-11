# Prompt Version 质量对比 PRD

## 背景

当前后端已经具备 `prompt_version` 管理、模型调用 prompt metadata 记录，以及 `evaluation_set` / `evaluation_sample` 样本集管理能力。但系统还不能回答论文和答辩中最关键的问题：同一批评测样本在不同 prompt version 下，RAG、自动批改或资源生成的质量指标是否提升。

## 目标

- 增加 Evaluation Run 持久化能力，记录某个 evaluation set 在某个 prompt version 下的一次评测结果。
- 支持按 `evaluationSetId + promptCode + promptVersion` 对比质量指标。
- 对比结果给出 baseline、各版本指标、相对 baseline 的 delta，以及每个指标的最优版本。

## 用户价值

- 论文实验可以用同一 evaluation set 对比不同 prompt version。
- 后端可沉淀可复现的质量证据，而不是只靠一次性接口返回。
- 后续可扩展为 RAG、批改、资源生成的离线评测和周期性评测报告。

## 非目标

- 本轮不做真实模型 A/B 执行器。
- 本轮不改造 `ResourceGenerationService` 以支持任意 prompt version。
- 本轮不保存 raw prompt、raw model output、学生原始答案全文。
- 本轮不做前端页面。

