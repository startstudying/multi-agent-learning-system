# Model Gateway Boundary

## 使用场景

当实现或审查模型调用入口、Spring AI provider 接入、structured output、`model_call_log`、`token_usage_log`、Agent Trace 模型证据、provider 观测或 provider 错误脱敏时使用。

## 核心规则

1. 业务服务只能通过 `AiModelGateway` 发起模型调用，不直接调用 provider SDK。
2. 模型请求必须携带明确 `promptVersion`；由 prompt version 决定 schema 校验与日志 metadata。
3. structured output 必须先在 gateway 边界校验，再进入业务持久化。
4. provider 失败、schema 失败、解析失败只能向 task / trace / model log 暴露安全错误码。
5. `ModelCallFailedException` 不能携带 raw provider exception cause。
6. 成功模型日志的 provider、model、latency、token、cost 必须来自 gateway response，不能从 trace 占位值或 model name 推断。
7. `model_call_log.provider` 和 metrics tag 只能使用低基数白名单：`none`、`openai`、`dashscope`、`anthropic`、`gemini`、`mock`、`other`。
8. provider 为 null / blank 时写 `none`；未知 provider、URL、endpoint path、tenant、region、account id、`apiKey`、`sk-` 或 token-like 字符串统一写 `other`。
9. `model_call_log.errorMessage` 只能保存安全错误码，例如 `MODEL_PROVIDER_ERROR`、`STRUCTURED_OUTPUT_INVALID`；不得保存 provider 原文、raw requestId 或 raw exception。
10. 新增 provider SDK、Embedding provider、VectorDB adapter、日志 schema 字段或 provider 聚合索引前，必须走 dependency / schema / security review。

## ResourceAgent schema 参考

`agent-resource-v1` structured output 至少包含：

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

校验要求：

- `resources` 是非空数组。
- item 是对象。
- 必填字段是非空字符串。
- `safetyStatus` 只能是白名单枚举。

## 日志模式

- 成功：
  - `model_call_log.provider`、`model_call_log.model`、`latencyMs`、`estimatedCost` 和 `token_usage_log` 来自 `AiModelGateway.ModelResponse`。
  - provider 必须经过低基数归一化。
- 失败：
  - provider 失败：`MODEL_PROVIDER_ERROR`
  - schema 失败：`STRUCTURED_OUTPUT_INVALID`
  - 不落 raw prompt、student answer、RAG chunk、raw provider message、secret、malformed output。

## 测试建议

- gateway 单测覆盖 retry 成功、retry 失败、缺 `resources`、缺 item 字段、非法枚举、raw provider error 脱敏、`latencyMs` 暴露。
- gateway 单测覆盖 provider 归一化：blank -> `none`，白名单大小写 -> 小写白名单，URL / `apiKey` / `sk-` -> `other`。
- recorder 单测覆盖成功日志事实源来自 gateway response，并持久化 provider。
- recorder 单测覆盖失败 provider overload 和旧签名兼容。
- controller / integration 测试覆盖失败 evidence 不落资源/review，成功 evidence 持久化 provider/model/token/cost。
- migration 测试覆盖 `model_call_log.provider varchar(80) not null default 'none'`。
- 静态边界检查：

```powershell
rg -n "ChatClient|ChatModel|EmbeddingModel|OpenAi|DashScope|springframework\.ai" backend\src\main\java
```

除 gateway/provider adapter 切片明确允许的文件外，不应出现业务服务直连 provider SDK。

## 反模式

- 在 Service 中直接调用 `ChatClient` 或 provider SDK。
- 把 raw provider exception 作为 `ModelCallFailedException` cause 继续抛出。
- 用 trace 里的占位 `model` / `latencyMs` 推断成功模型日志。
- 用 model name、endpoint、deployment path 或 exception message 推断 provider。
- schema 校验失败时保存原始模型输出到 task output、trace summary 或 `model_call_log.errorMessage`。
- 为了 provider 字段临时把 JSON 塞进 `errorMessage`，而不是走正式 schema 变更。
- 把 provider base URL、tenant、region、API key、`sk-` token 或 raw requestId 写入 DB、metrics tag、日志或文档记忆。
