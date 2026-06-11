# Retrospective - Learner Profile 维度与快照

## Feature Summary

本切片完成 P1-1 Learner Profile 更新策略的后端收口：画像维度扩展为七个产品字段，维度证据增加 `lastEvidenceId`，路径规划和资源生成都保存并返回当时引用的 `profileSnapshot`。

## What Went Well

- RED 测试直接暴露 DTO、实体和 migration 缺失。
- 资源生成快照加入模型请求 payload 后，画像上下文不再只停留在 API 展示层。
- 回归中补了历史字段别名兼容，避免旧画像 JSON 在新快照中退化为 `unknown`。

## What Didn't Go Well

- 宽回归发现 `LearningWorkflowServiceTest` 仍停留在 6 维旧契约，需要同步到 P1-1 的 7 维定义。
- 历史 profile 的 `weakPointsJson` / `preferencesJson` 初版没有进入快照，后来通过 RED 补齐。
- 前一轮执行时间过长且没有及时汇报，后续应按单切片闭环并及时停止无界并行。

## Process Improvements

- 子 Agent 只用于前置只读分析，集成修补阶段不再继续开新 subagent。
- 每个切片先跑最小 RED/GREEN，再跑宽回归；失败必须写入 evidence。
- 对兼容性字段，测试应同时覆盖 JSON 结构字段和实体独立列。

## Action Items

| Action | Owner | TASK |
|---|---|---|
| 将 `profileSnapshot` 结构化为可分析审计数据 | 后续 | Analytics / Profile governance |
| 教师端画像标注权限和 UI | 后续 | Teacher profile note |
