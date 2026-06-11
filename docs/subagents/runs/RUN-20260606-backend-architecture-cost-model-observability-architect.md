# Subagent Report - Backend Cost / Model / Observability Architect

## Summary

P2-4 已经有后端最小成本治理 API 和测试，但 `docs/planning/backend-architecture-todolist.md` 仍将多项标为未完成，规划状态与代码状态存在漂移。P3-3 和 P3-5 仍主要停留在占位或配置级：模型网关没有真实 Spring AI 调用，健康检查不是深度探活，Micrometer / 结构化日志 / 告警尚未形成后端实现闭环。

## 1. 影响模块

- `agent`：`AiModelGateway`、`AgentRunRecorder`、`model_call_log`、`token_usage_log`、Trace 记录与模型调用边界。
- `analytics`：成本 / Token 聚合、预算决策、高成本任务与异常模型调用识别。
- `health`：数据库、Redis、MinIO、model provider 健康状态暴露。
- `common.trace`：`X-Trace-Id` 注入与请求级 trace context。
- `orchestrator` / `resource generation`：真实模型调用、预算门禁、失败证据最终应在工作流入口或模型网关入口生效。

## 2. 当前已实现证据

### P2-4 成本 / Token 预算治理

- `docs/planning/backend-architecture-todolist.md` P2-4 原先仍保留未完成项。
- `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java` 已提供 `/api/analytics/token-budget/governance`，支持 `from`、`to`、`tokenBudget`、`degradeThreshold`、`manualConfirmationThreshold`、`highCostTokenThreshold`、`highCostUsdThreshold`、`anomalyLatencyMsThreshold`。
- `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java` 已返回 `costStats`、`budgetDecision`、`highCostTaskWarnings`、`abnormalModelCalls`。
- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java` 覆盖聚合、预算超限人工确认、高成本任务和异常模型调用识别。

### P3-3 模型接入边界

- `backend/src/main/java/com/learningos/agent/application/AiModelGateway.java` 当前仍是 provider 占位实现。
- `backend/src/test/java/com/learningos/agent/application/AiModelGatewayTest.java` 验证 configured provider 仍不调用外部服务。
- `backend/pom.xml` 导入 `spring-ai-bom`，但未见真实 chat / embedding starter。
- `ModelCallLog` 与 `TokenUsageLog` 已具备模型、prompt、schema、latency、token、cost 和 error 字段基础。

### P3-5 可观测性与运维

- `backend/src/main/java/com/learningos/common/trace/TraceFilter.java` 负责 `X-Trace-Id` 注入，但未形成结构化日志字段输出。
- `backend/src/main/resources/application.yml` 只暴露 `health,info`，未暴露 `metrics/prometheus`。
- `backend/src/main/java/com/learningos/health/application/HealthService.java` 当前主要返回配置状态，不做 DB / Redis / MinIO / model provider 深度探活。
- `backend/src/main/java/com/learningos/agent/application/AgentTraceGovernanceService.java` 已具备 trace 查询基础。

## 3. 缺口清单

1. P2-4 文档状态漂移：代码已实现最小治理 API，但 TODO 未同步。
2. P2-4 缺少调用前预算门禁：现有预算规则只在 analytics 查询时计算。
3. P2-4 聚合使用 `findAll()` 后内存过滤，生产数据量上来后有性能风险。
4. P3-3 缺少真实 Spring AI / Spring AI Alibaba provider adapter。
5. P3-3 缺少结构化输出 schema 校验与失败降级处理闭环。
6. P3-3 模型日志有字段，但真实 provider、真实 token usage、真实 estimatedCost 仍未闭环。
7. P3-5 缺少结构化日志实现。
8. P3-5 缺少 Micrometer 自定义指标与 Prometheus 暴露。
9. P3-5 健康检查仍是配置检查，不是深度探活。
10. P3-5 告警目前只以 analytics 响应列表形式存在，没有后台规则、事件或外部告警出口。

## 4. 推荐设计边界

- `SPEC-token-budget-governance-enforcement`：区分治理看板 API 与调用前预算门禁，定义 `BudgetDecisionService` 或等价边界。
- `SPEC-ai-model-gateway-provider-boundary`：只允许 `AiModelGateway` 接入 Spring AI / Spring AI Alibaba，业务服务继续只依赖 gateway。
- `SPEC-backend-observability-operations`：定义结构化日志字段、Micrometer metric names、health depth、alert rule 输出。

## 5. 文件边界建议

### 允许修改

- `backend/src/main/java/com/learningos/agent/application/AiModelGateway.java`
- `backend/src/main/java/com/learningos/agent/application/AgentRunRecorder.java`
- `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java`
- `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java`
- `backend/src/main/java/com/learningos/health/application/HealthService.java`
- `backend/src/main/java/com/learningos/health/api/HealthDtos.java`
- `backend/src/main/resources/application.yml`
- 相关测试：`AiModelGatewayTest`、`AnalyticsControllerTest`、`HealthControllerTest`、observability 测试。

### 不应修改

- 不应重写 `ResourceGenerationService` 全流程。
- 不应让 `orchestrator`、`rag`、`assessment` 直接依赖模型 SDK。
- 不应修改 frontend 直接接入模型 provider。
- 不应把 provider API key 写入 `application.yml` 默认值或前端环境变量。
- 不应绕过现有 `AgentRunRecorder` 另建一套模型日志路径。

## 6. 测试建议

- `cd backend; mvn "-Dtest=AnalyticsControllerTest" test`
- `cd backend; mvn "-Dtest=AiModelGatewayTest,AgentRunRecorderTest,ResourceGenerationControllerTest" test`
- `cd backend; mvn "-Dtest=HealthControllerTest,TraceFilterTest" test`
- 增加预算门禁测试：near budget 降级、over budget 要求人工确认、admin override、普通用户不可改预算规则。
- 增加模型 adapter 测试：provider disabled deterministic、provider enabled mock ChatClient、schema invalid fallback、provider error sanitized。
- 增加 observability 测试：traceId 出现在日志上下文、model latency metric、token cost metric、health deep check degraded 状态。

## 7. 架构漂移风险

- 最高风险是把真实模型 SDK 调用扩散到业务服务。基线要求模型调用集中在受控 gateway，并且模型输出要校验。
- 预算治理如果只留在 analytics 查询层，会形成“可观测但不可治理”的漂移。
- 深度健康检查如果强制依赖 Redis / MinIO / model live，可能破坏当前 Phase 1 本地启动约束。

## 8. 应进入工作流文档的内容

- PRD：成本治理目标、模型接入目标、可观测性目标。
- REQ：预算维度、阈值动作、人工确认语义、模型 provider 边界、日志 / metrics / health / alert 验收项。
- SPEC：API contract、`BudgetDecision` 状态机、`AiModelGateway` adapter contract、schema validation、metric names、health response 状态。
- PLAN：分三阶段实施：P2-4 状态同步与门禁、P3-3 provider adapter、P3-5 observability。
- TASK：每次只改一个边界，先测试后实现；依赖新增必须单独 dependency review。
- CONTEXT：列明允许修改文件、禁止修改文件、测试命令、并行专家输入边界。

## 9. Root Cause

根因不是单个代码缺陷，而是规划文档与后端实现节奏已经分叉：P2-4 的看板型治理能力已经实现，但 TODO 没有同步；P3-3 / P3-5 仍是后续生产化预留边界。正确收敛方向是保留 `AiModelGateway`、`AgentRunRecorder`、`AnalyticsService`、`HealthService` 这些既有边界，在边界内部补齐真实 provider、预算门禁、指标和探活，而不是重写业务模块。
