# Acceptance - RAG 索引 Worker 自动执行与进度可观测

## 1. 验收结论

P3-2 的索引任务 worker/progress/heartbeat/retry/detail API 切片已通过验收。

本次验收仅关闭以下 TODO：

```text
索引任务补齐 worker 自动执行、进度、heartbeat、自动重试/重新入队和 task detail API
```

P3-2 中 parser adapter/OCR、生产级 chunk、Embedding/VectorDB、hybrid retrieval/RRF/reranker fallback 仍保持未完成。

## 2. 验收清单

| 验收项 | 状态 | 证据 |
|---|---|---|
| V16 migration 覆盖 task 进度和 lease 字段 | 通过 | `SchemaConvergenceMigrationTest` 和 MySQL V1-V16 smoke。 |
| worker 自动消费 due `PENDING` task | 通过 | `IndexTaskWorkerSchedulerTest.workerProcessesDuePendingTaskAndMarksDocumentIndexed`。 |
| worker 可配置关闭 | 通过 | `IndexTaskWorkerSchedulerTest.disabledWorkerDoesNotProcessPendingTasks`。 |
| claim 避免重复消费 | 通过 | `IndexServiceTest.claimDuePendingTasksClaimsEachTaskOnlyOnceAndWritesLeaseState`。 |
| progress/phase/heartbeat/lease 更新 | 通过 | `IndexServiceTest` 和 `IndexTaskWorkerSchedulerTest` 均断言相关字段。 |
| 长耗时索引期间 progress/heartbeat/lease 可见 | 通过 | `IndexServiceTest.processIndexTaskCommitsRunningProgressBeforeStorageReadCompletes` 断言 storage read 阻塞期间其他事务可读到 `RUNNING/PARSING/progress=20`。 |
| 可恢复失败 bounded retry/requeue | 通过 | `failedProcessingRequeuesWithBackoffUntilRetryBudgetIsExhausted` 覆盖重入队和耗尽。 |
| lease expired recovery 使用 worker retry 策略 | 通过 | `IndexTaskRecoverySchedulerTest` 断言 scheduler 调用带 `maxRetryCount/retryBackoff` 的恢复入口，并以当前时间执行 `leaseUntil < now` 恢复。 |
| worker batch 单任务异常隔离 | 通过 | `IndexTaskWorkerSchedulerTest.workerContinuesBatchWhenOneClaimedTaskFailsBeforeDocumentLoad` 断言缺失文档任务重入队且后续任务成功。 |
| task detail API 可查询 | 通过 | `DocumentControllerTest.returnsIndexTaskDetailForReadableKnowledgeBaseWithoutRawFailureDetails`。 |
| task detail API 防 IDOR | 通过 | `DocumentControllerTest.deniesIndexTaskDetailWhenUserCannotReadKnowledgeBase` 返回 403 且无 data。 |
| 错误信息脱敏 | 通过 | API 和 task error 断言为 `DOCUMENT_EMPTY_OR_UNAVAILABLE` / `DOCUMENT_INDEX_LEASE_EXPIRED` 等安全错误码。 |
| 后端全量回归 | 通过 | `mvn test`：226 tests, 0 failures, 0 errors, 1 skipped。 |
| 真实 MySQL smoke | 通过 | V1-V16 applied，当前版本 v16。 |

## 3. 测试结果

| 命令 | 结果 |
|---|---|
| `mvn --% -Dtest=IndexTaskRecoverySchedulerTest test` | 通过：4 tests |
| `mvn --% -Dtest=IndexTaskRecoverySchedulerTest,IndexTaskWorkerSchedulerTest,IndexServiceTest test` | 通过：15 tests |
| `mvn --% -Dtest=SchemaConvergenceMigrationTest,IndexServiceTest,IndexTaskWorkerSchedulerTest,IndexTaskRecoverySchedulerTest,DocumentControllerTest test` | 通过：38 tests |
| `mvn --% -Dtest=MysqlMigrationSmokeTest test` | 通过：1 skipped |
| `mvn test` | 通过：226 tests, 0 failures, 0 errors, 1 skipped |
| `scripts/mysql-migration-smoke.ps1` with `MYSQL_PORT=3307` | 通过：MySQL 8 V1-V16 |

## 4. 非目标确认

- 未新增依赖。
- 未修改前端。
- 未修改 RAG query/citation、EmbeddingService、RerankerService、AdaptiveRagRouter 或 Orchestrator。
- 未把整个 P3-2 标记完成，只关闭 worker/progress/heartbeat/retry/detail API 项。

## 5. 审批状态

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-06 | 通过 |
