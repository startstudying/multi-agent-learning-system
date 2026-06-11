# RAG 索引任务超时恢复需求

## 功能需求

| ID | Requirement |
|---|---|
| FR-01 | `IndexService` 必须提供服务层恢复方法，接收超时阈值时间点。 |
| FR-02 | 最新或历史任务中，状态为 `RUNNING` 且 `updatedAt` 早于阈值的任务必须被标记为 `FAILED`。 |
| FR-03 | 恢复时必须将 `retryCount` 增加 1。 |
| FR-04 | 恢复时必须写入可查询的 `errorMessage`。 |
| FR-05 | 恢复时必须设置 `finishedAt`。 |
| FR-06 | `RUNNING` 但未超时的任务不能被修改。 |
| FR-07 | `PENDING`、`SUCCEEDED`、`FAILED` 任务不能被恢复逻辑误修改。 |
| FR-08 | 超时任务恢复为 `FAILED` 后，`createPendingTask(document)` 必须允许新建 `PENDING` 任务。 |

## 非功能需求

| ID | Requirement |
|---|---|
| NFR-01 | 不新增依赖。 |
| NFR-02 | 不新增数据库迁移，沿用 `V1__rag_foundation.sql` 已有字段。 |
| NFR-03 | 恢复逻辑位于 Service 层，不放入 Controller。 |
| NFR-04 | 保持现有公开 API 响应结构不变。 |
| NFR-05 | 聚焦测试和全量后端测试通过。 |

## 验收要求

- 单测覆盖超时恢复、非超时保护、非 RUNNING 状态保护。
- 集成测试覆盖恢复后 `reindex` 创建新任务。
- 文档权限行为不回退。
