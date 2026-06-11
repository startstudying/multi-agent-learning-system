# REQ-20260608 模型网关结构化校验与日志补齐

| ID | Requirement | Priority |
|---|---|---|
| REQ-P3-3-A-1 | 资源生成模型请求必须通过 `AiModelGateway`，并携带 `agent-resource-v1` prompt version。 | P0 |
| REQ-P3-3-A-2 | `AiModelGateway` 必须校验资源生成结构化输出包含非空 `resources[]`。 | P0 |
| REQ-P3-3-A-3 | 每个 resource item 必须包含 `title`、`type`、`modality`、`markdownContent`、`citationSummary`、`safetyStatus`。 | P0 |
| REQ-P3-3-A-4 | 结构化输出缺字段、字段为空或 `safetyStatus` 非法时必须触发 retry；重试耗尽后写入失败 task / trace / model_call_log。 | P0 |
| REQ-P3-3-A-5 | provider raw error、raw prompt、student answer、RAG chunk、secret 不得进入 HTTP 响应、`agent_task.outputJson`、`agent_trace.summary`、`model_call_log.errorMessage`。 | P0 |
| REQ-P3-3-A-6 | `ModelCallFailedException.getMessage()` 必须只包含安全错误码，不包含 raw provider error。 | P0 |
| REQ-P3-3-A-7 | 成功 `model_call_log.model`、`latencyMs`、`estimatedCost` 和 token usage 必须来自 gateway response / token usage，而不是 trace 占位值。 | P0 |
| REQ-P3-3-A-8 | 本切片不新增依赖、不改 DB schema、不改 frontend、不接真实 provider。 | P0 |
