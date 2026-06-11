# P3-3 真实模型 Provider 专家集成评审

## 输入报告

- `docs/subagents/runs/RUN-20260608-p3-3-real-model-provider-dependency.md`
- `docs/subagents/runs/RUN-20260608-p3-3-real-model-provider-architecture.md`
- `docs/subagents/runs/RUN-20260608-p3-3-real-model-provider-security.md`

## 集成结论

本切片采用 **Spring AI 官方 OpenAI-compatible provider**，不同时引入 Spring AI Alibaba DashScope，不引入 VectorDB。原因：

1. 当前项目已有 Spring AI BOM，且 P3-3 的最小目标是让现有 `AiModelGateway` / `EmbeddingService` 能真实调用 provider。
2. Dependency 专家建议先单 provider，避免 OpenAI + DashScope 双 starter 版本矩阵和配置冲突。
3. Security 专家要求真实 SDK 只能封装在 gateway/adapter 边界内，并禁止 raw provider error、secret、prompt、chunk、vector 泄露。
4. Architect 专家确认业务链路已具备，缺的是 adapter 装配；不需要改 Controller、frontend、DB schema。

## 冲突处理

| 冲突点 | 决策 |
|---|---|
| P3-3 文案包含 Spring AI/Spring AI Alibaba | 本切片选择 Spring AI OpenAI-compatible；DashScope 后续独立切片。 |
| 是否同时接 VectorDB | 不接。Embedding provider 可返回成功并记录 batch 状态，但真实语义检索需后续 VectorDB adapter。 |
| provider 配置但 bean 缺失时是否 fallback placeholder | 不 fallback 成功；必须 fail closed，返回固定安全错误。 |
| Spring AI BOM 是否继续 1.0.3 | 不继续；dependency review 批准升级到 1.0.8，同一 1.0.x 稳定线，规避已知 Spring AI 安全公告风险。 |

## 最终实施边界

- 新增 `org.springframework.ai:spring-ai-starter-model-openai`。
- 将 `spring-ai.version` 从 `1.0.3` 升级到 `1.0.8`。
- `AiModelGateway` 通过 Spring AI `ChatModel` adapter 调用真实 Chat provider。
- `EmbeddingService` 通过 Spring AI `EmbeddingModel` adapter 调用真实 Embedding provider。
- provider `none` 保持现有本地 deterministic/noop 行为。
- 不修改 API、DB schema、frontend、RAG answer generation、VectorDB。

## 实施模式

- Parallelism Level：L1 Parallel Analysis
- Implementation Mode：Single Codex 串行实现
- TDD：先写 gateway/embedding adapter RED，再写最小实现。
