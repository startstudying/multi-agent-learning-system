# EVIDENCE - P3-3 真实模型 Provider Adapter

## 1. 追踪

- PRD：`docs/product/PRD-20260608-real-model-provider-adapter.md`
- REQ：`docs/requirements/REQ-20260608-real-model-provider-adapter.md`
- SPEC：`docs/specs/SPEC-20260608-real-model-provider-adapter.md`
- PLAN：`docs/plans/PLAN-20260608-real-model-provider-adapter.md`
- TASK：`docs/tasks/TASK-20260608-real-model-provider-adapter.md`
- Context Pack：`docs/context/CONTEXT-20260608-real-model-provider-adapter.md`
- 日期：2026-06-08

## 2. 实现内容

本切片完成 P3-3 的最小真实模型接入边界：

- 通过 Spring AI 官方 OpenAI-compatible starter 接入 `ChatModel` 与 `EmbeddingModel`。
- `AiModelGateway` 在 `provider != none` 且配置了 chat model 时调用 Spring AI `ChatModel`。
- `EmbeddingService` 在 embedding provider 配置完整且 bean 存在时调用 Spring AI `EmbeddingModel`。
- 默认 `AI_MODEL_PROVIDER=none` 继续保持 deterministic/noop 本地兼容，不外呼。
- provider 配置完整但 Spring AI bean 缺失时 fail closed，不返回虚假成功 placeholder。
- provider raw error、prompt、chunk、secret 不进入异常消息、持久化证据或测试断言输出。
- OpenAI starter 默认会启用 audio/image/moderation 自动配置；已在 `application.yml` 显式设为 `none`，避免未使用模型族因缺少 API key 影响测试/启动。

本切片不接 DashScope、不接 VectorDB、不改 API、不改 DB migration、不改 frontend、不写入任何真实 secret。

## 3. 变更文件

| 文件 | 操作 | 摘要 |
|---|---|---|
| `backend/pom.xml` | 修改 | Spring AI BOM 升级到 `1.0.8`，新增 `spring-ai-starter-model-openai`。 |
| `backend/src/main/resources/application.yml` | 修改 | 增加 Spring AI chat/embedding/openai 配置；显式禁用 image/audio/moderation 默认 auto-config；所有 key/model/base-url 使用环境变量。 |
| `backend/src/main/java/com/learningos/agent/application/AiModelGateway.java` | 修改 | 注入可选 `ChatModel` / `ObjectMapper`，provider 配置完整时调用真实 adapter，解析 JSON，映射 usage/model，错误安全化。 |
| `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java` | 修改 | 注入可选 `EmbeddingModel`，批量调用 Spring AI embedding adapter，校验向量响应并返回安全错误码。 |
| `backend/src/test/java/com/learningos/agent/application/AiModelGatewayTest.java` | 修改 | 增加 fake `ChatModel` 成功、缺 bean fail-closed、非 JSON 输出、raw provider error 脱敏等覆盖；修复 Spring AI 1.0.8 `EmptyUsage.INSTANCE` API 差异。 |
| `backend/src/test/java/com/learningos/rag/application/EmbeddingServiceTest.java` | 修改 | 增加 fake `EmbeddingModel` 成功、缺 bean fail-closed、raw provider error 脱敏覆盖。 |
| `docs/product/PRD-20260608-real-model-provider-adapter.md` 等 workflow 文档 | 新增/更新 | 按 Spec-first 流程记录需求、规格、计划、任务、上下文、依赖审查与专家报告。 |

## 4. TDD RED/GREEN 记录

