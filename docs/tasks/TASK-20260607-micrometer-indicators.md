# TASK - Micrometer 运行指标
状态：已完成（2026-06-07）

## Task 1

补齐 P3-5 最小 Micrometer 业务指标。

### 目标

- HTTP 请求、RAG 查询、模型调用、token 使用在运行时写入 Micrometer。
- 指标可通过 `MeterRegistry` 单测验证，并可通过 `/actuator/metrics` 诊断。
- 指标 tags 只包含低基数非敏感字段。
- 不新增依赖、不改 API、不改 DB。

### 交付物

- `LearningOsMetrics` 内部适配层。
- HTTP request timer/failure counter。
- RAG query duration/count/retrieval/citation/failure metrics。
- Model gateway duration/failure metrics。
- Token usage/cost summaries。
- Actuator `metrics` endpoint exposure。
- 对应测试、证据、验收和文档更新。

### Done Criteria

- [x] `learningos.http.server.requests` timer 记录成功与失败请求。
- [x] `learningos.http.server.failures` counter 只记录 `status >= 400`。
- [x] `learningos.rag.query.duration` 只记录 fresh query，不记录 replay。
- [x] `learningos.rag.query.count` 区分 success/no_source/replay/error。
- [x] `learningos.rag.retrieval.count` 和 `learningos.rag.citation.count` 记录数值。
- [x] `learningos.model.call.duration` 记录模型网关成功与失败耗时。
- [x] `learningos.model.call.failures` 记录稳定错误码。
- [x] `learningos.token.usage` 记录 prompt/completion/total。
- [x] `learningos.token.cost` 记录 estimated cost。
- [x] 所有测试验证不含禁止 tag：`traceId/userId/requestId/agentTaskId/kbId/question/prompt/source/errorMessage`。
- [x] `application.yml` 只新增 `metrics` exposure。
- [x] 定向测试通过：`28 tests, 0 failures, 0 errors`。
- [x] 全量 `mvn test` 通过：`244 tests, 0 failures, 0 errors, 1 skipped`。

## 验证记录

- `cd backend && mvn --% -Dtest=StructuredRequestLoggingFilterTest,RagQueryServiceTest,AiModelGatewayTest,AgentRunRecorderTest test`
- `cd backend && mvn --% -Dtest=GlobalExceptionHandlerTest,HealthControllerTest,StructuredRequestLoggingFilterTest test`
- `cd backend && mvn test`

