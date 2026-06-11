# REQ - Micrometer 运行指标
状态：已完成（2026-06-07）

## 1. 需求说明

系统必须在关键运行链路发生时同步写入 Micrometer 指标，作为 P3-5 可观测性最小指标层。指标用于运行时趋势、失败率和成本趋势，不替代持久化审计表。

## 2. 功能需求

1. HTTP 请求完成时必须记录 `learningos.http.server.requests` timer。
2. HTTP 请求状态码 `>= 400` 时必须记录 `learningos.http.server.failures` counter。
3. HTTP 指标 tags 只能包含 `method`、`route`、`status`、`error_code`。
4. RAG fresh query 成功时必须记录 `learningos.rag.query.duration` timer。
5. RAG query 必须记录 `learningos.rag.query.count` counter，区分 `success`、`no_source`、`replay`、`error`。
6. RAG replay 不得计入 `learningos.rag.query.duration`。
7. RAG retrieval/citation 数量必须用 DistributionSummary 记录为数值，不能作为 tag。
8. RAG failure 必须记录稳定 `error_code`，不得记录原始异常信息。
9. 模型网关调用必须记录 `learningos.model.call.duration` timer，区分 `SUCCESS` 和 `FAILED`。
10. 模型调用失败必须记录 `learningos.model.call.failures` counter。
11. token 使用必须记录 prompt/completion/total 三类数值。
12. token 估算成本必须记录为数值 summary。
13. `/actuator/metrics` 必须在默认配置中可用于诊断已注册 meter。

## 3. 安全需求

1. 指标 tags 禁止包含 `traceId`、`userId`、`requestId`、`agentTaskId`、`kbId`、`documentId`、`resourceId`。
2. 指标 tags 禁止包含 `question`、`prompt`、`answer`、`excerpt`、`sourcesJson`、`responseJson`。
3. 指标 tags 禁止包含原始 `errorMessage` 或 provider 原始错误。
4. `route` 必须优先使用 Spring MVC pattern，不得包含完整 query string。
5. `/actuator/metrics` 只暴露指标诊断，不暴露 `env`、`beans`、`heapdump`、`threaddump` 等高风险端点。

## 4. 约束

- 不新增 Maven dependency。
- 不新增数据库表或字段。
- 不修改业务 API 响应结构。
- 不修改现有审计事实表写入语义。
- 不把 Analytics 查询接口作为 metrics 触发点。
- 不在 Controller 中散落业务埋点。

## 5. 验收条件

- 定向测试覆盖 HTTP、RAG、模型、token 指标写入。
- 测试断言禁止的高基数/敏感 tag 不存在。
- `application.yml` 只新增 `metrics` endpoint exposure，不启用高风险 endpoint。
- P3-5 TODO 中 Micrometer 指标项更新为完成。
- Evidence 和 Acceptance 记录测试命令、结果和任何限制。
