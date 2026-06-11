# SPEC-20260611 后端架构后续增强计划收口

## 设计

本轮采用“事实核验 + 最小补缺 + 计划同步”的收口方式。

## Qdrant Opt-in Smoke

- 测试类：`QdrantVectorExternalSmokeTest`
- 默认条件：未设置 `QDRANT_SMOKE_ENABLED=true` 时跳过。
- 显式运行时读取：
  - `RAG_VECTOR_QDRANT_HOST`
  - `RAG_VECTOR_QDRANT_PORT`
  - `RAG_VECTOR_QDRANT_TLS`
  - `RAG_VECTOR_QDRANT_API_KEY`
  - `RAG_VECTOR_QDRANT_COLLECTION`
  - `RAG_VECTOR_QDRANT_EXPECTED_DIMENSION`
  - `RAG_VECTOR_QDRANT_TIMEOUT_MS`
- 验证：
  - Qdrant client 可连接。
  - collection 存在。
  - 如设置 expected dimension，则 health probe 校验实际维度一致。
  - 断言输出不包含 api key、`secret`、`sk-`。

## Model Provider Opt-in Smoke

- 测试类：`ModelProviderExternalSmokeTest`
- 默认条件：未设置 `MODEL_PROVIDER_SMOKE_ENABLED=true` 时跳过。
- 显式运行时读取：
  - `MODEL_PROVIDER_CODE`
  - `MODEL_PROVIDER_BASE_URL`
  - `MODEL_PROVIDER_API_KEY`
  - `MODEL_PROVIDER_CHAT_MODEL`
- 验证：
  - 通过 `OpenAiCompatibleChatModelFactory` 创建模型。
  - 发送最小 ping prompt。
  - 响应非空。
  - metadata model 不暴露 URL、key 或 secret。

## 安全边界

- 所有 external smoke 均 opt-in。
- 本轮不保存外部服务返回体。
- 后续 provider/webhook 出站 URL allowlist 单独立项，不在本轮扩大。

## 架构漂移

- 不改变 Controller / Service / Repository 分层。
- VectorDB 仍通过 `VectorIndexAdapter`。
- Provider 仍通过 `AiModelGateway` / `EmbeddingService`。
