# EVIDENCE - P3-3-B 模型调用 provider 持久化观测

## 1. 变更概述

本切片完成 `model_call_log.provider` 最小持久化闭环：

- 新增 Flyway V18，为 `model_call_log` 增加 `provider varchar(80) not null default 'none'`。
- `ModelCallLog` entity 增加 `provider` 字段，并在 `@PrePersist` 对空值兜底为 `none`。
- `AgentRunRecorder` 成功路径从 `AiModelGateway.ModelResponse.provider()` 写入 provider。
- `AgentRunRecorder` 失败路径新增 provider overload；旧签名保留并默认 `none`。
- `AiModelGateway` 将 provider 归一化为低基数安全值，未知、URL、`apiKey`、`sk-` 等高风险字符串归为 `other`。

本切片不接真实 provider、不新增依赖、不修改前端、不新增 API。

## 2. 关键实现证据

- `backend/src/main/resources/db/migration/V18__model_call_provider_observability.sql`
  - 使用现有 `add_column_if_missing` procedure 风格。
  - 对历史数据提供 `NOT NULL DEFAULT 'none'`。
- `backend/src/main/java/com/learningos/agent/domain/ModelCallLog.java`
  - 新增 `@Column(nullable = false, length = 80) private String provider = "none";`。
  - `@PrePersist` 兜底 null / blank provider。
- `backend/src/main/java/com/learningos/agent/application/AgentRunRecorder.java`
  - 成功 `recordSuccessfulModelEvidence(..., ModelResponse)` 写入 `modelResponse.provider()`。
  - 失败 `recordFailure(...)` 保留旧签名，并新增 provider overload。
  - `safeProvider(...)` 只允许 `none/openai/dashscope/anthropic/gemini/mock/other`。
  - `errorMessage` 仍由 `sanitizeModelError(...)` 收敛为安全错误码。
- `backend/src/main/java/com/learningos/agent/application/AiModelGateway.java`
  - gateway 当前 provider 通过 `normalizeProvider()` 统一低基数化。
  - retry 耗尽后 metrics 和 recorder 使用同一 provider 口径。
  - `ModelResponse` 构造器再次兜底 provider，防止测试或未来 adapter 传入高风险值。

## 3. 安全与边界证据

### 3.1 Provider 不保存敏感或高基数字符串

测试覆盖：

- `AiModelGatewayTest.normalizesUnknownOrSensitiveProviderToOther`
- `AgentRunRecorderTest.normalizesSensitiveProviderBeforePersistingSuccessfulModelCall`

断言包括：

- `https://provider.example/v1?apiKey=sk-live-secret` 只记录为 `other`。
- 持久化 provider 不包含 `https://`、`apiKey`、`sk-live-secret`。
- null / blank provider 记录为 `none`。
- 白名单 provider 记录为小写安全值，例如 `openai`。

### 3.2 失败路径不落 raw provider error

测试覆盖：

- `AiModelGatewayTest.recordsFailureWhenStructuredGenerationFailsTwice`
- `AiModelGatewayTest.sanitizesProviderFailureBeforeThrowingAndRecordingFailure`
- `AgentRunRecorderTest.recordsFailureWithProviderAndSanitizedError`
- `ResourceGenerationControllerTest` provider failure / malformed output 断言

断言包括：

- `model_call_log.errorMessage` 只为 `MODEL_PROVIDER_ERROR` 或 `STRUCTURED_OUTPUT_INVALID`。
- `agent_task.outputJson`、`agent_trace.summary`、`model_call_log.errorMessage` 不包含 raw provider message、raw prompt、student answer、RAG chunk 或 `sk-` secret。

### 3.3 业务代码不直接接入模型 SDK

```powershell
rg -n "ChatClient|ChatModel|EmbeddingModel|OpenAi|DashScope|springframework\.ai" backend\src\main\java
```

结果：

- exit code 1，无匹配。

## 4. 测试命令

### 4.1 聚焦与相邻回归

```powershell
cd backend
mvn --% -Dtest=SchemaConvergenceMigrationTest,AgentRunRecorderTest,AiModelGatewayTest,ResourceGenerationControllerTest test
```

结果：

- BUILD SUCCESS
- Tests run: 51, Failures: 0, Errors: 0, Skipped: 0

### 4.2 完整后端测试

```powershell
cd backend
mvn test
```

结果：

- BUILD SUCCESS
- Tests run: 306, Failures: 0, Errors: 0, Skipped: 1

### 4.3 MySQL migration smoke

```powershell
cd backend
mvn --% -Dtest=MysqlMigrationSmokeTest -Dlearningos.mysql.smoke=true test
```

结果：

- BUILD FAILURE
- Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
- 失败原因：本机 `jdbc:mysql://127.0.0.1:3306/learning_os_migration_smoke...` 的 `root` 凭据不可用。
- 关键错误：`Access denied for user 'root'@'localhost' (using password: YES)`。

结论：

- 本轮尚未完成真实 MySQL V18 smoke 验证。
- 失败是环境可达性/凭据问题；当前输出不能证明 V18 migration 存在 MySQL 语法错误。
- `SchemaConvergenceMigrationTest` 和 `MysqlMigrationSmokeTest` 已更新到 V18 覆盖；环境可用后需补跑实库 smoke。

## 5. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 不接触 provider；仍由 Service/Gateway/Recorder 负责模型证据 |
| Frontend rules | PASS | 未修改 frontend，frontend 不接触 LLM API 或 API key |
| Agent / RAG rules | PASS | Agent 模型调用继续写 trace/model log/token log；structured output 校验不变 |
| Security | PASS | provider 低基数归一化；raw provider error 与 secret 不进入 DB/log evidence |
| API / Database | PASS | API contract 不变；V18 schema 已在 SPEC 和 migration 文本中定义 |

## 6. 验证限制

- 真实 MySQL V18 smoke 受本机数据库凭据限制未通过，需要在可控 MySQL 8 环境补跑。
- 本切片不接真实 Spring AI / Spring AI Alibaba Chat provider。
- 本切片不新增 provider analytics API；后续聚合可复用已持久化的 `model_call_log.provider`。
