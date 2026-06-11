# SPEC - P3-3 真实模型 Provider Adapter

## 1. 概述

本切片在现有模型边界内接入 Spring AI 官方 OpenAI-compatible provider。`AiModelGateway` 负责 Chat provider 调用、结构化输出校验、token/cost evidence；`EmbeddingService` 负责 Embedding provider 调用和 batch 状态。外部 API、数据库 schema、前端、VectorDB 不在本切片修改。

## 2. 追踪

- PRD：`docs/product/PRD-20260608-real-model-provider-adapter.md`
- REQ：`docs/requirements/REQ-20260608-real-model-provider-adapter.md`
- Dependency Review：`docs/security/DEPENDENCY-REVIEW-20260608-real-model-provider-adapter.md`

## 3. 领域模型

### Chat Provider Boundary

```text
ResourceGenerationService
-> AiModelGateway.generateStructuredWithRetry
-> ChatModelProviderAdapter
-> Spring AI ChatModel
-> AiModelGateway.ModelResponse
-> structured output validation
-> AgentRunRecorder model evidence
```

### Embedding Provider Boundary

```text
IndexService
-> EmbeddingService.embedDocumentChunks
-> EmbeddingModelProviderAdapter
-> Spring AI EmbeddingModel
-> EmbeddingBatchResult
-> VectorIndexAdapter
```

## 4. API 契约

本切片不新增、不修改 HTTP API。

## 5. 配置契约

```yaml
learning-os:
  ai-model:
    provider: ${AI_MODEL_PROVIDER:none}
    chat-model: ${AI_CHAT_MODEL:}
    embedding-model: ${AI_EMBEDDING_MODEL:}

spring:
  ai:
    model:
      chat: ${SPRING_AI_MODEL_CHAT:none}
      embedding: ${SPRING_AI_MODEL_EMBEDDING:none}
    openai:
      api-key: ${OPENAI_API_KEY:}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com}
      chat:
        options:
          model: ${AI_CHAT_MODEL:}
      embedding:
        options:
          model: ${AI_EMBEDDING_MODEL:}
```

说明：

- `spring.ai.openai.api-key` 只能来自环境变量或 Secret Manager。
- `OPENAI_BASE_URL` 仅允许运维受控配置，不允许来自请求参数、prompt、课程内容。
- Health、metrics、trace、DB 不输出 key/base-url/organization/project。

## 6. 后端流程

### `AiModelGateway.generateStructured`

1. 归一化 provider。
2. 若 provider 为 `none`，或 `chatModel` 为空，沿用 deterministic 输出。
3. 若 provider 非 `none` 且 `chatModel` 非空：
   - 调用内部 `ChatModelProviderAdapter`。
   - adapter 将 `ModelRequest` 组装为 Spring AI `Prompt`。
   - 调用 `ChatModel.call(Prompt)`。
   - 提取第一条 `Generation.output.text`。
   - 将文本解析为 JSON object。
   - 生成 `ModelResponse`。
4. `generateStructuredWithRetry` 继续负责最多 2 次重试、结构化校验、metrics、failure recorder。

### `EmbeddingService.embedDocumentChunks`

1. 若 provider 未配置或 `embeddingModel` 为空，返回 `DISABLED`。
2. 若 provider 配置完整但 `EmbeddingModel` bean 缺失，返回 `PROVIDER_ERROR / EMBEDDING_PROVIDER_NOT_CONFIGURED`。
3. 若 provider 配置完整且 `EmbeddingModel` bean 存在：
   - 调用内部 `EmbeddingModelProviderAdapter`。
   - 使用 `EmbeddingRequest` 批量传入 chunk content。
   - 确认返回向量数量与 chunk 数一致且每个 vector 非空。
   - 返回 `SUCCEEDED`。
4. provider 异常返回 `PROVIDER_ERROR / EMBEDDING_PROVIDER_ERROR`，不暴露 raw error。

## 7. Agent 工作流

资源生成 Agent 工作流不变。真实 Chat provider 的接入点仍是 `AiModelGateway`，业务服务不感知 Spring AI SDK。

## 8. RAG 工作流

