# 模型调用 Prompt 元数据证据

## 1. 代码变更

- `ModelCallLog` 增加 `promptCode`、`promptVersion`、`temperature`、`structuredOutputSchema`。
- `AgentRunRecorder` 在成功模型调用日志中写入 prompt metadata。
- `AgentRunRecorder` 在失败模型调用日志中写入 prompt metadata。
- `AgentRunRecorder` 将 provider 原始错误脱敏为 `MODEL_PROVIDER_ERROR`，避免进入 `model_call_log`、`agent_task.outputJson` 和 `agent_trace.summary`。
- 新增 `V12__model_call_prompt_metadata.sql`。
- `ResourceGenerationControllerTest` 增加资源生成集成路径的模型日志元数据断言。

## 2. TDD 证据

RED：

```powershell
cd backend
mvn "-Dtest=AgentRunRecorderTest,SchemaConvergenceMigrationTest" test
```

结果：失败符合预期，`ModelCallLog` 缺少 `getPromptCode/getPromptVersion/getTemperature/getStructuredOutputSchema`。

GREEN：

```powershell
cd backend
mvn "-Dtest=AgentRunRecorderTest,SchemaConvergenceMigrationTest" test
```

结果：15 个测试通过，0 失败，0 错误。

## 3. 回归验证

```powershell
cd backend
mvn "-Dtest=AgentRunRecorderTest,ResourceGenerationControllerTest,SchemaConvergenceMigrationTest,PromptVersionServiceTest,PromptVersionControllerTest,AiModelGatewayTest" test
```

结果：39 个测试通过，0 失败，0 错误。

## 4. 安全证据

- 成功日志只保存 prompt code/version、temperature 和白名单 schema 摘要。
- 失败日志断言 `errorMessage = MODEL_PROVIDER_ERROR`。
- 失败任务输出和 trace summary 断言不包含原始 `provider unavailable` / `provider timeout`。
- 本轮未新增直接暴露 `model_call_log` 明细的 API。

## 5. 剩余风险

- PromptVersion API 仍返回 `promptText`，后续需要接入真实 RBAC 或按角色隐藏 promptText。
- `recordRuntimeFailure(...)` 当前不写 `model_call_log`，本轮不处理。
- 未运行完整 `mvn test`。
