# RUN-20260608 backend TODO P3-3 Model Provider Expert

## 结论

P3-3 当前已具备 `AiModelGateway` 与 `EmbeddingService` 边界、结构化输出校验、provider 低基数观测和安全错误码，但没有真实 Spring AI / Spring AI Alibaba provider SDK 调用。

## 关键证据

- `AiModelGateway` 当前仍是 placeholder/deterministic 输出，`externalCall=false`。
- `EmbeddingService` 在 provider/model 配置后仍返回 `EMBEDDING_PROVIDER_NOT_CONFIGURED`，没有外呼。
- `AiModelProperties` 只有 `provider/chatModel/embeddingModel`，缺少真实 provider 所需的 key、baseUrl、timeout、retry、temperature、token 等配置。
- `pom.xml` 有 Spring AI BOM，但没有具体 model starter。
- 测试 profile 默认 `provider=none`，普通 `mvn test` 不应外呼。

## 推荐

下一步 P3-3 不应直接加 SDK。先做“provider config adapter skeleton”：

- 继续让 `AiModelGateway` 做唯一 Chat 边界。
- 继续让 `EmbeddingService` 做唯一 Embedding 边界。
- 抽出 adapter 接口与 noop 实现。
- 不新增依赖、不外呼。

真实 SDK 切片必须先核验官方文档并创建 dependency review。
