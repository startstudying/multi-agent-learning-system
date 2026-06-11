# RAG 索引恢复调度与并发锁规格

## 状态集合

Active 索引任务：

```text
PENDING
RUNNING
```

Terminal 索引任务：

```text
SUCCEEDED
FAILED
```

## 并发去重规格

`IndexService.createPendingTask(KbDocument document)`：

1. 校验 `document` 与 `document.id` 不为空。
2. 在同一事务内通过 `KbDocumentRepository` 按 `document.id` 获取 `PESSIMISTIC_WRITE` 行锁。
3. 基于锁定后的文档重新查询最新 `KbIndexTask`。
4. 最新任务为 `PENDING` 或 `RUNNING` 时返回已有任务。
5. 最新任务不存在或为 `FAILED` / `SUCCEEDED` 时，新建 `PENDING` 任务，`documentId/kbId` 取锁定文档。

说明：锁文档行而不是只锁 task 行，是为了覆盖“当前还没有 active task”的并发创建窗口。

## 后台恢复规格

新增 `IndexTaskRecoveryScheduler`：

- 包位置：`com.learningos.rag.application`。
- 启动后恢复：监听 `ApplicationReadyEvent`，若配置允许则执行一次恢复。
- 定时恢复：使用 `@Scheduled(fixedDelayString = "...")`，按固定间隔执行。
- 单次执行逻辑：`cutoff = Instant.now().minus(timeout)`，调用 `indexService.recoverTimedOutRunningTasks(cutoff)`。
- 不做长循环、不直接操作 repository、不吞掉业务恢复规则。

配置前缀：

```yaml
learning-os.rag.index-recovery:
  enabled: true
  run-on-startup: true
  running-timeout: 30m
  fixed-delay: 5m
```

默认值：

- `enabled = true`
- `runOnStartup = true`
- `runningTimeout = 30m`
- `fixedDelay = 5m`

## API 与数据库兼容性

- 不新增 API。
- 不新增迁移。
- 不改变 `kb_index_task` 字段。
- 不改变 `DocumentUploadResponse`。

## 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 不变；业务逻辑仍在 Service/Scheduler 调用 Service。 |
| Frontend rules | PASS | 不涉及前端。 |
| Agent / RAG rules | PASS | RAG 索引任务状态更可恢复；不涉及生成回答和引用。 |
| Security | PASS | 不新增依赖，不接触密钥。 |
| API / Database | PASS | API 和 schema 不变。 |
