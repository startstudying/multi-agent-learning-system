# EVIDENCE - P3-3-A 模型网关结构化校验与日志补齐

## 1. 变更概述

本切片完成 `docs/planning/backend-architecture-todolist.md` 中 P3-3 的最小边界硬化：

- 资源生成模型请求显式传入 `agent-resource-v1` prompt version。
- `AiModelGateway` 对 `agent-resource-v1` structured output 校验 `resources[]` 及资源必填字段。
- provider 失败与 schema 失败只向 task / trace / model log 暴露安全错误码：`MODEL_PROVIDER_ERROR` 或 `STRUCTURED_OUTPUT_INVALID`。
- 成功模型证据中的 `model_call_log.model`、`latencyMs`、token usage、cost 改为来自 `AiModelGateway.ModelResponse`。

未新增依赖、未修改数据库 schema、未修改 frontend、未接入真实 Spring AI provider。

## 2. 关键实现证据

- `AiModelGateway`：
  - `ModelRequest` 增加 `promptVersion`，`ModelResponse` 增加 `latencyMs`。
  - 当 `promptVersion == agent-resource-v1` 时校验 `resources` 必须是非空数组。
  - 每个 resource item 必须包含非空 `title`、`type`、`modality`、`markdownContent`、`citationSummary`、`safetyStatus`。
  - `safetyStatus` 只允许 `APPROVED`、`NEEDS_REVIEW`、`BLOCKED`。
  - retry 耗尽后只抛出安全错误码；`ModelCallFailedException` 不保留 raw provider cause。
- `AgentRunRecorder`：
  - 新增基于 `AiModelGateway.ModelResponse` 的成功模型证据记录入口。
  - 成功 `model_call_log` 和 `token_usage_log` 使用 gateway response 的 model、latency、token、cost。
  - `sanitizeModelError(...)` 保留 `STRUCTURED_OUTPUT_INVALID`，其他错误统一为 `MODEL_PROVIDER_ERROR`。
- `ResourceGenerationService`：
  - ResourceAgent 请求传入 `AgentRuntimeConstants.PROMPT_RESOURCE_V1`。
  - 成功模型证据绑定 `step_resource` trace，并传入完整 gateway `modelResponse`。
- `AgentRuntimeConstants`：
  - 新增 `MODEL_STRUCTURED_OUTPUT_INVALID = "STRUCTURED_OUTPUT_INVALID"`。

## 3. 安全与边界证据

### 3.1 原始 provider error / secret 不进入失败证据

测试覆盖：

- `AiModelGatewayTest.recordsFailureWhenStructuredGenerationFailsTwice`
- `AiModelGatewayTest.sanitizesProviderFailureBeforeThrowingAndRecordingFailure`
- `ResourceGenerationControllerTest` malformed structured output / provider failure 持久化断言

关键断言包括：

- `ModelCallFailedException.getMessage() == "MODEL_PROVIDER_ERROR"` 或 `"STRUCTURED_OUTPUT_INVALID"`。
- `ModelCallFailedException.getCause() == null`。
- recorder 参数只接收安全错误码。
- `agent_task.outputJson`、`agent_trace.summary`、`model_call_log.errorMessage` 不包含 raw prompt、student answer、RAG chunk、secret、raw provider message。

### 3.2 业务代码不直接接入模型 SDK

```powershell
rg -n "ChatClient|ChatModel|EmbeddingModel|OpenAi|DashScope|springframework\.ai" backend\src\main\java
```

结果：

- 无匹配（命令 exit 1 表示 `NO_MATCHES`）。

### 3.3 未新增依赖 / DB schema / frontend 变更

本切片未修改：

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`

`model_call_log` 当前没有 `provider` 字段，因此 provider 仍只在 gateway response / metrics 层覆盖；DB provider 持久化留给后续 P3-3-B 或独立 schema 切片。

## 4. 测试命令

### 4.1 聚焦测试

```powershell
cd backend
mvn --% -Dtest=AiModelGatewayTest test
```

结果：

- BUILD SUCCESS
- Tests run: 8, Failures: 0, Errors: 0, Skipped: 0

### 4.2 集成测试

```powershell
cd backend
mvn --% -Dtest=AgentRunRecorderTest,ResourceGenerationControllerTest test
```

结果：

- BUILD SUCCESS
- Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
- 备注：Surefire 输出过一次 `Unable to create temporary directory ... dumpstream` 警告，但最终 Maven exit 0 且测试结果为 0 failure / 0 error。

### 4.3 相邻回归

```powershell
cd backend
mvn --% -Dtest=AiModelGatewayTest,AgentRunRecorderTest,ResourceGenerationControllerTest,OrchestratorWorkflowControllerTest,AgentTraceControllerTest,AnalyticsControllerTest test
```

结果：

- BUILD SUCCESS
- Tests run: 75, Failures: 0, Errors: 0, Skipped: 0

### 4.4 完整后端测试

```powershell
cd backend
mvn test
```

结果：

- BUILD SUCCESS
- Tests run: 275, Failures: 0, Errors: 0, Skipped: 1

## 5. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | `ResourceGenerationService` 调用 `AiModelGateway`，Controller 不接触模型 SDK |
| Frontend rules | PASS | 未修改 frontend，frontend 不接触模型 API |
| Agent / RAG rules | PASS | 资源生成模型输出在 gateway 边界校验；失败写入 Agent Trace / Model Call Log |
| Security | PASS | provider/schema 失败只暴露安全错误码；不保存 raw provider error、prompt、student answer、RAG chunk、secret |
| API / Database | PASS | 未修改 API path / response contract；未新增 migration |

## 6. 验证限制

- 本切片不接真实 Spring AI / Spring AI Alibaba provider。
- 本切片不实现 embedding provider、VectorDB adapter、hybrid retrieval、reranker fallback。
- `model_call_log` 无 provider 字段，provider DB 持久化未完成。
- 子代理代码审查尝试因当前线程代理数量达到上限未能启动；已通过本轮人工核对、聚焦测试、相邻回归、完整测试和静态边界检查补足验证证据。
