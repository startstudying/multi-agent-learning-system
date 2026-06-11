# RUN-20260607 Backend/Spring AI Model Boundary Expert

## 任务

围绕 `docs/planning/backend-architecture-todolist.md` 中 P3-3 模型接入边界做只读专家分析。

## 1. 当前已实现证据

- P3-3 在 TODO 中仍为未完成：真实 Chat/Embedding、统一 `AiModelGateway`、结构化输出 schema 校验/降级、provider/model/prompt/latency/token/error 全量日志。
- `AiModelGateway` 已存在边界、重试和失败记录雏形，但当前仍是 deterministic placeholder，即使 provider configured 也不实际调用外部模型。
- `ResourceGenerationService` 已经通过 `AiModelGateway` 走模型边界，但资源正文仍由确定性模板生成，未消费 `structuredOutput()` 作为业务事实。
- `pom.xml` 只有 Spring AI BOM，没有真实 provider starter。
- `application.yml` / `application-test.yml` 已有 `learning-os.ai-model.provider/chat-model/embedding-model` 配置，测试默认 `provider=none`。
- `EmbeddingService` 仍为 `noop-embedding-v1`，无真实 embedding API。
- `model_call_log` 已记录 model、prompt metadata、temperature、schema、status、latency、error、cost，但缺少 provider 字段；成功/失败日志中的 model 来源仍固定为 deterministic trace model。

## 2. 未完成项拆分

1. 真实 Chat provider 未接入：缺少 `ChatClient` / provider adapter。
2. 真实 Embedding provider 未接入：`EmbeddingService` 无 `embed()` 能力。
3. 结构化输出 schema 校验缺失：没有 required field/type/fallback 校验。
4. 模型日志未全量：缺少 provider，且 model/token/latency 未完全以 gateway 返回为事实源。
5. Gateway 边界需防回退：需要测试约束业务服务不能直接注入 Spring AI SDK 类型。

## 3. 建议最小实现方案

### P3-3-A：不新增依赖的边界硬化

- 扩展 `model_call_log.provider`，统一用 gateway evidence 写成功/失败模型日志。
- 在 `AiModelGateway` 内做基于 Jackson/DTO 的最小结构化输出校验。
- Resource generation 在结构化输出有效时映射模型结果，无效时使用 deterministic fallback 并记录降级原因。
- 增加边界测试：configured provider no-key fallback、schema 缺字段 fallback、模型日志 provider/model/prompt/latency/token/error 记录、防止业务层直接依赖 SDK。

### P3-3-B：真实 provider 接入

- 先走 `docs/security/` dependency review。
- 只在 `AiModelGateway` 注入 Spring AI `ChatModel` / `ChatClient` 或 Alibaba adapter。
- `provider=none` 保持默认可测试 fallback。
- Embedding 单独通过 `EmbeddingGateway` 或 `AiModelGateway` embedding 方法接入。

## 4. 依赖判断

P3-3-A 默认不新增依赖；P3-3-B 真实 provider 接入需要新增 starter，必须先做依赖评审。

## 5. 建议测试命令

```powershell
cd backend
mvn --% -Dtest=AiModelGatewayTest,AgentRunRecorderTest,ResourceGenerationControllerTest,SchemaConvergenceMigrationTest,HealthServiceTest test
mvn test
```

如新增 migration，追加 MySQL smoke。
