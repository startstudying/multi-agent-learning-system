# RUN-20260608 P3-3-A 安全与质量专家报告

## 结论

当前失败持久化日志已有二次脱敏，`agent_task.outputJson`、`agent_trace.summary`、`model_call_log.errorMessage` 不直接保存 provider raw error。但 `AiModelGateway` 仍会把 raw provider error 拼进 `ModelCallFailedException` message，并传给 recorder 后再脱敏。该模式依赖下游脱敏，后续日志、AOP 或异常采集一旦记录 exception message，就可能泄露 prompt、学生答案、RAG chunk 或 secret。

## 安全要求

1. gateway 传给 recorder 的错误值必须已经是安全错误码。
2. `ModelCallFailedException.getMessage()` 不得包含 raw provider error。
3. root cause 可以保留在 JVM 内部异常链，但不得进入响应、持久化日志、trace summary 或 model_call_log。
4. 结构化输出校验失败不得保存 malformed output。
5. 本切片不新增依赖，不改 schema，不接真实 provider。

## 建议测试断言

- raw provider error 样本包含：
  - `sk-live-secret`
  - `raw prompt`
  - `student answer`
  - `RAG chunk`
- 断言 HTTP 响应、`agent_task.outputJson`、`agent_trace.summary`、`model_call_log.errorMessage` 均不包含上述敏感片段。
- 结构化输出缺字段时，任务进入 `FAILED` 且只保存安全错误码。

## 风险记录

`model_call_log` 无 provider 字段，因此本切片不能声明 provider 已持久化到 DB。验收口径限定为 gateway response / metrics 层覆盖 provider，DB provider 字段留给后续 schema 切片。
