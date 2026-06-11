# RUN-20260607 backend architecture completion - alerting integration

## 集成结论

统一采用 query-time 告警 API：

```http
GET /api/analytics/ops/alerts
```

本切片不新增告警表、不改 DB schema、不新增依赖、不改前端。

## API 合同

### 权限

- 临时 admin-only：`X-User-Id: admin`
- 非 admin 或缺失 header：`403 / FORBIDDEN`
- 文档必须说明这不是完整 JWT/RBAC。

### Query 参数

| 参数 | 默认值 | 说明 |
|---|---:|---|
| `from` | `to - 24h` | 查询窗口开始 |
| `to` | `now` | 查询窗口结束 |
| `slowQueryMs` | `1000` | 慢 RAG 查询阈值 |
| `slowModelMs` | `2000` | 慢模型调用阈值 |
| `noSourceRateThreshold` | `0.2` | 无来源比例阈值 |
| `noSourceMinCount` | `3` | 无来源数量阈值 |
| `reviewBacklogHours` | `24` | 审核积压年龄阈值 |
| `reviewBacklogCount` | `10` | 审核积压数量阈值 |

### 响应

```json
{
  "windowStart": "2026-06-06T00:00:00Z",
  "windowEnd": "2026-06-07T00:00:00Z",
  "thresholds": {
    "slowQueryMs": 1000,
    "slowModelMs": 2000,
    "noSourceRateThreshold": 0.2,
    "noSourceMinCount": 3,
    "reviewBacklogHours": 24,
    "reviewBacklogCount": 10
  },
  "alerts": []
}
```

只返回触发的 alert；无触发时 `alerts` 为空数组。

## 触发口径

| Alert | 触发规则 |
|---|---|
| `SLOW_RAG_QUERY` | 窗口内 `KbQueryLog.latencyMs >= slowQueryMs` |
| `SLOW_MODEL_CALL` | 窗口内 `ModelCallLog.latencyMs >= slowModelMs` |
| `RAG_NO_SOURCE` | `noSourceCount >= noSourceMinCount` 且 `noSourceCount / totalRagCount >= noSourceRateThreshold` |
| `REVIEW_BACKLOG` | 待处理 review 中，年龄超过 `reviewBacklogHours` 的数量 `>= reviewBacklogCount` |

边界使用 `>=`。

## 冲突决议

- Endpoint 采用 Ops Expert 的 `/api/analytics/ops/alerts`。
- 权限和脱敏采用 Security Expert 的 admin-only + 白名单 DTO。
- 不复用 `AbnormalModelCall`，因为该 record 包含 `errorMessage`。
- 本切片不新增 dependency review，因为不新增依赖。

## 最小修改文件

- `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java`
- `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java`
- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`

可读取但默认不修改 repository/entity/schema。

## 测试清单

- 非 admin / 缺失 header 返回 `FORBIDDEN`。
- admin 无数据返回默认阈值和空 `alerts`。
- seed 四类风险信号后返回四类 alert。
- 响应不包含敏感字段名或敏感 seed 内容。
- `from >= to`、非正阈值、非法比例返回 `VALIDATION_ERROR`。
