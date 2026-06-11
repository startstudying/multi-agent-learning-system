# RAG 索引任务 Active 去重验收

## 验收结论

通过。

本轮 P0-3 最小切片已完成：重复重建索引时，`PENDING/RUNNING` 任务不会被重复创建；终态任务允许重新创建新的 `PENDING` 索引任务。

## 验收项

| 验收项 | 结果 | 证据 |
|---|---|---|
| `PENDING` 最新任务重复 reindex 返回同一 `indexTaskId` | PASS | `DocumentControllerTest.uploadsDocumentCreatesPendingIndexTaskAndSupportsReindex` |
| `PENDING` 重复 reindex 不增加任务数量 | PASS | 同上，断言同一文档任务数为 1 |
| `RUNNING` 最新任务重复 reindex 返回同一 `indexTaskId` | PASS | `DocumentControllerTest.reindexReturnsActiveRunningTaskAndCreatesNewTaskAfterTerminalStatus` |
| `RUNNING` 重复 reindex 不增加任务数量 | PASS | 同上，断言同一文档任务数为 1 |
| `FAILED` 最新任务再次 reindex 新建 `PENDING` 任务 | PASS | 同上，第二个任务 id 不等于第一个任务 id |
| `SUCCEEDED` 最新任务再次 reindex 新建 `PENDING` 任务 | PASS | 同上，第三个任务 id 不等于第二个任务 id |
| 权限规则不回退 | PASS | `DocumentControllerTest.publicKnowledgeBaseIsReadableButNotWritableByOtherUsers` |
| API 响应结构兼容 | PASS | 文档上传、详情、列表、reindex 测试均通过 |
| 聚焦测试 | PASS | `mvn "-Dtest=DocumentControllerTest" test`：4 tests，0 failures |
| 全量后端测试 | PASS | `mvn test`：95 tests，0 failures |

## 未纳入本轮的后续项

- 文档上传 `requestId` 幂等。
- RAG query 响应快照重放。
- 索引任务 `retry_count`、`next_retry_at`、`last_error`、`recoverable` 等长任务字段。
- 启动后扫描超时 `RUNNING` 任务并恢复或失败化。
- 并发窗口下的数据库级去重约束或锁。
