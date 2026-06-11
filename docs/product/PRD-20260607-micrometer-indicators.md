# PRD - Micrometer 运行指标
状态：已完成（2026-06-07）

## 1. 背景

P3-5 可观测性已经完成结构化请求日志，HTTP 请求结束时可以看到 `traceId/userId/route/status/latencyMs/errorCode`。但这些信息仍停留在日志层，无法通过 Micrometer/Actuator 形成可聚合的运行时指标，也无法稳定支撑延迟趋势、失败率和 token 成本趋势观察。

后端已经引入 `spring-boot-starter-actuator`，但当前没有业务 `MeterRegistry` 埋点，`application.yml` 也只暴露 `health,info`。本切片补齐最小可交付的业务指标层，不新增依赖、不修改业务 API、不修改数据库。

## 2. 目标

- 为 HTTP 请求补齐 Micrometer 请求延迟和失败计数。
- 为 RAG 查询补齐真实查询延迟、查询结果计数、检索数量和失败计数。
- 为模型网关补齐模型调用延迟和失败计数。
- 为 token 使用补齐 prompt/completion/total token 与估算成本指标。
- 暴露 `/actuator/metrics` 诊断入口，便于本地和运维环境确认 meter 已注册。
- 所有指标 tags 只使用低基数、非敏感字段。

## 3. 范围

纳入：

- 新增内部 metrics 适配层，集中维护 meter 名称和 tag 白名单。
- 在 `StructuredRequestLoggingFilter` 记录 HTTP request timer/failure counter。
- 在 `RagQueryService` 记录 RAG fresh query、replay、no-source 和 failure 指标。
- 在 `AiModelGateway` 记录模型调用真实网关耗时和失败指标。
- 在 `AgentRunRecorder` 记录 token 使用量和 token cost 指标。
- 将 Actuator web exposure 增加 `metrics`。
- 补充单元/集成测试、证据、验收、变更日志和项目记忆。

不纳入：

- Prometheus registry、OTLP exporter、Grafana dashboard。
- 告警规则和告警阈值。
- 深度健康检查。
- API 合同、数据库 schema、权限模型变更。
- 真实模型 provider SLA 解释；当前模型网关仍可能是 deterministic/placeholder。

## 4. 用户价值

- 运维人员可以通过 `/actuator/metrics` 看到请求、RAG、模型和 token 指标是否在运行时变化。
- 后端开发可以用低成本指标判断“慢在哪里”：HTTP 层、RAG 检索层还是模型调用层。
- 管理端后续仪表盘可以把 Micrometer 用作实时趋势层，把现有 `agent_trace/model_call_log/token_usage_log/kb_query_log` 用作审计事实层。
- 安全审计可以确认指标系统不会把学习内容、Prompt、用户身份或 trace 级上下文写入 tags。

## 5. 成功标准

- 后端存在集中式 metrics 适配层，业务类不散落 meter name/tag 策略。
- HTTP 成功/失败请求会写入 `learningos.http.server.requests`，失败请求会写入 `learningos.http.server.failures`。
- RAG fresh query 会写入 `learningos.rag.query.duration` 和 `learningos.rag.query.count`；replay 只计数，不污染真实查询延迟。
- RAG no-source、permission/safety/conflict failure 能通过低基数 tags 区分。
- 模型网关成功/失败调用会写入 `learningos.model.call.duration` 和失败计数。
- token prompt/completion/total 与 estimated cost 会写入 DistributionSummary。
- 指标 tags 不包含 `traceId/userId/requestId/agentTaskId/kbId/question/prompt/source/errorMessage`。
- 定向测试和全量 Maven 测试通过，或在 Evidence 中明确环境限制。
