# RAG 索引任务超时恢复 PRD

## 背景

当前 RAG 索引任务已经支持 active 去重：同一文档最新任务为 `PENDING/RUNNING` 时重复 `reindex` 会复用已有任务。但如果真实索引 worker 在 `RUNNING` 状态下崩溃或长时间卡住，系统会一直复用这个卡死任务，后续用户无法通过 `reindex` 创建新的任务。

`kb_index_task` 表已经有 `retry_count`、`error_message`、`started_at`、`finished_at`、`updated_at` 字段，但当前实体和服务没有使用这些恢复字段。

## 目标

完成 P0-3 的最小长任务恢复切片：提供服务层方法扫描超时 `RUNNING` 索引任务，将其标记为 `FAILED` 并记录恢复证据，使后续 `reindex` 可以创建新的 `PENDING` 任务。

## 用户价值

- 避免索引任务永久卡在 `RUNNING`。
- 模型或索引失败后，任务状态和错误原因可查询。
- 为后续后台恢复调度和真实索引 worker 打基础。

## 非目标

- 本轮不实现后台定时调度。
- 本轮不实现真实 PDF/embedding 索引 worker。
- 本轮不新增数据库迁移。
- 本轮不实现文档上传 `requestId` 或 RAG query 重放。

## 成功标准

- 超时 `RUNNING` 任务会变为 `FAILED`。
- 恢复时写入 `errorMessage`、递增 `retryCount`、设置 `finishedAt`。
- 未超时 `RUNNING` 任务不受影响。
- `PENDING/SUCCEEDED/FAILED` 任务不被误恢复。
- 超时恢复后，同一文档再次 `reindex` 能新建 `PENDING` 任务。
