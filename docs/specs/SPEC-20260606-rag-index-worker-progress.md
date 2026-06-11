# SPEC - RAG 索引 Worker 自动执行与进度可观测

## 1. 数据库

新增 Flyway migration：

```text
backend/src/main/resources/db/migration/V16__rag_index_task_worker_progress.sql
```

`kb_index_task` 新增列：

| 字段 | 类型 | 说明 |
|---|---|---|
| `progress_percent` | `int not null default 0` | 0-100 进度 |
| `progress_phase` | `varchar(80) not null default 'PENDING'` | 当前阶段 |
| `heartbeat_at` | `datetime(6) null` | worker 最近 heartbeat |
| `lease_owner` | `varchar(120) null` | 当前 claim worker id |
| `lease_until` | `datetime(6) null` | lease 到期时间 |
| `next_retry_at` | `datetime(6) null` | 下次可重试时间 |
| `recoverable` | `boolean not null default true` | 是否仍可自动恢复/重试 |

新增索引：

- `idx_kb_index_task_due(status, next_retry_at, created_at)`
- `idx_kb_index_task_lease(status, lease_until)`

## 2. 状态和阶段

状态仍使用现有 `IndexTaskStatus`：

```text
PENDING, RUNNING, SUCCEEDED, FAILED
```

阶段使用字符串：

```text
PENDING, CLAIMED, PARSING, CHUNKING, EMBEDDING, INDEXING, SUCCEEDED, RETRY_WAIT, FAILED
```

## 3. Worker 配置

新增配置类：

```text
learning-os.rag.index-worker
```

默认值：

| 配置 | 默认值 |
|---|---|
| `enabled` | `true` |
| `fixed-delay` | `5s` |
| `batch-size` | `2` |
| `lease-duration` | `2m` |
| `retry-backoff` | `30s` |
| `max-retry-count` | `2` |

依据：

- Spring Framework 官方 scheduling 文档支持 fixed-delay 周期任务。
- Spring Boot 官方 external config 文档支持 `@ConfigurationProperties` 与 `Duration` 绑定。
- Spring Data JPA 官方文档支持 repository 查询方法上的 `@Lock`。

## 4. Worker 流程

1. `IndexTaskWorkerScheduler.runOnce()` 在配置开启时执行。
2. 调用 `IndexService.claimDuePendingTasks(now, batchSize, workerId, leaseDuration)`。
3. claim 方法在事务内按 `PENDING` 和 `nextRetryAt <= now` 选择 due task，并使用任务行锁或条件更新原子抢占。
4. claim 成功后写入：
   - `status=RUNNING`
   - `progressPhase=CLAIMED`
   - `progressPercent=5`
   - `heartbeatAt=now`
   - `leaseOwner=workerId`
   - `leaseUntil=now + leaseDuration`
5. worker 对每个 claimed task 调用 `processIndexTask(taskId, workerId, maxRetryCount, retryBackoff, leaseDuration)`。
6. 处理阶段更新 progress / phase / heartbeat / lease。
   - 阶段更新必须独立提交，不能被完整索引事务延迟到任务结束后才可见。
   - 长耗时 parse/read/chunk/index 过程中，task detail API 应能读取最近一次 durable progress、heartbeat 和 lease。
7. 成功后写入 `SUCCEEDED`、`progressPercent=100`、`recoverable=false`、`finishedAt`。
8. 可恢复失败时：
   - `retryCount + 1`
   - 未超上限：`status=PENDING`、`progressPhase=RETRY_WAIT`、`nextRetryAt=now + retryBackoff`、`recoverable=true`
   - 超上限：`status=FAILED`、`progressPhase=FAILED`、`recoverable=false`、`finishedAt=now`

## 5. 恢复流程

`recoverTimedOutRunningTasks(...)` 升级为基于 explicit lease：

- `RUNNING` 且 `leaseUntil < now` 的任务视为 lease expired。
- `IndexTaskRecoveryScheduler` 传入当前时间 `now`，不得再叠加 `runningTimeout` 到 explicit `leaseUntil` 上。
- 若未超 retry 上限，恢复为 `PENDING` 并设置 `nextRetryAt`。
- 若已超 retry 上限，进入终态 `FAILED`。
- 未过期 lease 不恢复。
- 恢复查询应使用 `status + leaseUntil` 条件查询，避免 `findAll()` 后内存过滤。

`IndexTaskRecoveryScheduler` 调用恢复服务时必须使用 `learning-os.rag.index-worker.max-retry-count` 和 `learning-os.rag.index-worker.retry-backoff`，保证 lease 过期恢复与 worker 处理失败使用同一套有界 retry 策略。

## 6. API

新增：

```http
GET /api/index-tasks/{taskId}
```

响应字段：

```json
{
  "taskId": "idx_xxx",
  "documentId": "doc_xxx",
  "kbId": "kb_xxx",
  "status": "RUNNING",
  "progressPercent": 45,
  "progressPhase": "CHUNKING",
  "retryCount": 1,
  "recoverable": true,
  "nextRetryAt": "2026-06-06T10:05:00Z",
  "heartbeatAt": "2026-06-06T10:00:30Z",
  "leaseUntil": "2026-06-06T10:02:30Z",
  "startedAt": "2026-06-06T10:00:00Z",
  "finishedAt": null,
  "errorMessage": null
}
```

权限：

- Service 通过 `task.kbId` 调用 `PermissionService.canReadKnowledgeBase(userId, task.kbId)`。
- 无权限返回 `FORBIDDEN`，不返回 task detail。
- 不存在 task 返回 `NOT_FOUND`。

## 7. 错误脱敏

持久化和 API 返回只允许安全错误码：

- `DOCUMENT_READ_FAILED`
- `DOCUMENT_EMPTY_OR_UNAVAILABLE`
- `DOCUMENT_PARSE_FAILED`
- `DOCUMENT_INDEX_FAILED`
- `DOCUMENT_INDEX_UNEXPECTED_ERROR`
- `DOCUMENT_INDEX_LEASE_EXPIRED`

不得保存原始异常文本、storage key、对象路径、文档片段或 provider 错误。

## 8. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 委托 Service；worker 调 Service；Repository 只提供数据访问。 |
| Frontend rules | PASS | 不修改前端。 |
| Agent / RAG rules | PASS | RAG 索引任务更可观测；不修改 RAG answer/citation。 |
| Security | PASS | 不新增依赖；权限在后端 Service 校验；错误脱敏。 |
| API / Database | PASS | 新 API 和 V16 schema 均记录在 SPEC。 |

## 9. 实施状态

2026-06-06 已完成本 SPEC 范围内 worker/progress/heartbeat/retry/detail API 切片。验收证据见：

- `docs/evidence/EVIDENCE-20260606-rag-index-worker-progress.md`
- `docs/acceptance/ACCEPT-20260606-rag-index-worker-progress.md`
