# PRD-20260608 模型网关结构化校验与日志补齐

## 背景

P3-3 要求所有模型调用经过统一网关、结构化输出必须校验、模型调用日志需要可审计。当前资源生成已通过 `AiModelGateway`，但网关输出仍是占位结构，成功 `model_call_log` 仍从 trace 占位值取 `model` 和 `latencyMs`，失败异常 message 也可能携带 raw provider error。

## 目标

在不接真实 provider、不新增依赖、不改 DB schema、不改 frontend 的前提下，完成最小模型边界硬化：

- 资源生成模型请求显式绑定 `agent-resource-v1`。
- `AiModelGateway` 对 `ResourceGenerationOutputSchema` 做轻量字段校验。
- 缺字段或类型错误触发 retry；最终失败只保留安全错误码。
- 成功模型日志使用 gateway response 的 `model`、`latencyMs`、token/cost。

## 非目标

- 不接入 Spring AI / Spring AI Alibaba 真实 provider。
- 不新增 `model_call_log.provider` 字段。
- 不消费模型输出生成真实资源正文；当前资源草稿仍保持 deterministic draft 与 Review Gate。
- 不改前端展示与 API contract。

## 成功标准

- 缺 `resources` 或资源必填字段缺失的 structured output 不能被当作成功模型调用。
- raw provider error / prompt / student answer / RAG chunk / secret 不进入响应、trace、task output、model_call_log。
- 成功 `model_call_log` 反映 gateway model 与 latency。
- 聚焦、相邻回归和完整后端测试可运行并形成 Evidence。
