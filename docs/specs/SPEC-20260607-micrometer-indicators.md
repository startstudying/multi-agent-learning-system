# SPEC - Micrometer 运行指标
状态：已完成（2026-06-07）

## 1. 目标

在不新增依赖、不修改业务 API、不修改数据库的前提下，为后端关键链路补齐 Micrometer 业务指标：请求延迟、RAG 延迟、模型延迟、失败率、token 成本。

## 2. 追踪

- PRD：`docs/product/PRD-20260607-micrometer-indicators.md`
- REQ：`docs/requirements/REQ-20260607-micrometer-indicators.md`
- TODO：`docs/planning/backend-architecture-todolist.md` P3-5
- 架构基线：`docs/architecture/ARCHITECTURE_BASELINE.md`
- 子代理报告：`docs/subagents/runs/RUN-20260607-micrometer-indicators.md`

## 3. 设计

### 3.1 Metrics 适配层

新增 `LearningOsMetrics`：

```text
backend/src/main/java/com/learningos/common/observability/LearningOsMetrics.java
```

职责：

- 统一封装 `MeterRegistry`、`Timer`、`Counter`、`DistributionSummary` 调用。
- 统一 meter name。
- 统一 tag 白名单和安全归一化。
- 对缺失 `MeterRegistry` 的手工单测路径提供 no-op fallback。

### 3.2 HTTP 指标

埋点位置：

```text
StructuredRequestLoggingFilter#doFilterInternal finally
```

指标：

| Meter | Type | Tags |
|---|---|---|
| `learningos.http.server.requests` | Timer | `method`, `route`, `status`, `error_code` |
| `learningos.http.server.failures` | Counter | `method`, `route`, `status`, `error_code` |

说明：

- 复用现有 `route/status/errorCode/latencyMs` 计算。
- `traceId` 和 `userId` 继续只进结构化日志，不进入 metrics tags。

### 3.3 RAG 指标

埋点位置：

```text
RagQueryService
```

指标：

| Meter | Type | Tags |
|---|---|---|
| `learningos.rag.query.duration` | Timer | `strategy`, `outcome`, `no_source`, `replayed`, `error_code` |
| `learningos.rag.query.count` | Counter | `strategy`, `outcome`, `no_source`, `replayed`, `error_code` |
| `learningos.rag.retrieval.count` | DistributionSummary | `strategy`, `no_source` |
| `learningos.rag.citation.count` | DistributionSummary | `strategy`, `no_source` |
| `learningos.rag.query.failures` | Counter | `strategy`, `outcome`, `no_source`, `replayed`, `error_code` |

语义：

- fresh query 成功：记录 duration、count、retrieval summary、citation summary。
- no-source：`outcome=no_source`，`no_source=true`。
- replay：只记录 `query.count`，`replayed=true`，不记录 duration。
- failure：记录 `outcome=error` 和稳定 `error_code`；不记录原始异常消息。

### 3.4 模型指标

埋点位置：

```text
AiModelGateway#generateStructuredWithRetry
```

指标：

| Meter | Type | Tags |
|---|---|---|
| `learningos.model.call.duration` | Timer | `agent_name`, `provider`, `model`, `status` |
| `learningos.model.call.failures` | Counter | `agent_name`, `provider`, `model`, `error_code` |

说明：

- 使用网关真实执行耗时，不使用当前 `AgentTrace` 固定步骤耗时推断模型 latency。
- 当前 provider 可能仍是 deterministic/placeholder，指标仅代表网关边界耗时，不承诺外部 provider SLA。

### 3.5 Token / Cost 指标

埋点位置：

```text
AgentRunRecorder#recordSuccessfulModelEvidence
```

指标：

| Meter | Type | Tags |
|---|---|---|
| `learningos.token.usage` | DistributionSummary | `model`, `prompt_code`, `token_type` |
| `learningos.token.cost` | DistributionSummary | `model`, `prompt_code`, `currency` |

说明：

- token/cost 指标以写入 `token_usage_log` 的事实为准。
- 不按 user/course/task/trace 维度打 tags。

## 4. Actuator 暴露

`backend/src/main/resources/application.yml`：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

安全限制：

- 本切片只新增 `metrics`。
- 不暴露 `env`、`beans`、`heapdump`、`threaddump`、`loggers`。
- 后续生产部署必须通过网关、防火墙或 Spring Security 收口 actuator 访问。

## 5. Tag 白名单

允许：

- HTTP：`method`、`route`、`status`、`error_code`
- RAG：`strategy`、`outcome`、`no_source`、`replayed`、`error_code`
- Model：`agent_name`、`provider`、`model`、`status`、`error_code`
- Token：`model`、`prompt_code`、`token_type`、`currency`

禁止：

- `traceId`、`userId`、`requestId`、`agentTaskId`、`kbId`、`documentId`、`resourceId`
- `question`、`prompt`、`answer`、`excerpt`、`sourcesJson`、`responseJson`
- 原始 `errorMessage`、异常 message、provider 原始错误

## 6. 官方文档依据

- Micrometer `Timer` 用于记录耗时；非时间数值使用 `DistributionSummary`。
- Micrometer `Counter` 适合记录单调递增事件，Timer/Summary 自带 count，但独立 failure counter 便于失败率告警。
- Spring Boot Actuator `metrics` endpoint 默认不暴露，需要通过 `management.endpoints.web.exposure.include` 明确暴露；暴露 endpoint 前需要评估敏感信息和访问控制。

## 7. 测试策略

- `StructuredRequestLoggingFilterTest`
  - 断言 HTTP timer 和 failure counter 写入。
  - 断言 tags 不包含 `traceId/userId/query`。
- `RagQueryServiceTest`
  - 断言 success/no-source/replay/failure 指标。
  - 断言 replay 不增加 duration count。
- `AiModelGatewayTest`
  - 断言成功和双次失败都记录模型调用 duration。
  - 断言失败 counter 使用稳定 `error_code=MODEL_PROVIDER_ERROR`。
- `AgentRunRecorderTest`
  - 断言 token prompt/completion/total 和 cost summary。
  - 断言 tags 不包含 `traceId/agentTaskId/userId`。

## 8. 非目标

- 不实现 Prometheus scrape endpoint。
- 不实现 Grafana dashboard。
- 不实现告警阈值。
- 不实现深度健康检查。
- 不改变持久化审计事实表。
