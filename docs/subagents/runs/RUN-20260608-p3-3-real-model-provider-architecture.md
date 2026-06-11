# P3-3 真实模型 Provider 架构边界分析

## 结论

当前 P3-3 的缺口是 adapter 装配缺失，不是业务链路缺失。资源生成已经通过 `AiModelGateway`，RAG/Embedding 已有 `EmbeddingService` 和 `VectorIndexAdapter` 边界，模型日志、token/cost、trace 基础闭环已经具备。真实 Chat/Embedding provider 应只接入这些边界内部。

## 关键证据

- `docs/planning/backend-architecture-todolist.md` 仍有 P3-3 未完成项：用 Spring AI/Spring AI Alibaba 接入真实 Chat/Embedding 模型。
- `backend/pom.xml` 已有 Spring AI BOM，但没有具体 provider starter。
- `backend/src/main/resources/application.yml` 已有 `AI_MODEL_PROVIDER`、`AI_CHAT_MODEL`、`AI_EMBEDDING_MODEL` 配置位，默认 provider 为 `none`。
- `backend/src/main/java/com/learningos/agent/application/AiModelGateway.java` 当前 `generateStructured(...)` 返回 deterministic / gateway-placeholder，`externalCall=false`。
- `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java` 当前 provider 配置完整时返回 `EMBEDDING_PROVIDER_NOT_CONFIGURED`。
- `ResourceGenerationService` 调用 `AiModelGateway.generateStructuredWithRetry(...)`；Controller、Agent、Orchestrator 未直接调用 provider SDK。
- `RagQueryService` 当前回答由检索结果拼接与引用产生，不在本切片改成 Chat 生成。
- `VectorIndexAdapter` 默认 `NoopVectorIndexAdapter`，本切片不引入 VectorDB。

## 架构建议

1. 在 `AiModelGateway` 内部挂接 Chat adapter。provider 为 `none` 时继续 deterministic；provider 非 `none` 且 bean 缺失时 fail closed，不再产生假的 provider 成功。
2. 在 `EmbeddingService` 内部挂接 Embedding adapter。provider 为 `none` 或 embedding model 为空时仍为 `DISABLED`；provider 配置完整但 bean 缺失时返回固定安全错误。
3. Spring AI SDK 只允许在 gateway/embedding adapter 内部出现，禁止 Controller、业务 Service、Agent Tool、RAG 查询层直接注入 provider SDK。
4. 复用现有结构化输出校验、provider 归一化、`MODEL_PROVIDER_ERROR` / `STRUCTURED_OUTPUT_INVALID` 安全错误、`model_call_log` 与 `token_usage_log` 记录。
5. 本切片不修改 API、DB schema、frontend，不新增 VectorDB。

## 可改范围建议

- `backend/pom.xml`
- `backend/src/main/java/com/learningos/agent/application/AiModelGateway.java`
- `backend/src/main/java/com/learningos/agent/application/provider/*`
- `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java`
- `backend/src/main/java/com/learningos/rag/application/provider/*`
- `backend/src/test/java/com/learningos/agent/application/*`
- `backend/src/test/java/com/learningos/rag/application/*`
- 本切片 workflow、security、evidence、acceptance、memory、changelog 文档

## 不建议改动

- 不改 Controller 直接调用模型 SDK。
- 不改 `ResourceGenerationService` 感知 Spring AI SDK。
- 不改前端保存 provider key 或调用 LLM API。
- 不把 API key、base URL、raw provider error、raw prompt、raw chunk、raw vector 写入 DB、trace、metadata 或日志。
- 不改 `ModelCallLog` / `TokenUsageLog` schema。

## 集成判断

采用 L1 并行分析 + 主 Codex 串行实现。先接 OpenAI-compatible Spring AI starter，完成 Chat 与 Embedding provider boundary；VectorDB 和 Spring AI Alibaba DashScope 作为后续单独 dependency review。
