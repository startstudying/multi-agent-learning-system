# RUN-20260608 P3-3-A 集成评审

## 集成决策

采纳三个专家报告的共同结论：本切片只做模型边界硬化，不接真实 provider，不新增依赖，不改 DB schema，不改 frontend。

## 统一范围

### 实现

- `AiModelGateway`
  - `ModelRequest` 增加可选 `promptVersion`。
  - `ModelResponse` 增加 `latencyMs`。
  - 对 `agent-resource-v1` 结构化输出进行轻量字段校验。
  - 校验失败进入 retry；最终失败只暴露安全错误码。
- `AgentRunRecorder`
  - 成功模型调用证据使用 gateway response 的 `model`、`latencyMs`、token/cost。
  - 失败脱敏允许安全错误码，禁止 raw provider error。
- `ResourceGenerationService`
  - 资源生成模型请求传入 `agent-resource-v1`。
  - 成功模型证据绑定 `step_resource` 与 gateway response。

### 测试

- `AiModelGatewayTest`
- `AgentRunRecorderTest`
- `ResourceGenerationControllerTest`
- 相邻回归：`OrchestratorWorkflowControllerTest`、`AgentTraceControllerTest`、`AnalyticsControllerTest`

## 约束

- 不修改 `backend/pom.xml`。
- 不修改 migration。
- 不修改 frontend。
- 不把 provider raw error、prompt、学生答案、RAG chunk、secret 写入响应、trace、task output 或 model_call_log。

## 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 仍由 service 调用 gateway，controller 不接触模型 SDK |
| Frontend rules | PASS | 不改 frontend |
| Agent / RAG rules | PASS | 模型输出校验在 gateway 边界执行 |
| Security | PASS | 失败只暴露安全错误码 |
| API / Database | PASS | 不改 API path，不改 DB schema |
