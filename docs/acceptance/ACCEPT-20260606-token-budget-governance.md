# Token / Cost 预算治理验收

## 验收项

- [x] 提供 `GET /api/analytics/token-budget/governance`。
- [x] 仅 `admin` 可以访问治理 API。
- [x] 支持 `from` / `to` 时间窗口过滤。
- [x] 支持按用户聚合 Token / 成本。
- [x] 支持按课程聚合 Token / 成本。
- [x] 支持按 Agent 类型聚合 Token / 成本。
- [x] 支持预算规则：接近预算时建议降级到 deterministic/cached response。
- [x] 支持预算规则：超过预算时要求人工确认。
- [x] 支持高成本任务告警。
- [x] 支持异常模型调用识别。
- [x] 不新增数据库迁移。
- [x] 不新增依赖。

## 验收命令

```bash
cd backend; mvn "-Dtest=AnalyticsControllerTest" test
```

当前结果：通过，`Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`。

## 非本轮范围

- 调用前预算门禁。
- 真实 provider 价格表。
- Spring AI / Spring AI Alibaba 真实模型接入。
- 外部告警系统。
- 前端治理看板。

## 验收结论

P2-4 的 analytics 层 Token / 成本预算治理最小闭环已满足验收标准。P3 仍需继续实现真实模型接入、调用前预算门禁、生产级指标和安全权限加固。
