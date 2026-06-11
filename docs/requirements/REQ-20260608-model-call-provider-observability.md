# REQ-20260608 模型调用 provider 持久化观测

| ID | Requirement | Priority |
|---|---|---|
| REQ-P3-3-B-1 | `model_call_log` 必须新增 `provider` 字段，用于持久化模型 provider 观测。 | P0 |
| REQ-P3-3-B-2 | `provider` 字段必须兼容历史数据，迁移后历史行默认值为 `none`。 | P0 |
| REQ-P3-3-B-3 | `ModelCallLog` entity 必须与 V18 schema 保持一致，默认 provider 不得为 null。 | P0 |
| REQ-P3-3-B-4 | 成功模型调用日志必须从 `AiModelGateway.ModelResponse.provider()` 写入 provider，而不是从 model name 或 trace 占位值推断。 | P0 |
| REQ-P3-3-B-5 | 失败模型调用日志必须写入安全 provider，且不得从 raw exception message 解析 provider。 | P0 |
| REQ-P3-3-B-6 | provider 值必须归一化为低基数白名单：`none`、`openai`、`dashscope`、`anthropic`、`gemini`、`mock`、`other`。 | P0 |
| REQ-P3-3-B-7 | provider 配置为 URL、tenant、deployment path、`apiKey`、`sk-` 等敏感或高基数字符串时，DB / metrics 只能记录 `other`。 | P0 |
| REQ-P3-3-B-8 | 失败路径 `errorMessage` 仍只能保存 `MODEL_PROVIDER_ERROR` 或 `STRUCTURED_OUTPUT_INVALID` 等安全错误码。 | P0 |
| REQ-P3-3-B-9 | 本切片不新增依赖、不接真实 provider、不修改前端、不新增 API。 | P0 |

## 用户流

1. ResourceAgent 通过 `AiModelGateway` 请求模型。
2. gateway 生成或接收 `ModelResponse.provider`。
3. `AgentRunRecorder` 写入 `model_call_log.provider`。
4. 后续 analytics / ops 可从持久化日志按 provider 聚合。

## 边界情况

- provider 为空或 blank：记录 `none`。
- provider 为大小写混合：转为小写白名单值。
- provider 为未知名称或敏感字符串：记录 `other`。
- provider failure：不保存 raw exception，不保存 provider error 原文。
- 结构化输出失败：provider 仍可记录，错误码为 `STRUCTURED_OUTPUT_INVALID`。

