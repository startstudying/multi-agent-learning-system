# RUN - Micrometer 运行指标子代理集成报告

## 1. Backend Expert 摘要

结论：

- `backend/pom.xml` 已有 `spring-boot-starter-actuator`，无需新增 Micrometer 依赖。
- `backend/src/main/resources/application.yml` 当前只暴露 `health,info`，如需通过 HTTP 诊断 meter，需要加入 `metrics`。
- 请求指标适合落在 `StructuredRequestLoggingFilter`，复用现有 route/status/errorCode/latency 计算。
- RAG 指标适合落在 `RagQueryService`，replay 必须与真实查询延迟分开。
- token 和模型事实当前由 `AiModelGateway` 与 `AgentRunRecorder` 共同承担，不能把 Analytics 查询作为 metrics 触发点。
- 推荐新增轻量 metrics adapter，避免 meter name/tag 逻辑散落。

## 2. Agent/RAG Expert 摘要

结论：

- Micrometer 是运行时趋势层，不替代 `kb_query_log`、`model_call_log`、`token_usage_log`、`agent_task/agent_trace`。
- RAG 指标应区分 `success/no_source/replay/error`。
- 模型 latency 应以 `AiModelGateway` 网关边界真实耗时为准，不应使用当前固定 trace step latency 推断。
- token/cost 指标应保留数值趋势，不按 user/course/task 维度打 tags。
- 允许 tags 必须低基数，禁止 `traceId/userId/requestId/agentTaskId/question/prompt/source/errorMessage`。

## 3. Security & Quality 状态

尝试启动 Security & Quality 子代理失败：

```text
agent thread limit reached
```

补偿措施：

- Main Codex 本地执行安全检查。
- SPEC 中写入 tag 白名单和禁止字段。
- 测试中遍历 meter tag keys，确认敏感/高基数字段没有进入 tags。
- Actuator 只新增 `metrics` exposure，不开放 `env/beans/heapdump/threaddump/loggers`。

## 4. 冲突与决策

| 议题 | Backend Expert | Agent/RAG Expert | 最终决策 |
|---|---|---|---|
| 模型 latency 来源 | `AgentRunRecorder` 为主，`AiModelGateway` 为辅 | `AiModelGateway` 为真实模型调用边界 | latency 放在 `AiModelGateway`；token/cost 放在 `AgentRunRecorder` |
| replay 是否计 latency | 独立 tag 或不纳入 latency | 必须区分 replay，避免污染检索性能 | replay 只计数，不计 duration |
| 是否新增 adapter | 建议新增 | 可接受 | 新增 `LearningOsMetrics` |
| 是否暴露 metrics endpoint | 需要验收决策 | 未强制 | 默认加入 `health,info,metrics`，但不开放高风险 endpoint |

## 5. 最终实施边界

- `common/observability`：集中 metrics adapter。
- `common/trace`：HTTP 请求指标。
- `rag/application`：RAG 查询指标。
- `agent/application/AiModelGateway`：模型调用耗时和失败指标。
- `agent/application/AgentRunRecorder`：token usage 和 estimated cost 指标。
- `application.yml`：新增 `metrics` Actuator exposure。

