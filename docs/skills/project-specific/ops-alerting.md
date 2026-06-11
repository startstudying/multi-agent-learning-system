# Ops Alerting

## 使用场景

当实现或审查后端运维告警、告警聚合 API、慢查询/慢模型/无来源/RAG/审核积压风险视图时使用。

## 核心规则

1. 优先做 query-time 聚合；只有 SPEC 明确要求时才新增告警表、推送渠道或后台任务。
2. Controller 只处理 HTTP 参数、当前用户和统一响应；告警口径应放在 Application Service。
3. 运维告警默认 admin-only；如果当前项目仍是过渡身份模型，只能声明临时 `X-User-Id: admin`，不能声明真实 JWT/RBAC 已完成。
4. 告警响应必须使用白名单 DTO，不直接序列化 entity。
5. 告警指标只返回聚合信息：类型、严重级别、数量、阈值、摘要、reason codes、safe metrics。
6. 不在告警响应中返回：
   - prompt / question / answer
   - request body / response body
   - `responseJson` / `sourcesJson`
   - raw `errorMessage`
   - generated `markdownContent`
   - review `citationCheck` / `safetyCheck` / `revisionSuggestion`
   - API key、token、secret、provider raw error
7. 阈值参数必须校验：
   - 时间窗口：`from < to`
   - 毫秒/数量/小时阈值：正数
   - 比例阈值：`0..1`
8. 阈值边界要明确使用 `>` 还是 `>=`，并在测试中覆盖边界。
9. Review backlog 不应因为对象早于查询窗口开始就被漏报；积压类告警通常以 `createdAt <= windowEnd` 和年龄阈值判断。

## 推荐测试

- 非 admin 和缺失身份返回 `FORBIDDEN`。
- 无信号时返回默认 thresholds 和空 alerts。
- 每类告警至少一个触发用例。
- 非法参数返回 `VALIDATION_ERROR`。
- response body 断言敏感字段名和敏感 seed 内容不存在。
- 相邻回归至少覆盖日志、健康检查、RAG 查询和 Review Gate。
- 完整后端测试通过后再声明完成。

## 反模式

- 为最小告警 API 新增外部推送依赖。
- 复用包含 raw error 字段的 DTO。
- 在 metrics/alerts 中携带 `userId`、`traceId`、`requestId`、问题原文或生成内容。
- 把“查询型告警”表述为已经具备生产告警通知能力。
