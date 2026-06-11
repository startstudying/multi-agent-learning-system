# RUN-20260607 Observability/Ops Expert

## 任务

围绕 `docs/planning/backend-architecture-todolist.md` 中 P3-5 可观测性与运维告警做只读专家分析。

## 1. 已实现证据

- P3-5 已完成结构化请求日志、Micrometer 运行指标、深度健康检查；未完成项为慢查询、慢模型调用、无引用回答、审核积压告警。
- `StructuredRequestLoggingFilter` 输出 `traceId/userId/route/status/latencyMs/errorCode` 白名单字段，并避免 query/body/header/prompt/raw exception 泄露。
- `LearningOsMetrics` 已记录 HTTP、RAG、model、token/cost 指标，并保持低基数 tag。
- `/api/health` 已覆盖 database、Redis、MinIO、model provider 的低成本探测和脱敏响应。
- `AnalyticsService` 已有 token budget governance、异常模型调用识别和 pending review summary，但还不是统一 Ops alert 闭环。
- `RagQueryService` 已记录 no-source 语义和 Micrometer 指标；`ReviewGovernanceService` 已阻断 `NO_SOURCE` 资源直接发布。

## 2. 未完成告警项拆分

1. 慢查询告警：当前只有 HTTP/RAG latency 和 `kb_query_log.latencyMs`，无统一 alert DTO/API。
2. 慢模型调用告警：已有 metrics 和 token governance 异常识别，但无独立 Ops alert。
3. 无引用回答告警：已有 no-source 响应和指标，但无时间窗/课程维度 no-source rate 告警。
4. 审核积压告警：已有 review queue/pending summary，但无积压数量、最老待审年龄和 severity。

## 3. 最小实现方案

- 在 `analytics` 模块新增 admin-only query-time API：

```text
GET /api/analytics/ops/alerts?from=&to=&slowQueryMs=&slowModelMs=&reviewBacklogHours=&reviewBacklogCount=
```

- 统一 alert 类型：
  - `SLOW_RAG_QUERY`
  - `SLOW_MODEL_CALL`
  - `RAG_NO_SOURCE`
  - `REVIEW_BACKLOG`
- `LearningOsMetrics` 可新增 `learningos.ops.alert.count`，tag 只允许 `type/severity`。
- 不新增依赖、不新增外部告警系统；Prometheus/Grafana/Alertmanager 后续再接。

## 4. 测试建议

- `AnalyticsControllerTest`：非 admin 403，admin 可返回四类 alert，响应不含 `markdownContent`、prompt、raw question、raw provider error。
- seed `KbQueryLog.latencyMs` 超阈值触发 `SLOW_RAG_QUERY`。
- seed `ModelCallLog.latencyMs` 超阈值触发 `SLOW_MODEL_CALL`。
- seed no-source query/resource review 触发 `RAG_NO_SOURCE`。
- seed old pending review 触发 `REVIEW_BACKLOG`。
- metrics tag 不包含 `traceId/userId/courseId/reviewId/requestId/question/prompt/errorMessage`。

## 5. 风险

- 当前 analytics 部分路径仍使用 `findAll()`，告警路径需增加 repository 条件查询。
- no-source 最小方案可能依赖文本/JSON 推断，长期建议 schema 增加 `no_source` / `citation_count`。
- “慢查询”需明确为 `SLOW_RAG_QUERY`；SQL slow query 采集应单独设计。
- 当前只是 query-time alert，不含外部推送。

## 6. 建议测试命令

```powershell
cd backend
mvn --% -Dtest=AnalyticsControllerTest,StructuredRequestLoggingFilterTest,HealthServiceTest,HealthControllerTest,AiModelGatewayTest,RagQueryServiceTest,ResourceReviewControllerTest test
mvn test
```