### RED

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AiModelGatewayTest,EmbeddingServiceTest test
```

结果：BUILD FAILURE。预期失败点：

- 测试已引用 Spring AI `ChatModel` / `EmbeddingModel` API，但实现/依赖尚未完全接通。
- 首轮 GREEN 后暴露 Spring AI 1.0.8 API 差异：`EmptyUsage.INSTANCE` 不存在。
- 旧测试仍期待 configured provider 返回 placeholder，与新 fail-closed 规格冲突。

### GREEN

修复点：

- `FixedUsage.getNativeUsage()` 改为返回 `Map.of()`，避免依赖不存在的 `EmptyUsage.INSTANCE`。
- 将旧 placeholder 测试调整为：provider 配置完整但缺 `ChatModel` bean 时返回安全 `MODEL_PROVIDER_ERROR`。
- 增加 fake adapter 路径验证真实 provider adapter 成功语义。
- 显式关闭未使用的 OpenAI audio/image/moderation auto-config，避免缺 API key 时 ApplicationContext 启动失败。

## 5. 测试结果

| 命令 | 结果 | 备注 |
|---|---|---|
| `mvn --% -Dtest=AiModelGatewayTest,EmbeddingServiceTest test` | PASS | BUILD SUCCESS；Tests run: 18, Failures: 0, Errors: 0, Skipped: 0 |
| `mvn --% -Dtest=AiModelGatewayTest,EmbeddingServiceTest,ResourceGenerationControllerTest,RagQueryServiceTest,IndexServiceEmbeddingVectorTest,ChunkServiceVectorRetrievalTest test` | PASS | BUILD SUCCESS；Tests run: 53, Failures: 0, Errors: 0, Skipped: 0 |
| `mvn dependency:tree` | PASS | BUILD SUCCESS；确认新增 Spring AI OpenAI starter 及传递依赖。 |
| `mvn compile` | PASS | BUILD SUCCESS。 |
| `mvn test` | PASS | BUILD SUCCESS；Tests run: 357, Failures: 0, Errors: 0, Skipped: 1 |

## 6. 静态边界检查

```powershell
rg -n "ChatClient|ChatModel|EmbeddingModel|OpenAi|DashScope|springframework\.ai" backend\src\main\java
```

结果：

- 仅 `AiModelGateway` 使用 Spring AI chat API。
- 仅 `EmbeddingService` 使用 Spring AI embedding API。
- 业务 Controller / ResourceGenerationService / RagQueryService / VectorIndexAdapter 未直接调用模型 SDK。

## 7. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Spring AI SDK 只封装在 `AiModelGateway` / `EmbeddingService` 内。 |
| Frontend rules | PASS | 未修改 frontend；无前端 LLM/API key 暴露。 |
| Agent / RAG rules | PASS | ResourceAgent 仍走 gateway；结构化输出继续校验；embedding 仍走 RAG service 边界。 |
| Security | PASS | 未写入真实 secret；raw provider error 映射为固定安全错误码。 |
| API / Database | PASS | 未修改 Controller/API/DB migration。 |

## 8. 已知限制

- 真实外部 provider smoke 未纳入默认测试；需要运行时提供环境变量并在受控环境手动验证。
- DashScope / Spring AI Alibaba 不在本切片，后续如接入需单独依赖审查。
- VectorDB 不在本切片；当前只是让 embedding adapter 能产生真实向量并通过现有 noop vector boundary。
- OpenAI-compatible provider 的 `base-url` / `api-key` 仅允许通过环境变量注入，不应写入仓库文件或 memory/evidence。

## 9. 运行配置提示

如需用用户指定的第一个模型，可在运行环境设置：

```powershell
$env:AI_MODEL_PROVIDER = "openai"
$env:SPRING_AI_MODEL_CHAT = "openai"
$env:SPRING_AI_MODEL_EMBEDDING = "openai"
$env:AI_CHAT_MODEL = "gpt-5.5"
$env:AI_EMBEDDING_MODEL = "<embedding-model-name>"
$env:OPENAI_API_KEY = "<runtime-secret>"
$env:OPENAI_BASE_URL = "<provider-base-url>"
```

注意：不要把真实 key 或真实 base URL 写入仓库文件、文档、memory、evidence 或日志。
