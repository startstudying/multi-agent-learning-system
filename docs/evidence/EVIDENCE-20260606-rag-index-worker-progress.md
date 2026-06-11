# Evidence - RAG 索引 Worker 自动执行与进度可观测

## 1. 范围

本证据归档 TODO P3-2 的一个独立切片：

```text
索引任务补齐 worker 自动执行、进度、heartbeat、自动重试/重新入队和 task detail API
```

本轮不关闭 parser adapter/OCR、生产级 token chunk、Embedding/VectorDB、hybrid retrieval/RRF/reranker fallback。

## 2. 关键变更证据

| 文件 | 证据 |
|---|---|
| `backend/src/main/resources/db/migration/V16__rag_index_task_worker_progress.sql` | `kb_index_task` 增加 `progress_percent`、`progress_phase`、`heartbeat_at`、`lease_owner`、`lease_until`、`next_retry_at`、`recoverable`，并新增 due/lease 索引。 |
| `backend/src/main/java/com/learningos/config/IndexWorkerProperties.java` | 新增 worker 配置，默认开启、小批量、有 lease、backoff 和最大 retry 次数。 |
| `backend/src/main/java/com/learningos/rag/application/IndexTaskWorkerScheduler.java` | 定时 claim due `PENDING` 任务并调用 `IndexService.processIndexTask(...)` 自动处理。 |
| `backend/src/main/java/com/learningos/rag/application/IndexTaskRecoveryScheduler.java` | lease 过期恢复使用 worker 的 `maxRetryCount` 和 `retryBackoff`，避免 scheduler 与 worker retry 策略分裂。 |
| `backend/src/main/java/com/learningos/rag/application/IndexService.java` | 增加 claim、progress/phase/heartbeat/lease 更新、bounded retry/requeue、lease expired recovery 和安全错误码；处理阶段进度使用独立事务提交，便于 task detail 在长耗时读取期间看到 durable heartbeat/lease。 |
| `backend/src/main/java/com/learningos/rag/repository/KbIndexTaskRepository.java` | due task 与 expired lease task 均使用带锁查询，lease recovery 不再 `findAll()` 后内存过滤。 |
| `backend/src/main/java/com/learningos/rag/api/DocumentController.java` | 新增 `GET /api/index-tasks/{taskId}`。 |
| `backend/src/main/java/com/learningos/rag/application/DocumentService.java` | task detail 通过 `task.kbId` 做 KB read 权限检查。 |
| `backend/src/main/java/com/learningos/rag/api/dto/DocumentDtos.java` | 新增 `IndexTaskDetailResponse`，只返回 task 状态字段和安全错误码。 |
| `backend/src/main/resources/application-test.yml` | 测试 profile 显式关闭 worker，避免后台调度影响断言。 |

## 3. TDD / 调试证据

### RED-01：P3-2 主实现前红灯

实现前已先补充 migration、worker、retry、detail API 测试。红灯表现为缺少 `IndexWorkerProperties`、`IndexTaskWorkerScheduler`、`claimDuePendingTasks(...)`、task detail DTO/API 和 V16 字段映射，编译失败。

### RED-02：lease recovery scheduler 策略分裂

收口审查发现旧 `IndexTaskRecoveryScheduler` 仍调用兼容入口 `recoverTimedOutRunningTasks(timeoutCutoff)`，会把过期 lease 任务按手动策略终态失败，不能复用 worker 的 retry/backoff 策略。

新增测试后先运行：

```powershell
cd backend
mvn --% -Dtest=IndexTaskRecoverySchedulerTest test
```

结果：编译失败，缺少带 `IndexWorkerProperties` 的 `IndexTaskRecoveryScheduler` 构造器。随后补实现并转绿。

### RED-03：代码审查后补充红灯

收口代码审查提出 3 个重要问题：

- `processIndexTask(...)` 整体事务导致 progress/heartbeat/lease 阶段更新直到索引结束才提交，task detail 在长耗时阶段不可观测。
- recovery scheduler 把 `runningTimeout` 叠加到 explicit `leaseUntil` 上，默认会把 2 分钟 lease 过期延后到约 32 分钟。
- worker batch 中单个任务在 document load 前异常会中断整个 batch，使后续已 claim 任务停留在 `RUNNING`。

补充 RED 测试后先运行：

```powershell
cd backend
mvn --% -Dtest=IndexTaskRecoverySchedulerTest,IndexTaskWorkerSchedulerTest,IndexServiceTest test
```

红灯结果：

- `IndexTaskRecoverySchedulerTest` 期望传入当前时间 `2026-06-06T08:00:00Z`，实际仍传入 `now - runningTimeout`。
- `IndexTaskWorkerSchedulerTest.workerContinuesBatchWhenOneClaimedTaskFailsBeforeDocumentLoad` 因 `Document not found` 中断 batch。
- `IndexServiceTest.processIndexTaskCommitsRunningProgressBeforeStorageReadCompletes` 在 storage read 阻塞时读取到的任务仍为 `PENDING`。

