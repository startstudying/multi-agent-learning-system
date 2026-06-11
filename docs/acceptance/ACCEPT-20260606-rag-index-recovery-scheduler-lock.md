# RAG 索引恢复调度与并发锁 Acceptance

## 验收结论

状态：`DONE`

本切片核心行为已实现并通过 RAG 聚焦测试。此前由 Orchestrator 构造器不匹配导致的编译阻断已在集成验证中解除。

## 验收项

| ID | Acceptance | Result | Evidence |
|---|---|---|---|
| AC-01 | 同一文档并发 `createPendingTask(...)` 只产生一个 active task。 | PASS | `IndexServiceTest.createPendingTaskReusesSingleActiveTaskWhenConcurrentReindexStartsTogether` |
| AC-02 | active task 为 `PENDING/RUNNING` 时继续复用。 | PASS | `DocumentControllerTest` reindex 去重测试 |
| AC-03 | terminal task 后允许新建 `PENDING`。 | PASS | `DocumentControllerTest.reindexReturnsActiveRunningTaskAndCreatesNewTaskAfterTerminalStatus` |
| AC-04 | 超时 `RUNNING` 任务恢复为 `FAILED` 的服务行为保持。 | PASS | `IndexServiceTest.recoversTimedOutRunningTasksAndKeepsNonExpiredOrTerminalTasksUnchanged` |
| AC-05 | 启动后恢复入口调用既有服务方法。 | PASS | `IndexTaskRecoverySchedulerTest.recoverOnStartupCallsIndexServiceWithConfiguredTimeoutCutoff` |
| AC-06 | 定时恢复入口单次有界调用既有服务方法。 | PASS | `IndexTaskRecoverySchedulerTest.recoverOnScheduleCallsIndexServiceOnceWithoutLongLoop` |
| AC-07 | disabled 时不调用恢复。 | PASS | `IndexTaskRecoverySchedulerTest.disabledRecoverySkipsStartupAndScheduledCalls` |
| AC-08 | 不修改上传幂等相关 service/controller。 | PASS | 未修改 `DocumentService` / `DocumentController`。 |
| AC-09 | 用户指定最小测试命令可运行。 | PASS | `mvn "-Dtest=IndexServiceTest,DocumentControllerTest" test`，10 tests, 0 failures |

## Open Concerns

- 未增加数据库唯一索引；本轮通过文档行锁收敛经 `IndexService` 进入的并发 reindex。
- 后台恢复只失败化超时任务，不自动重试或重新入队。
