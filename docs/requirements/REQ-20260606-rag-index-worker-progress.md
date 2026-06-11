# REQ - RAG 索引 Worker 自动执行与进度可观测

## 1. 功能需求

| 编号 | 需求 | 验收 |
|---|---|---|
| FR-01 | 系统必须定时扫描 due `PENDING` 索引任务并自动处理。 | worker 测试证明 `PENDING -> RUNNING -> SUCCEEDED`，文档进入 `INDEXED`。 |
| FR-02 | worker 必须原子 claim 任务，避免重复消费。 | 并发 claim 同一 task 只有一个成功。 |
| FR-03 | 任务必须记录 progress、phase、heartbeat、lease owner、lease until、next retry、recoverable。 | V16 migration、实体映射、API response 和测试均覆盖。 |
| FR-04 | 处理阶段必须更新 progress 和 heartbeat。 | `processIndexTask(...)` 成功后 progress 为 100，阶段为 `SUCCEEDED`。 |
| FR-05 | 可恢复失败必须按 backoff 重入队。 | 未超最大重试时 `retryCount + 1`、`status=PENDING`、`nextRetryAt` 非空。 |
| FR-06 | 超过最大重试次数后必须终态失败。 | `status=FAILED`、`recoverable=false`、`nextRetryAt=null`。 |
| FR-07 | lease 过期的 `RUNNING` 任务必须可恢复为 due `PENDING` 或终态失败。 | 恢复测试覆盖 lease expired 与 lease active 两类任务。 |
| FR-08 | 新增 task detail API。 | `GET /api/index-tasks/{taskId}` 返回 task detail。 |
| FR-09 | task detail API 必须校验 KB read 权限。 | 未授权用户不能看到 task/document/kb/error detail。 |
| FR-10 | 错误信息必须脱敏。 | API 和持久化状态只包含安全错误码，不包含 storage key、路径或原始异常文本。 |

## 2. 非功能需求

| 编号 | 需求 |
|---|---|
| NFR-01 | 不新增外部依赖。 |
| NFR-02 | worker 每轮处理数量有上限，默认小批量。 |
| NFR-03 | 普通 H2 测试继续使用 `spring.flyway.enabled=false`，MySQL 兼容性由 opt-in smoke 验证。 |
| NFR-04 | Controller 只做 HTTP 委托，权限和业务逻辑在 Service 层。 |

## 3. 安全需求

- 不允许通过 taskId 枚举私有 KB 任务。
- 不允许把原始异常消息、对象存储路径、文档内容或 provider 错误返回给读者。
- 自动 retry 必须有最大次数和 backoff，避免无限循环。

## 4. 验收状态

| 编号 | 状态 | 证据 |
|---|---|---|
| FR-01 | 通过 | `IndexTaskWorkerSchedulerTest.workerProcessesDuePendingTaskAndMarksDocumentIndexed` |
| FR-02 | 通过 | `IndexServiceTest.claimDuePendingTasksClaimsEachTaskOnlyOnceAndWritesLeaseState` |
| FR-03 | 通过 | V16 migration、`KbIndexTask` 映射、`IndexTaskDetailResponse` 和 MySQL smoke |
| FR-04 | 通过 | `IndexServiceTest.processMarkdownIndexTaskCleansChunksAndMarksDocumentIndexed` |
| FR-05 | 通过 | `IndexServiceTest.failedProcessingRequeuesWithBackoffUntilRetryBudgetIsExhausted` |
| FR-06 | 通过 | `IndexServiceTest.failedProcessingRequeuesWithBackoffUntilRetryBudgetIsExhausted` |
| FR-07 | 通过 | `IndexServiceTest.recoversTimedOutRunningTasksAndKeepsNonExpiredOrTerminalTasksUnchanged` 和 `IndexTaskRecoverySchedulerTest` |
| FR-08 | 通过 | `DocumentControllerTest.returnsIndexTaskDetailForReadableKnowledgeBaseWithoutRawFailureDetails` |
| FR-09 | 通过 | `DocumentControllerTest.deniesIndexTaskDetailWhenUserCannotReadKnowledgeBase` |
| FR-10 | 通过 | API 返回 `DOCUMENT_EMPTY_OR_UNAVAILABLE` / `DOCUMENT_INDEX_LEASE_EXPIRED` 等安全错误码 |
