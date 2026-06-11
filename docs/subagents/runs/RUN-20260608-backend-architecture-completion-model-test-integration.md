# RUN-20260608 P3-3-A 测试与集成专家报告

## 结论

P3-3-A 的测试应先覆盖三个行为：

1. `AiModelGateway` 拒绝缺 `resources` 或资源必填字段缺失的结构化输出。
2. provider 失败时 gateway 对外 message 与 recorder 参数都只包含安全错误码。
3. 成功 `model_call_log` 使用 gateway response 的 `model`、`latencyMs` 和 token/cost。

## RED 用例建议

### `AiModelGatewayTest`

- `rejectsStructuredResourceOutputWhenRequiredResourcesFieldIsMissing`
- `rejectsStructuredResourceOutputWhenResourceItemMissesRequiredFields`
- `sanitizesProviderFailureBeforeThrowingAndRecordingFailure`

预期当前失败点：

- 当前没有结构化 schema 校验，缺字段会直接成功。
- 当前异常 message 和 recorder 参数包含 raw provider error。

### `AgentRunRecorderTest`

- `recordsSuccessfulModelEvidenceFromGatewayResponseInsteadOfFirstTrace`

预期当前失败点：

- 当前方法签名没有 gateway response 入参。
- `model_call_log.model` 与 `latencyMs` 来自 first trace，而不是 gateway response。

### `ResourceGenerationControllerTest`

- `persistsGatewayModelLatencyAndTokenUsageForSuccessfulResourceGeneration`
- `persistsSafeRecoverableFailureWhenStructuredOutputIsMissingRequiredFields`

## 推荐验证命令

```powershell
cd backend
mvn --% -Dtest=AiModelGatewayTest test
mvn --% -Dtest=AgentRunRecorderTest,ResourceGenerationControllerTest test
mvn --% -Dtest=AiModelGatewayTest,AgentRunRecorderTest,ResourceGenerationControllerTest,OrchestratorWorkflowControllerTest,AgentTraceControllerTest,AnalyticsControllerTest test
mvn test
```
