# RUN-20260608 后端 P3 生产化 Model Gateway Expert

## 角色

Model Gateway / Provider Integration Expert

## 当前事实

- `docs/planning/backend-architecture-todolist.md` 中 P3-3 仍保留真实 Chat/Embedding provider 接入与 `model_call_log.provider` 落库两个未完成点。
- `backend/pom.xml` 当前只导入 Spring AI BOM，没有真实 Chat / Embedding provider starter。
- `AiModelGateway` 已形成业务模型调用边界，但当前非 `none` provider 仍返回 placeholder，不执行真实外部调用。
- `ResourceGenerationService` 已经通过 `AiModelGateway` 调用模型，业务服务没有直接调用 provider SDK。
- `AiModelGateway` 已校验 `agent-resource-v1` 结构化输出，并把 provider 异常和 schema 异常收敛为安全错误码。
- `model_call_log` 当前记录 model、promptCode、promptVersion、temperature、structuredOutputSchema、status、latency、safe error 和 cost，但没有 provider 字段。
- token usage 当前通过 `token_usage_log` 单独记录，不在 `model_call_log` 单表内。
- `EmbeddingService` 已有 batch contract 和 enabled/disabled 语义，但没有真实 embedding provider。

## 建议切片

### P3-3-B provider observability schema

先补 `model_call_log.provider`：

- 新增 Flyway migration。
- `ModelCallLog` 增加 `provider` 字段。
- `AgentRunRecorder` 成功和失败均写入 provider。
- MySQL smoke 覆盖新增列。

该切片不接真实模型、不新增 provider SDK。

### P3-3-C Chat provider adapter

在 `AiModelGateway` 后面新增真实 Chat adapter：

- 业务服务仍只调用 `AiModelGateway`。
- adapter 封装 Spring AI `ChatModel` / `ChatClient`。
- `AiModelGateway` 继续负责计时、结构化输出校验、重试和 safe error 映射。
- API key / baseUrl / timeout 只来自环境变量或安全配置，不写日志和 health 响应。

该切片需要依赖审查。

### P3-3-D Embedding provider adapter

在 `EmbeddingService` 后面接真实 embedding provider：

- `IndexService` 继续只调用 `EmbeddingService.embedDocumentChunks(...)`。
- provider failure 只返回稳定 safe code。
- batch embedding 失败不能留下新可检索 chunk。

该切片建议在 Chat adapter 和 provider 日志闭环之后执行。

## 依赖 / 配置 / migration 结论

- provider adapter 需要新增 provider starter，因此必须先创建 `docs/security/DEPENDENCY-REVIEW-*.md`。
- `AiModelProperties` 需要补充 api key、base url、timeout 等配置项，但 secret 只能来自环境变量。
- `application.yml` 可以新增环境变量映射，但不得让 secret 出现在日志、health 或 memory 文档。
- `model_call_log.provider` 需要 schema migration；建议先做独立 P3-3-B。

## 当前总控决策

当前 P3-4-C 不修改模型网关、依赖、配置或 schema。下一推荐切片是 P3-3-B：`model_call_log.provider` schema/provider observability。