修复后：

- `IndexService` 用 `TransactionTemplate` 的 `PROPAGATION_REQUIRES_NEW` 提交阶段进度、heartbeat 和 lease。
- `IndexTaskRecoveryScheduler` 按 `leaseUntil < now` 恢复，不再叠加 `runningTimeout`。
- `IndexTaskWorkerScheduler` 对每个 claimed task 做异常隔离；缺失/删除文档按安全错误码 `DOCUMENT_EMPTY_OR_UNAVAILABLE` 进入 retry/requeue。
- `KbIndexTaskRepository.findExpiredRunningTasksForUpdate(now)` 使用 lease 索引查询过期 `RUNNING` 任务。

## 4. 正向验证命令

### 4.1 Recovery scheduler 单测

命令：

```powershell
cd backend
mvn --% -Dtest=IndexTaskRecoverySchedulerTest test
```

结果：

```text
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 4.2 P3-2 聚焦回归

命令：

```powershell
cd backend
mvn --% -Dtest=SchemaConvergenceMigrationTest,IndexServiceTest,IndexTaskWorkerSchedulerTest,IndexTaskRecoverySchedulerTest,DocumentControllerTest test
```

结果：

```text
Tests run: 38, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

覆盖点：

- V16 migration 文本级字段和索引断言。
- due `PENDING` task 自动 worker 消费并进入 `SUCCEEDED`。
- worker disabled 时不消费任务。
- claim 后 `RUNNING/CLAIMED/progress=5/heartbeat/leaseOwner/leaseUntil`。
- 处理成功后 `progress=100/SUCCEEDED/recoverable=false`。
- 可恢复失败后 `PENDING/RETRY_WAIT/nextRetryAt/recoverable=true`。
- retry budget exhausted 后 `FAILED/recoverable=false/nextRetryAt=null`。
- lease expired recovery 按 retry/backoff 重新入队或终态失败。
- task detail API 的授权、无权限 403 和安全错误码返回。
- 阶段 progress/heartbeat/lease 独立提交，可在 storage read 阻塞期间被其他事务查询到。
- worker batch 中单任务 document load 前失败不会阻断后续任务。

### 4.3 默认 MySQL smoke opt-in

命令：

```powershell
cd backend
mvn --% -Dtest=MysqlMigrationSmokeTest test
```

结果：

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

### 4.4 后端全量回归

命令：

```powershell
cd backend
mvn test
```

结果：

```text
Tests run: 226, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

### 4.5 真实 MySQL 8 smoke

命令：

```powershell
$env:MYSQL_PORT='3307'
powershell -ExecutionPolicy Bypass -File scripts/mysql-migration-smoke.ps1 -JdbcUrl 'jdbc:mysql://127.0.0.1:3307/learning_os_migration_smoke?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
```

结果：

```text
Successfully validated 16 migrations
Migrating schema `learning_os_migration_smoke` to version "16 - rag index task worker progress"
Successfully applied 16 migrations to schema `learning_os_migration_smoke`, now at version v16
MySQL migration smoke test passed.
```

说明：Flyway 输出的 `Table ... already exists` 和 `PROCEDURE ... does not exist` 为既有 idempotent DDL 模式警告；最终迁移成功且 smoke 断言通过。

## 5. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 只委托 Service；worker 和 recovery scheduler 调用 Service；Repository 只做数据访问。 |
| Frontend rules | PASS | 未修改前端，前端仍不直接调用 LLM。 |
| Agent / RAG rules | PASS | 本轮只增强 RAG 索引任务状态和恢复，不改变 RAG answer/citation 生成链路。 |
| Security | PASS | 无新增依赖；task detail 在 Service 层校验 KB read 权限；错误信息为安全错误码。 |
| API / Database | PASS | 新 API、V16 schema、worker/recovery 策略均已记录在 SPEC 和测试中。 |

## 6. 残余风险

- `@Lock(PESSIMISTIC_WRITE)` 的真实并发语义最终以 MySQL 为准；当前测试覆盖应用层语义和 MySQL DDL smoke，但没有做多进程 MySQL 并发压测。
- parser/OCR、token chunk、Embedding/VectorDB、hybrid retrieval/RRF/reranker fallback 仍是 P3-2 后续任务。
- worker 默认开启；测试 profile 已关闭。生产部署需要根据实例数和 DB 容量调小批量与固定延迟。
- `runningTimeout` 配置在 worker lease 恢复路径不再叠加到 `leaseUntil`，保留给旧兼容入口/配置兼容；后续可在单独清理任务中移除或重命名。
