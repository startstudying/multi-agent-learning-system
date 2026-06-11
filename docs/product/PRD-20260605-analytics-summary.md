# Analytics 学习分析扩展 PRD

## 背景

后端 TODO P1-5 要求补齐学习分析 API，P2-4 要求成本与 Token 预算治理继续保留按模型、按 Agent Task 的统计能力，并尽量增加更多聚合维度。本次只由后端 analytics worker 执行，避免修改其他模块。

## 目标

- 保留 `GET /api/analytics/overview` 的兼容性。
- 新增学生端 `GET /api/analytics/students/{learnerId}/summary`。
- 在 overview 中新增 admin/agent summary 字段，展示 Agent 成功率、失败率、平均延迟、Token 成本、RAG 命中率。
- 保留 `tokenUsage.byAgentTask` 与 `tokenUsage.byModel`，可行时增加 `byUser` 与 `byAgentName`。
- 为 overview 兼容性和 student summary 增加测试覆盖。

## 非目标

- 不新增数据库表、迁移脚本或外部依赖。
- 不实现教师端 class summary。
- 不修改 learning、assessment、agent、rag 等 domain repository。
- 不引入真实 LLM 调用或预算拦截规则。

## 用户价值

- 学生能看到自己的学习进度、掌握度趋势、最近错因和下一步建议。
- 管理端能从现有日志了解 Agent 运行健康、成本和 RAG 命中情况。
- 已有 token 成本统计能力不被破坏。
