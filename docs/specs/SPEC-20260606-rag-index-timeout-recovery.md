# RAG 索引任务超时恢复规格

## 状态定义

可恢复候选：

```text
RUNNING and updatedAt < timeoutCutoff
```

恢复结果：

```text
status = FAILED
retryCount = retryCount + 1
errorMessage = "Index task timed out while RUNNING; marked failed by recovery."
finishedAt = now
```

不处理状态：

```text
PENDING
RUNNING and updatedAt >= timeoutCutoff
SUCCEEDED
FAILED
```

## 服务规则

`IndexService.recoverTimedOutRunningTasks(Instant timeoutCutoff)`：

1. 查询 `KbIndexTaskRepository.findByStatusAndUpdatedAtBefore(IndexTaskStatus.RUNNING, timeoutCutoff)`。
2. 逐个将任务状态改为 `FAILED`。
3. 递增 `retryCount`。
4. 写入 `errorMessage`。
5. 设置 `finishedAt`。
6. 返回恢复任务数量。

## 与 active 去重的关系

`IndexService.createPendingTask(KbDocument document)` 已复用 `PENDING/RUNNING` 最新任务。超时恢复后，原 `RUNNING` 任务成为 `FAILED`，因此再次 `reindex` 会创建新的 `PENDING` 任务。

## API 行为

本轮不新增公开 API。恢复方法先作为 Service 能力，由测试直接调用；后续可以接入启动扫描、定时任务或管理端恢复入口。

## 兼容性

- 不修改 Controller。
- 不修改现有 DTO。
- 不新增迁移。
- 不影响文档读写权限。
