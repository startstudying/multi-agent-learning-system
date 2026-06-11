# RUN-20260608 P3 Next Model Provider

## 1. 当前 pom/config/AiModelGateway/EmbeddingService 现状

- `backend/pom.xml` 有 `spring-ai.version=1.0.3` 和 `spring-ai-bom` dependency management，但没有任何 `spring-ai-*starter*` 或 Alibaba provider starter。
- 当前配置只有 `learning-os.ai-model.provider/chat-model/embedding-model`，没有 Spring AI 官方 provider 配置段。
- `AiModelGateway` 当前是 provider 归一化 + deterministic/gateway-placeholder 输出，`externalCall=false`。
- `ResourceGenerationService` 正确只通过 `AiModelGateway` 发起模型调用。
- `EmbeddingService` 当前是 contract：未配置时 `DISABLED`，配置 provider + embedding model 后返回安全 `PROVIDER_ERROR / EMBEDDING_PROVIDER_NOT_CONFIGURED`。

## 2. 是否已有 Spring AI 依赖可用

没有。当前只有 Spring AI BOM，没有主代码 `ChatClient`、`ChatModel`、`EmbeddingModel` 使用，也没有 provider starter。

## 3. 不新增依赖的配置型 provider adapter 是否可行

不可行。当前配置项只能让 gateway 报告 provider/model 名称，不会触发真实外部调用；测试也覆盖了 configured provider 不外呼的行为。

## 4. 新增依赖时需要的 dependency review

至少需要：

1. Spring AI Chat provider starter review。
2. Spring AI Embedding provider starter review。
3. 如选择 DashScope / Spring AI Alibaba，需要单独确认 Maven 坐标、版本兼容、配置属性和商业 API 风险。
4. Security review：API key 只能后端环境变量注入，不进 DB/log/trace/metrics；raw provider error 必须映射成安全码。

## 5. 推荐最小可验收切片

推荐先做“dependency/security review + gateway 内 provider adapter seam”，不碰业务服务调用点：

- `ResourceGenerationService` 仍只调用 `AiModelGateway`。
- 默认 `provider=none` 继续 deterministic/noop，CI/test/local 不依赖外网或密钥。
- Chat 最小覆盖 `agent-resource-v1`，真实响应进入 gateway 后继续执行 `resources[]`、必填字段、`safetyStatus` 校验。
- Embedding 最小让 `EmbeddingService.embedDocumentChunks(...)` 能返回 `SUCCEEDED` 以及模型版本/latency；VectorDB 仍可 noop，避免扩大到 P3-2。

## 6. 测试建议

- `AiModelGatewayTest`：provider mock 成功、timeout/error 映射 `MODEL_PROVIDER_ERROR`、raw error 不进入 exception/log/trace。
- `AgentRunRecorderTest`：成功/失败都持久化 provider、model、latency、token，provider 仍低基数。
- `EmbeddingServiceTest`：mock embedding 成功、provider 失败安全码、维度不匹配。
- opt-in smoke：真实 provider 仅在显式 profile/env 下运行。

## 7. 结论

P3-3 未完成的根因不是业务调用链缺口，而是 provider runtime 缺失。不能在未做 dependency/security review 的情况下直接修改 `pom.xml` 接入模型 SDK。
