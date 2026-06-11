# RAG 索引任务超时恢复验收

## 验收结论

通过。

本轮 P0-3 最小长任务恢复切片已完成：服务层可以将超时 `RUNNING` 索引任务恢复为 `FAILED`，记录失败证据，并允许后续 `reindex` 创建新的 `PENDING` 任务。

## 验收项

| 验收项 | 结果 | 证据 |
|---|---|---|
| 超时 `RUNNING` 任务恢复为 `FAILED` | PASS | `IndexServiceTest.recoversTimedOutRunningTasksAndKeepsNonExpiredOrTerminalTasksUnchanged` |
| 恢复时 `retryCount` 增加 | PASS | 同上，断言 2 变为 3；集成测试断言 0 变为 1 |
| 恢复时写入 `errorMessage` | PASS | 同上，断言包含 `timed out` |
| 恢复时设置 `finishedAt` | PASS | 同上 |
| 未超时 `RUNNING` 不被修改 | PASS | 同上 |
| `PENDING/SUCCEEDED/FAILED` 不被误修改 | PASS | 同上 |
| 恢复后同一文档 `reindex` 新建 `PENDING` 任务 | PASS | `DocumentControllerTest.reindexCreatesNewPendingTaskAfterRunningTaskIsRecoveredAsTimedOut` |
| API 响应结构兼容 | PASS | 文档上传、详情、列表、reindex 既有测试均通过 |
| 聚焦测试 | PASS | `mvn "-Dtest=IndexServiceTest,DocumentControllerTest" test`：6 tests，0 failures |
| 全量后端测试 | PASS | `mvn test`：97 tests，0 failures |

## 未纳入本轮的后续项

- 后台定时恢复扫描或应用启动恢复。
- 索引 worker heartbeat/progress。
- 文档上传 `requestId` 幂等。
- RAG query 响应快照重放。
- 数据库级并发去重约束或锁。
- `RAG_QA` 接入 Orchestrator 统一 workflow context。
