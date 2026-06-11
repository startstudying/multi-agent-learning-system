# REQ-20260611 后端架构后续增强计划收口

## 功能需求

| ID | 需求 | 优先级 |
|---|---|---|
| REQ-1 | 保存专家 subagent 的并行分析报告和集成评审 | P0 |
| REQ-2 | 将 Qdrant external smoke 从占位测试改为真实 opt-in health / collection / dimension 检查 | P0 |
| REQ-3 | 将 Model Provider external smoke 从占位测试改为真实 opt-in OpenAI-compatible chat endpoint 检查 | P0 |
| REQ-4 | 同步 `backend-architecture-todolist.md` 中已由现有实现覆盖的后续增强项 | P0 |
| REQ-5 | 记录不能默认执行的外部服务 smoke 限制和后续风险 | P0 |

## 约束

- 默认测试不得依赖 Qdrant、外部模型 provider、API key 或公网。
- 不新增依赖、DB migration、REST API、前端页面。
- 不把真实密钥、外部 smoke 输出、raw provider error 写入文档或 memory。
- 工业级 Parser/OCR 和 DashScope 专用 provider 不在本轮扩大实现。
