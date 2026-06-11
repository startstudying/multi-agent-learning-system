# Analytics 学习分析扩展证据

## TDD RED

命令：

```bash
cd backend
mvn "-Dtest=AnalyticsControllerTest" test
```

结果：失败，符合预期。

失败点：

- `$.data.tokenUsage.byUser[0].userId` 缺失。
- `GET /api/analytics/students/alice/summary` 路由不存在，返回 500。
- 跨 learner summary 访问尚未返回 403。

## GREEN 验证

命令：

```bash
cd backend
mvn "-Dtest=AnalyticsControllerTest" test
```

结果：

```text
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 覆盖范围

- `GET /api/analytics/overview` 旧字段兼容。
- `tokenUsage.byAgentTask` 和 `tokenUsage.byModel` 保留。
- `tokenUsage.byUser` 和 `tokenUsage.byAgentName` 新增。
- `agentSummary` 新增成功率、失败率、平均延迟、tokenCost、ragHitRate。
- `GET /api/analytics/students/{learnerId}/summary` 返回学习进度、当前掌握度、趋势、最近错因、下一步推荐。
- 跨 learnerId 查询返回 403。

## 备注

测试输出包含 Mockito 动态 agent 的 JDK 未来兼容 warning，不影响本次测试结果。
