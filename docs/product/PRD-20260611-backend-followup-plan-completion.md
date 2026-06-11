# PRD-20260611 后端架构后续增强计划收口

## 背景

`docs/planning/backend-architecture-todolist.md` 的 MVP 主计划已经完成，但文末“后续增强”仍包含多项未勾选工作。用户要求完成该计划，并明确要求使用专家 subagent 并行开发。

## 目标

- 对后续增强项做事实核验，区分已实现、已具备最小能力、必须继续拆分的生产化深水区。
- 使用专家 subagent 并行分析 RAG/Vector、Backend、Security & Quality 风险。
- 对可在本轮安全完成的缺口进行最小实现。
- 更新计划、证据、验收、记忆和 changelog。

## 非目标

- 不在本轮新增 native/cloud OCR、DashScope 专用 SDK 或新的大依赖。
- 不引入新的前端页面。
- 不把外部 provider 或 Qdrant smoke 纳入默认 `mvn test`。
- 不修改 REST API 合同或数据库 schema。

## 验收口径

- 后续增强计划中的已实现项有代码/测试/证据对应。
- 外部 smoke 从占位变成真实 opt-in 测试。
- 未能在无凭证环境真实执行的外部 smoke 明确记录为 opt-in 限制。
- 专家报告和集成评审落盘。
