# 资源生成任务恢复状态验收

## 1. 验收结论

通过。

## 2. 验收项

| 验收项 | 结果 | 证据 |
|---|---|---|
| `resource_generation_task` 具备恢复字段 | 通过 | `SchemaConvergenceMigrationTest#v11MigrationAddsRecoveryStateColumnsToResourceGenerationTask` |
| 模型失败后任务状态为 `FAILED` | 通过 | `ResourceGenerationControllerTest#persistsRetryMetadataWhenResourceGenerationFailsRecoverably` |
| 模型失败后写入 `retryCount = 1` | 通过 | 同上 |
| 模型失败后写入未来 `nextRetryAt` | 通过 | 同上 |
| 模型失败后写入安全 `lastError = MODEL_CALL_FAILED` | 通过 | 同上 |
| 模型失败后 `recoverable = true` | 通过 | 同上 |
| GET 任务详情返回恢复字段 | 通过 | 同上 |
| 不暴露 provider 原始错误到 `lastError` | 通过 | 断言固定错误码 |

## 3. 验证命令

- `mvn "-Dtest=ResourceGenerationControllerTest#persistsRetryMetadataWhenResourceGenerationFailsRecoverably,SchemaConvergenceMigrationTest#v11MigrationAddsRecoveryStateColumnsToResourceGenerationTask" test`
- `mvn "-Dtest=ResourceGenerationControllerTest,SchemaConvergenceMigrationTest" test`

## 4. 未完成事项

- 后台自动 retry worker、retry limit 和 backoff 策略仍属于后续任务。
- 完整 MySQL 8 Flyway smoke 仍属于 P3。
