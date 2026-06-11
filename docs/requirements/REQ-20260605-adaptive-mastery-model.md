# 自适应掌握度模型需求

## 功能需求

1. 提交答案时，系统必须根据 `questionId` 推导目标 `knowledgePointId`。
2. 系统必须读取 `mastery_record` 中当前学习者和目标知识点的最新记录作为 `beforeMastery`。
3. 当没有历史掌握度记录时，系统使用默认初始掌握度 `0.42`。
4. 系统必须根据错因类别计算 `afterMastery`：
   - `TRANSFER_WEAKNESS`：认可核心概念但迁移不足，允许提升或校准到上限。
   - `STEP_ERROR`：部分掌握，温和提升。
   - `INCOMPLETE_EXPRESSION`：表达不足，小幅提升上限。
   - `CONCEPT_ERROR`：核心概念错误，小幅下降或校准到低掌握度。
5. 掌握度必须始终在 `[0.0, 1.0]` 范围内，不允许 NaN 或 Infinity。
6. `reasonSummary` 必须包含掌握度更新前后值，支撑路径重规划解释。
7. 现有 `AnswerSubmitResponse` 字段保持兼容。

## 约束

- Controller 只做 HTTP 委托。
- 仓储读取必须在 Service 层完成。
- 不新增数据库迁移。
- 不新增外部依赖。
- 不让 Agent 或 Tool 直接访问 Repository。
