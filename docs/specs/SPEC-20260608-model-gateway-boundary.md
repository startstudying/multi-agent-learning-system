# SPEC-20260608 模型网关结构化校验与日志补齐

## 范围

本切片只强化 `AiModelGateway` 与资源生成模型日志闭环，不改变外部 API 和数据库结构。

## 模型请求

`AiModelGateway.ModelRequest` 增加可选 `promptVersion`。资源生成调用必须传入：

```java
AgentRuntimeConstants.PROMPT_RESOURCE_V1
```

## 结构化输出 schema

当 `promptVersion == agent-resource-v1` 时，`structuredOutput` 必须满足：

```json
{
  "resources": [
    {
      "title": "...",
      "type": "...",
      "modality": "...",
      "markdownContent": "...",
      "citationSummary": "...",
      "safetyStatus": "APPROVED|NEEDS_REVIEW|BLOCKED"
    }
  ]
}
```

校验规则：

- `resources` 必须是非空数组。
- 每个 item 必须是对象。
- 必填字段必须是非空字符串。
- `safetyStatus` 只允许 `APPROVED`、`NEEDS_REVIEW`、`BLOCKED`。

## 失败语义

- provider 失败：安全错误码为 `MODEL_PROVIDER_ERROR`。
- 结构化输出非法：安全错误码为 `STRUCTURED_OUTPUT_INVALID`。
- gateway 重试次数沿用现有 2 次。
- HTTP 响应仍由统一异常处理返回 `INTERNAL_ERROR`。
- `resource_generation_task.lastError` 沿用 `MODEL_CALL_FAILED`，避免 API contract 变化。

## 日志语义

成功模型证据：

- `model_call_log.model` 使用 `ModelResponse.model`。
- `model_call_log.latencyMs` 使用 `ModelResponse.latencyMs`，缺失时使用 gateway 实测耗时兜底。
- token usage 使用 `ModelResponse.tokenUsage`。
- `promptCode` / `promptVersion` / `structuredOutputSchema` 继续由 prompt metadata 白名单生成。

失败模型证据：

- `agent_task.outputJson`、`agent_trace.summary`、`model_call_log.errorMessage` 只能出现安全错误码。
- 不保存 raw provider error、raw prompt、student answer、RAG chunk、secret 或 malformed structured output。

## 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Service 调用 gateway；Controller 不接触模型 SDK |
| Frontend rules | PASS | 不改 frontend |
| Agent / RAG rules | PASS | 模型输出在 gateway 边界校验 |
| Security | PASS | 错误码脱敏，禁止 raw provider error 落库 |
| API / Database | PASS | 不改 API path，不改 schema |
