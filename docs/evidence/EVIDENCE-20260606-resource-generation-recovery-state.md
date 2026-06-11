# 资源生成任务恢复状态证据

## 1. 代码变更

- `ResourceGenerationTask` 增加 `retryCount`、`nextRetryAt`、`lastError`、`recoverable` 字段和默认值。
- `ResourceGenerationResponse` 增加恢复状态字段。
- `ResourceGenerationService` 在模型调用失败时写入：
  - `status = FAILED`
  - `progressPercent = 0`
  - `retryCount = 1`
  - `nextRetryAt = now + 5 minutes`
  - `lastError = MODEL_CALL_FAILED`
  - `recoverable = true`
- 新增 `V11__resource_generation_task_recovery_state.sql`。

## 2. TDD 证据

RED：

```powershell
cd backend
mvn "-Dtest=ResourceGenerationControllerTest#persistsRetryMetadataWhenResourceGenerationFailsRecoverably,SchemaConvergenceMigrationTest#v11MigrationAddsRecoveryStateColumnsToResourceGenerationTask" test
```

结果：失败符合预期，`ResourceGenerationTask` 缺少 `getRetryCount/getNextRetryAt/getLastError/getRecoverable`。

GREEN：

```powershell
cd backend
mvn "-Dtest=ResourceGenerationControllerTest#persistsRetryMetadataWhenResourceGenerationFailsRecoverably,SchemaConvergenceMigrationTest#v11MigrationAddsRecoveryStateColumnsToResourceGenerationTask" test
```

结果：2 个测试通过，0 失败，0 错误。

## 3. 回归验证

```powershell
cd backend
mvn "-Dtest=ResourceGenerationControllerTest,SchemaConvergenceMigrationTest" test
```

结果：21 个测试通过，0 失败，0 错误。

## 4. 剩余风险

- 未运行完整 `mvn test`。
- 本 slice 只补齐资源生成任务恢复状态，不实现后台自动重试调度器。
- `agent_trace` / `model_call_log` 仍保存更详细失败信息；本次只保证业务任务 `lastError` 使用安全错误码。
