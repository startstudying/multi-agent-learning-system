# RUN-20260611 backend architecture Provider/Ops Backend 子代理报告

## 结论

P3-3 / P3-5 / P2-4 的若干“后续增强”在代码中已有基础实现，但计划状态曾未完全同步。建议拆成“状态复核补证据”和“真实生产化增强”，避免重复实现已有的 embedding registry、告警持久化、预算门禁基础能力。

## 相关现状

- `AiModelGateway` 已优先读取 `ModelProviderService.resolveDefaultChatProvider()`。
- `EmbeddingService` 已读取 registry embedding provider，并通过 `EmbeddingModelFactory` 动态创建 OpenAI-compatible embedding model。
- `OpsAlertPersistenceService` 已支持告警持久化、webhook opt-in、persisted list 和 acknowledge。
- `TokenBudgetGateService` 已在 `AiModelGateway.generateStructured()` 前执行预算评估并触发 deterministic fallback。
- `MysqlMigrationSmokeTest` 曾停在 V20，需要同步到 V22。

## 推荐拆分

1. Embedding registry provider 状态复核与补测：S。
2. DashScope / Spring AI Alibaba 专用 provider 设计与依赖审查：M。
3. 外部 provider smoke 与 MySQL migration smoke 更新：S/M。
4. Ops alert 持久化/推送状态复核与硬化：S/M。
5. 调用前 token 预算门禁生产化：M，后续扩展 user/course/provider/time window 维度。

## 风险

- DashScope 专用 starter 不能直接加入，需要 dependency review。
- Provider smoke 必须 opt-in，默认离线可测。
- Webhook 出站请求需要后续统一 outbound URL policy。
- MySQL smoke 需要跟随最新 migration version。

## 测试建议

```powershell
cd backend
mvn --% -Dtest=EmbeddingServiceTest,ModelProviderServiceTest,AiModelGatewayTest test
mvn --% -Dtest=ModelProviderControllerTest,AnalyticsControllerTest,TokenBudgetGateServiceTest test
mvn --% -Dtest=SchemaConvergenceMigrationTest,MysqlMigrationSmokeTest test
```
