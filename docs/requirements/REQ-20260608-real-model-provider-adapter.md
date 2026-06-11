# REQ - P3-3 真实模型 Provider Adapter

## 1. 追踪

- PRD：`docs/product/PRD-20260608-real-model-provider-adapter.md`
- 需求编号：REQ-20260608-real-model-provider-adapter

## 2. 功能需求

| 编号 | 需求描述 | 优先级 | 验收标准 |
|---|---|---|---|
| FR-01 | 后端引入 Spring AI 官方 OpenAI-compatible provider starter，并升级 Spring AI BOM 到安全的 1.0.x patch 版本。 | 必须 | `backend/pom.xml` 包含 `spring-ai-starter-model-openai`，`spring-ai.version=1.0.8`，`mvn dependency:tree` 可执行。 |
| FR-02 | `AiModelGateway` 在 provider 非 `none` 且 `chatModel` 配置完整时，通过内部 adapter 调用 Spring AI `ChatModel`。 | 必须 | 测试可用 fake `ChatModel` 验证 adapter 返回 provider/model/content/token/structuredOutput。 |
| FR-03 | provider 为 `none` 时，`AiModelGateway` 保持 deterministic 输出，不需要 provider bean 或 key。 | 必须 | 既有 deterministic 测试保持通过。 |
| FR-04 | provider 非 `none` 但 `ChatModel` bean 缺失时，`AiModelGateway` 必须 fail closed。 | 必须 | 返回 `MODEL_PROVIDER_ERROR`，不产生 gateway-placeholder 成功。 |
| FR-05 | `EmbeddingService` 在 provider 非 `none` 且 `embeddingModel` 配置完整时，通过内部 adapter 调用 Spring AI `EmbeddingModel`。 | 必须 | 测试可用 fake `EmbeddingModel` 验证返回 `EmbeddingStatus.SUCCEEDED`。 |
| FR-06 | provider 未配置或 `embeddingModel` 为空时，`EmbeddingService` 保持 `DISABLED`。 | 必须 | 既有 disabled 测试保持通过。 |
| FR-07 | provider 异常、结构化输出异常、embedding 异常必须映射为固定安全错误码。 | 必须 | 测试断言异常 message / failure recorder / batch result 不包含 raw error、secret、prompt、chunk。 |

## 3. 非功能需求

| 编号 | 需求描述 | 优先级 |
|---|---|---|
| NFR-01 | 不允许 Controller、业务 Service、Agent Tool、RAG 查询层直接注入 Spring AI provider SDK。 | 必须 |
| NFR-02 | 不允许 API key、base URL、organization/project、raw provider response、raw vector 写入 DB、trace、日志、metrics、memory、evidence。 | 必须 |
| NFR-03 | 不修改外部 API、DB schema、frontend。 | 必须 |
| NFR-04 | 新依赖必须先完成 dependency review。 | 必须 |
| NFR-05 | 默认测试不依赖真实 API key，不发生真实外呼。 | 必须 |

## 4. 用户流程

```text
运维配置 AI_MODEL_PROVIDER=openai / AI_CHAT_MODEL / AI_EMBEDDING_MODEL / spring.ai.openai.api-key
-> Spring Boot 创建 ChatModel / EmbeddingModel
-> 资源生成请求进入 ResourceGenerationService
-> 业务服务调用 AiModelGateway
-> gateway adapter 调用 ChatModel 并校验 JSON schema
-> model_call_log / token_usage_log 写入安全 evidence
```

```text
RAG 文档索引
-> IndexService 产出 chunk
-> EmbeddingService 调用 EmbeddingModel
-> 返回 SUCCEEDED 后继续 VectorIndexAdapter
-> 当前默认 NoopVectorIndexAdapter 仍表示 vector index disabled
```

## 5. 输入 / 输出

### 输入

| 字段 | 来源 | 规则 |
|---|---|---|
| `learning-os.ai-model.provider` | 环境变量 `AI_MODEL_PROVIDER` | `none/openai/dashscope/anthropic/gemini/mock` 或归一化为 `other` |
| `learning-os.ai-model.chat-model` | 环境变量 `AI_CHAT_MODEL` | 非空时用于 Chat model evidence |
| `learning-os.ai-model.embedding-model` | 环境变量 `AI_EMBEDDING_MODEL` | 非空时用于 embedding model version |
| `spring.ai.openai.api-key` | 环境变量 / Secret Manager | 不写默认值，不进入文档 memory/evidence |

### 输出

| 字段 | 类型 | 说明 |
|---|---|---|
| `AiModelGateway.ModelResponse` | record | provider/model/status/content/structuredOutput/tokenUsage/latencyMs |
| `EmbeddingBatchResult` | record | status/modelVersion/chunkCount/latencyMs/errorCode |

## 6. 边界情况

| 场景 | 预期行为 |
|---|---|
| provider `none` | Chat deterministic，Embedding disabled。 |
| provider 非 `none` 但 chat model 为空 | Chat 使用 deterministic fallback，不外呼。 |
| provider 非 `none` 且 chat model 非空但 `ChatModel` bean 缺失 | `MODEL_PROVIDER_ERROR`。 |
| Chat provider 返回非 JSON 或缺字段 | `STRUCTURED_OUTPUT_INVALID`。 |
| Chat provider 抛 raw secret/error | 只记录 `MODEL_PROVIDER_ERROR`。 |
| Embedding provider bean 缺失 | `EMBEDDING_PROVIDER_NOT_CONFIGURED`。 |
| Embedding provider 抛 raw secret/error | `EMBEDDING_PROVIDER_ERROR`。 |

## 7. 依赖关系

- 上游：`ResourceGenerationService`、`IndexService`。
- 下游：Spring AI `ChatModel`、`EmbeddingModel`。

## 8. 审批

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-08 | APPROVED |
