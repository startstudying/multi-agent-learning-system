# RAG 索引任务超时恢复任务

## 任务清单

- [x] 读取项目记忆、RAG 记忆、TODO、IndexService、KbIndexTask、迁移和测试。
- [x] 启动 subagent 并行审查 P0 剩余切片。
- [x] 创建本轮 workflow 文档。
- [x] 写失败测试覆盖超时恢复和恢复后 reindex。
- [x] 实现 `IndexService.recoverTimedOutRunningTasks(...)`。
- [x] 跑聚焦测试。
- [x] 跑全量后端测试。
- [x] 更新 Evidence、Acceptance、Memory、Changelog、总 TODO、Retrospective。

## Done Criteria

- [x] 超时 `RUNNING` 任务恢复为 `FAILED`。
- [x] 恢复时 `retryCount` 增加。
- [x] 恢复时写入 `errorMessage` 并设置 `finishedAt`。
- [x] 未超时 `RUNNING` 不被修改。
- [x] `PENDING/SUCCEEDED/FAILED` 不被误修改。
- [x] 恢复后同一文档再次 reindex 新建 `PENDING` 任务。
- [x] `mvn test` 通过或记录失败原因。
