# Orchestrator 节点契约与策略显式化复盘

## 1. 做对的事

- 先用 RED 测试证明 API 缺少节点契约字段，再实现最小代码。
- 采用现有 `steps[]` 响应扩展，避免新增 DB 和不必要 endpoint。
- 子代理只读分析，主线程单点集成，避免文件冲突。
- 安全建议中可立即落地的 `nextActions` 修正已纳入本切片。

## 2. 发现的问题

- 初版 `step_runtime_failure` 映射成入口 DTO，整类测试暴露后改为业务 DTO。
- 当前 `LEARNING_GOAL_CREATION` 枚举存在但未实现完整 workflow，应在后续处理，避免入口接受后长期 `RUNNING`。
- 资源生成模型失败 summary 仍可能包含 provider message，后续需要统一 public error code。

## 3. 后续建议

- P0-3 继续补齐长任务 `retry_count`、`next_retry_at`、`last_error`、`recoverable`。
- 为 `ANSWER_SUBMISSION` workflow envelope 增加 `answerHash`，避免同长度不同答案误 replay。
- 将 workflow 查询从 `inputJsonContaining` 迁移为结构化 workflow 表或索引字段。

## 4. Skill Extraction

不新增项目专属 skill。本切片模式可复用现有 `agent-trace-governance` 和 `ai-learning-architecture`。