RAG 文档索引进入真实 embedding provider 后，仍由当前 `VectorIndexAdapter` 控制向量入库。默认 `NoopVectorIndexAdapter` 不产生真实语义检索；这不是本切片缺陷，而是 VectorDB 后续切片边界。

## 9. 数据库变更

无数据库变更。

## 10. 状态流转

### Chat

```text
provider=none -> deterministic SUCCESS
provider configured + ChatModel bean -> provider SUCCESS / STRUCTURED_OUTPUT_INVALID / MODEL_PROVIDER_ERROR
provider configured + bean missing -> MODEL_PROVIDER_ERROR
```

### Embedding

```text
provider=none or embedding model blank -> DISABLED
provider configured + EmbeddingModel bean -> SUCCEEDED / PROVIDER_ERROR
provider configured + bean missing -> PROVIDER_ERROR
```

## 11. 错误处理

| 错误码 | 说明 | 触发条件 |
|---|---|---|
| `MODEL_PROVIDER_ERROR` | Chat provider 不可用或调用失败 | bean 缺失、timeout、rate limit、SDK exception |
| `STRUCTURED_OUTPUT_INVALID` | Chat provider 输出无法通过 schema | 非 JSON、缺字段、非法 `safetyStatus` |
| `EMBEDDING_PROVIDER_NOT_CONFIGURED` | Embedding provider bean 缺失 | provider/model 配置完整但无 `EmbeddingModel` |
| `EMBEDDING_PROVIDER_ERROR` | Embedding provider 调用失败 | SDK exception、返回数量不匹配、空向量 |

## 12. 权限规则

本切片不新增入口。所有模型调用必须在现有后端认证、对象级授权、课程/enrollment scope 之后发生。不得新增绕过权限的模型调用入口。

## 13. Trace / 日志

- `model_call_log.provider` 使用 gateway 归一化 provider。
- `model_call_log.model` 使用配置 model 或 provider response metadata model 的安全值。
- `token_usage_log` 使用 provider usage；缺失时使用现有保守估算。
- `errorMessage` 只允许固定错误码。
- 不记录 raw prompt、raw response、raw provider exception、raw chunk、raw vector、secret。

## 14. 测试策略

```powershell
cd backend
mvn --% -Dtest=AiModelGatewayTest,EmbeddingServiceTest test
mvn --% -Dtest=AiModelGatewayTest,EmbeddingServiceTest,ResourceGenerationControllerTest,RagQueryServiceTest,IndexServiceEmbeddingVectorTest,ChunkServiceVectorRetrievalTest test
mvn test
```

新增/更新测试：

- `AiModelGatewayTest`
  - fake `ChatModel` 返回合法 JSON，gateway 返回真实 provider response。
  - provider 配置完整但 bean 缺失时不返回 placeholder success。
  - raw provider error 脱敏。
  - 非 JSON / schema invalid 映射 `STRUCTURED_OUTPUT_INVALID`。
- `EmbeddingServiceTest`
  - fake `EmbeddingModel` 返回向量，batch result 为 `SUCCEEDED`。
  - bean 缺失时返回 `EMBEDDING_PROVIDER_NOT_CONFIGURED`。
  - raw provider error 脱敏为 `EMBEDDING_PROVIDER_ERROR`。

## 15. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller/业务服务不接触 provider SDK。 |
| Frontend rules | PASS | 不改 frontend，不保存 key。 |
| Agent / RAG rules | PASS | Agent 继续走 gateway；Embedding 继续走 service boundary。 |
| Security | PASS | 新依赖已审查；错误和配置脱敏。 |
| API / Database | PASS | 不改 API / schema。 |

## 16. 验收清单

- [ ] PRD / REQ / SPEC / PLAN / TASK / Context Pack 存在。
- [ ] Dependency Review 存在。
- [ ] Spring AI OpenAI starter 接入。
- [ ] Chat adapter TDD red-green 完成。
- [ ] Embedding adapter TDD red-green 完成。
- [ ] 安全错误脱敏测试通过。
- [ ] 回归测试通过或限制已记录。
- [ ] Evidence / Acceptance / Changelog / Memory 更新。
