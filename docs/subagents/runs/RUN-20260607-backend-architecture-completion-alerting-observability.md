# RUN-20260607 backend architecture completion - alerting observability

## 任务

P3-5-A 慢 RAG 查询、慢模型调用、RAG 无来源、审核积压告警 API 只读运维分析。

## 结论

采用 query-time API，不新增告警表、不新增外部推送、不新增依赖、不修改 DB schema。

推荐 endpoint：

```text
GET /api/analytics/ops/alerts
```

建议 query 参数：

```text
from
to
slowQueryMs
slowModelMs
noSourceRateThreshold
noSourceMinCount
reviewBacklogHours
reviewBacklogCount
```

告警类型：

- `SLOW_RAG_QUERY`
- `SLOW_MODEL_CALL`
- `RAG_NO_SOURCE`
- `REVIEW_BACKLOG`

## 数据来源

| Alert | Source | 口径 |
|---|---|---|
| `SLOW_RAG_QUERY` | `KbQueryLog` | `latencyMs >= slowQueryMs` |
| `SLOW_MODEL_CALL` | `ModelCallLog` | `latencyMs >= slowModelMs` |
| `RAG_NO_SOURCE` | `KbQueryLog` | `retrievalCount <= 0` 的数量和比例同时达标 |
| `REVIEW_BACKLOG` | `ResourceReview` | 待处理 review 年龄和数量达标 |

## 响应约束

只返回聚合数量、比例、最大耗时、最老待审时间、阈值和安全 reason code。

禁止返回：

- `question`
- `responseJson`
- `sourcesJson`
- `prompt`
- `markdownContent`
- `errorMessage`
- `citationCheck`
- `revisionSuggestion`
- raw provider error

## 最小修改建议

- `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java`
- `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java`
- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`

如后续性能需要，可再单独扩展 repository 聚合查询和 DB 索引；本切片不改 schema。

## 风险

- 当前实现只能作为查询型告警视图，不是推送型告警系统。
- `RAG_NO_SOURCE` 依赖 `retrievalCount <= 0` 的最小可用口径；长期建议结构化 `noSource/citationCount` 字段。
- 慢查询在本切片中定义为 `SLOW_RAG_QUERY`，不是数据库 SQL slow query。
